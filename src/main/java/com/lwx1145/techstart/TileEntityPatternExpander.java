package com.lwx1145.techstart;

import appeng.api.AEApi;
import appeng.api.networking.*;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.util.AECableType;
import appeng.api.util.AEColor;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.helpers.DualityInterface;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 样板扩展器TileEntity
 * 实现AE2的IGridHost接口，连接到AE网络
 * 自动扫描网络中的ME接口，找到通配符样板并展开
 */
public class TileEntityPatternExpander extends TileEntity implements IGridHost, ICraftingProvider, ITickable {

    private IGridNode gridNode;
    private boolean isFirstTick = true;
    private final Set<ICraftingPatternDetails> expandedPatterns = new HashSet<>();
    private final Map<DualityInterface, List<SmartPatternDetails>> interfacePatternMap = new HashMap<>();

    private long lastScanTick = 0;
    private boolean scanDirty = true;
    private int lastPatternHash = 0;
    private int lastPatternCount = 0;
    private long lastLightScanTick = 0;
    private int lightScanIntervalTicks = 5;
    private int lastInterfacePatternHash = 0;
    private int lastInterfacePatternCount = 0;
    private int currentInterfacePatternHash = 0;
    private int currentInterfacePatternCount = 0;

    private static Field cachedDualityField;
    private static Field cachedPatternsField;
    private static boolean fieldsInitialized = false;

    public TileEntityPatternExpander() {
        // 构造器
    }

    public void update() {
        if (world.isRemote) {
            return; // 只在服务器端执行
        }

        if (isFirstTick) {
            isFirstTick = false;
            // 创建并初始化网格节点
            createGridNode();
        }

        if (shouldLightScan() && detectPatternChange()) {
            scanDirty = true;
            scanAndExpandPatterns();
            return;
        }

        if (shouldScan()) {
            scanAndExpandPatterns();
        }
    }

    private void createGridNode() {
        if (gridNode == null) {
            try {
                gridNode = AEApi.instance().grid().createGridNode(new GridBlockInfo(this));
                if (gridNode != null) {
                    gridNode.updateState();
                    scanDirty = true;
                }
            } catch (Exception e) {
                System.err.println("[PatternExpander] 创建网格节点失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void initReflectionFields() {
        if (fieldsInitialized) {
            return;
        }
        fieldsInitialized = true;
        try {
            cachedDualityField = appeng.tile.misc.TileInterface.class.getDeclaredField("duality");
            cachedDualityField.setAccessible(true);
        } catch (Exception e) {
            cachedDualityField = null;
        }
        try {
            cachedPatternsField = DualityInterface.class.getDeclaredField("patterns");
            cachedPatternsField.setAccessible(true);
        } catch (Exception e) {
            cachedPatternsField = null;
        }
    }

    private boolean shouldScan() {
        if (world == null) {
            return false;
        }
        if (scanDirty) {
            return true;
        }
        long now = world.getTotalWorldTime();
        return now - lastScanTick >= getScanIntervalTicks();
    }

    private boolean shouldLightScan() {
        if (world == null) {
            return false;
        }
        if (scanDirty) {
            return false;
        }
        long now = world.getTotalWorldTime();
        return now - lastLightScanTick >= lightScanIntervalTicks;
    }

    private int getScanIntervalTicks() {
        int interval = ModConfig.patternExpanderScanIntervalTicks;
        if (interval < 20) {
            interval = 20;
        }
        return interval;
    }

    private void markScanDirty() {
        scanDirty = true;
    }

    private int computePatternHash(Set<ICraftingPatternDetails> patterns) {
        int hash = 1;
        for (ICraftingPatternDetails pattern : patterns) {
            if (pattern instanceof SmartPatternDetails) {
                hash = 31 * hash + ((SmartPatternDetails) pattern).getOreName().hashCode();
            } else if (pattern != null) {
                hash = 31 * hash + pattern.getPattern().toString().hashCode();
            }
        }
        return hash;
    }

    private void recordInterfacePatternSignature(ItemStack patternStack) {
        currentInterfacePatternCount++;
        int stackHash = patternStack.getItem().hashCode();
        stackHash = 31 * stackHash + patternStack.getItemDamage();
        if (patternStack.hasTagCompound()) {
            stackHash = 31 * stackHash + patternStack.getTagCompound().toString().hashCode();
        }
        currentInterfacePatternHash = 31 * currentInterfacePatternHash + stackHash;
    }

    private boolean detectPatternChange() {
        if (gridNode == null || !gridNode.isActive()) {
            return false;
        }

        IGrid grid = gridNode.getGrid();
        if (grid == null) {
            return false;
        }

        int hash = 1;
        int count = 0;

        try {
            for (IGridNode node : grid.getNodes()) {
                Object host = node.getMachine();

                if (host instanceof appeng.tile.misc.TileInterface) {
                    appeng.tile.misc.TileInterface tileInterface = (appeng.tile.misc.TileInterface) host;
                    DualityInterface duality = getDualityFromTile(tileInterface);
                    if (duality == null) {
                        continue;
                    }

                    initReflectionFields();
                    if (cachedPatternsField == null) {
                        continue;
                    }
                    Object patternsObj = cachedPatternsField.get(duality);
                    if (!(patternsObj instanceof net.minecraftforge.items.IItemHandler)) {
                        continue;
                    }

                    net.minecraftforge.items.IItemHandler patterns = (net.minecraftforge.items.IItemHandler) patternsObj;
                    for (int i = 0; i < patterns.getSlots(); i++) {
                        ItemStack patternStack = patterns.getStackInSlot(i);
                        if (patternStack.isEmpty()) {
                            continue;
                        }
                        if (!(patternStack.getItem() instanceof ItemTest)) {
                            continue;
                        }
                        if (!ItemTest.hasEncodedItemStatic(patternStack)) {
                            continue;
                        }

                        count++;
                        int stackHash = patternStack.getItem().hashCode();
                        stackHash = 31 * stackHash + patternStack.getItemDamage();
                        if (patternStack.hasTagCompound()) {
                            stackHash = 31 * stackHash + patternStack.getTagCompound().toString().hashCode();
                        }
                        hash = 31 * hash + stackHash;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        } finally {
            lastLightScanTick = world.getTotalWorldTime();
        }

        return count != lastInterfacePatternCount || hash != lastInterfacePatternHash;
    }

    /**
     * 扫描网络中的ME接口，找到通配符样板并展开
     */
    private void scanAndExpandPatterns() {
        if (gridNode == null || !gridNode.isActive()) {
            return;
        }

        IGrid grid = gridNode.getGrid();
        if (grid == null) {
            return;
        }

        try {
            // 清空之前的展开样板
            expandedPatterns.clear();
            interfacePatternMap.clear();
            currentInterfacePatternHash = 1;
            currentInterfacePatternCount = 0;

            // 遍历网络中的所有节点
            for (IGridNode node : grid.getNodes()) {
                Object host = node.getMachine();
                
                // 检查是否是TileInterface（方块形式的ME接口）
                if (host instanceof appeng.tile.misc.TileInterface) {
                    appeng.tile.misc.TileInterface tileInterface = (appeng.tile.misc.TileInterface) host;
                    DualityInterface duality = getDualityFromTile(tileInterface);
                    if (duality != null) {
                        expandPatternsFromInterface(duality);
                    }
                }
                // 注意：Part形式的接口暂时跳过，因为类名可能不同
                // 如果需要支持，需要找到正确的类名
            }

            int currentCount = expandedPatterns.size();
            int currentHash = computePatternHash(expandedPatterns);

            // 仅在配方发生变化时通知网络更新合成列表
            if (gridNode.isActive() && (currentCount != lastPatternCount || currentHash != lastPatternHash)) {
                lastPatternCount = currentCount;
                lastPatternHash = currentHash;
                grid.postEvent(new MENetworkCraftingPatternChange(this, gridNode));
            }

            lastInterfacePatternCount = currentInterfacePatternCount;
            lastInterfacePatternHash = currentInterfacePatternHash;

            scanDirty = false;
            lastScanTick = world.getTotalWorldTime();

        } catch (Exception e) {
            System.err.println("[PatternExpander] 扫描失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从Tile形式的ME接口获取DualityInterface
     */
    private DualityInterface getDualityFromTile(appeng.tile.misc.TileInterface tile) {
        try {
            initReflectionFields();
            if (cachedDualityField == null) {
                return null;
            }
            return (DualityInterface) cachedDualityField.get(tile);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从ME接口展开通配符样板
     */
    private void expandPatternsFromInterface(DualityInterface duality) {
        try {
            // 获取接口的样板槽位（AppEngInternalInventory类型）
            initReflectionFields();
            if (cachedPatternsField == null) {
                return;
            }
            Object patternsObj = cachedPatternsField.get(duality);
            
            // AppEngInternalInventory 实现了 IItemHandler 接口
            if (!(patternsObj instanceof net.minecraftforge.items.IItemHandler)) {
                return;
            }
            
            net.minecraftforge.items.IItemHandler patterns = (net.minecraftforge.items.IItemHandler) patternsObj;
            List<SmartPatternDetails> expandedForThisInterface = new ArrayList<>();

            for (int i = 0; i < patterns.getSlots(); i++) {
                ItemStack patternStack = patterns.getStackInSlot(i);
                if (patternStack.isEmpty()) {
                    continue;
                }

                // 检查是否是我们的智能样板
                if (!(patternStack.getItem() instanceof ItemTest)) {
                    continue;
                }

                if (!ItemTest.hasEncodedItemStatic(patternStack)) {
                    continue;
                }

                recordInterfacePatternSignature(patternStack);

                String inputOre = ItemTest.getInputOreNameStatic(patternStack);
                String outputOre = ItemTest.getOutputOreNameStatic(patternStack);

                // 检查是否是通配符样板
                if (!inputOre.contains("*") && !outputOre.contains("*")) {
                    continue;
                }

                // 展开为虚拟样板
                SmartPatternDetails mainPattern = new SmartPatternDetails(patternStack);
                List<SmartPatternDetails> virtualPatterns = mainPattern.expandToVirtualPatterns();

                expandedPatterns.addAll(virtualPatterns);
                expandedForThisInterface.addAll(virtualPatterns);
            }

            if (!expandedForThisInterface.isEmpty()) {
                interfacePatternMap.put(duality, expandedForThisInterface);
            }

        } catch (Exception e) {
            System.err.println("[PatternExpander] 展开样板失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ========== IGridHost 接口实现 ==========

    public IGridNode getGridNode(AEPartLocation dir) {
        return gridNode;
    }

    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.SMART;
    }

    public void securityBreak() {
        // 安全破坏
        world.destroyBlock(pos, true);
    }

    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    // ========== ICraftingProvider 接口实现 ==========

    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        // 提供展开的虚拟样板给合成系统
        for (ICraftingPatternDetails pattern : expandedPatterns) {
            craftingTracker.addCraftingOption(this, pattern);
        }
    }

    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        // 将合成任务推送到原始的ME接口
        // 找到包含这个样板的接口
        for (Map.Entry<DualityInterface, List<SmartPatternDetails>> entry : interfacePatternMap.entrySet()) {
            if (entry.getValue().contains(patternDetails)) {
                DualityInterface targetInterface = entry.getKey();
                
                try {
                    // 调用接口的pushPattern方法
                    return targetInterface.pushPattern(patternDetails, table);
                } catch (Exception e) {
                    System.err.println("[PatternExpander] 推送样板失败: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }
        
        return false;
    }

    public boolean isBusy() {
        // 检查所有关联的接口是否繁忙
        for (DualityInterface duality : interfacePatternMap.keySet()) {
            if (duality.isBusy()) {
                return true;
            }
        }
        return false;
    }

    // ========== TileEntity 生命周期 ==========

    public void onLoad() {
        if (!world.isRemote) {
            scanDirty = true;
            createGridNode();
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (gridNode != null) {
            gridNode.destroy();
            gridNode = null;
        }
    }

    public void onChunkUnload() {
        if (gridNode != null) {
            gridNode.destroy();
            gridNode = null;
        }
    }

    public void onBlockDestroyed() {
        if (gridNode != null) {
            gridNode.destroy();
            gridNode = null;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        // 读取存储的数据（如果有）
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        // 保存数据（如果有）
        return compound;
    }

    /**
     * GridBlockInfo - AE2网格节点信息
     */
    private static class GridBlockInfo implements IGridBlock {
        private final TileEntityPatternExpander tile;

        public GridBlockInfo(TileEntityPatternExpander tile) {
            this.tile = tile;
        }

        @Override
        public double getIdlePowerUsage() {
            return 1.0; // 每tick消耗1AE
        }

        @Override
        public EnumSet<GridFlags> getFlags() {
            return EnumSet.of(GridFlags.REQUIRE_CHANNEL);
        }

        @Override
        public boolean isWorldAccessible() {
            return true;
        }

        @Override
        public DimensionalCoord getLocation() {
            return new DimensionalCoord(tile);
        }

        @Override
        public AEColor getGridColor() {
            return AEColor.TRANSPARENT;
        }

        @Override
        public void onGridNotification(GridNotification notification) {
            tile.markScanDirty();
        }

        @Override
        public void setNetworkStatus(IGrid grid, int channelsInUse) {
            // 网络状态更新
        }

        @Override
        public EnumSet<EnumFacing> getConnectableSides() {
            return EnumSet.allOf(EnumFacing.class);
        }

        @Override
        public IGridHost getMachine() {
            return tile;
        }

        public void gridChanged() {
            tile.markScanDirty();
        }

        public ItemStack getMachineRepresentation() {
            return new ItemStack(TechStart.PATTERN_EXPANDER);
        }
    }
}
