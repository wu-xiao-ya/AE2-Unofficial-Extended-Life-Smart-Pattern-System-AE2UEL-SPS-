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
import appeng.api.storage.data.IAEItemStack;
import appeng.util.InventoryAdaptor;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 样板扩展器TileEntity
 * 实现AE2的IGridHost接口，连接到AE网络
 * 自动扫描网络中的ME接口，找到通配符样板并展开
 */
public class TileEntityPatternExpander extends TileEntity implements IGridHost, ICraftingProvider, ITickable {

    private static final boolean DEBUG_LOG = false;

    private IGridNode gridNode;
    private boolean isFirstTick = true;
    // 使用常规集合减少写入时的额外拷贝
    private final Set<ICraftingPatternDetails> expandedPatterns = new HashSet<>();
    private final Map<Object, List<ICraftingPatternDetails>> interfacePatternMap = Collections.synchronizedMap(new HashMap<>());

    private long lastScanTick = 0;
    private boolean scanDirty = true;
    private int lastPatternHash = 0;
    private int lastPatternCount = 0;
    private long lastLightScanTick = 0;
    private int lightScanIntervalTicks = 20;
    private int lastInterfacePatternHash = 0;
    private int lastInterfacePatternCount = 0;
    private int currentInterfacePatternHash = 0;
    private int currentInterfacePatternCount = 0;
    private int lastProvideCount = -1;

    private static Field cachedDualityField;
    private static Field cachedPatternsField;
    private static Field cachedCraftingListField;
    private static Field cachedWaitingToSendField;
    private static Field cachedWaitingToSendFacingField;
    private static final Map<Class<?>, Field> cachedDualityFieldByClass = new HashMap<>();
    private static final Map<Class<?>, Field> cachedPatternsFieldByClass = new HashMap<>();
    private static boolean fieldsInitialized = false;

    private static Method cachedFakeFluidCheck;
    private static Method cachedFluidAdaptorWrap;
    private static boolean ae2fcHelpersInitialized = false;

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
        try {
            cachedCraftingListField = DualityInterface.class.getDeclaredField("craftingList");
            cachedCraftingListField.setAccessible(true);
        } catch (Exception e) {
            cachedCraftingListField = null;
        }
        try {
            cachedWaitingToSendField = DualityInterface.class.getDeclaredField("waitingToSend");
            cachedWaitingToSendField.setAccessible(true);
        } catch (Exception e) {
            cachedWaitingToSendField = null;
        }
        try {
            cachedWaitingToSendFacingField = DualityInterface.class.getDeclaredField("waitingToSendFacing");
            cachedWaitingToSendFacingField.setAccessible(true);
        } catch (Exception e) {
            cachedWaitingToSendFacingField = null;
        }
    }

    private void initAe2fcHelpers() {
        if (ae2fcHelpersInitialized) {
            return;
        }
        ae2fcHelpersInitialized = true;
        try {
            Class<?> fakeFluids = Class.forName("com.glodblock.github.common.item.fake.FakeFluids");
            cachedFakeFluidCheck = fakeFluids.getMethod("isFluidFakeItem", ItemStack.class);
        } catch (Exception e) {
            cachedFakeFluidCheck = null;
        }
        try {
            Class<?> adaptor = Class.forName("com.glodblock.github.inventory.FluidConvertingInventoryAdaptor");
            cachedFluidAdaptorWrap = adaptor.getMethod("wrap", net.minecraftforge.common.capabilities.ICapabilityProvider.class, EnumFacing.class);
        } catch (Exception e) {
            cachedFluidAdaptorWrap = null;
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
            stackHash = 31 * stackHash + computeNbtSignature(patternStack.getTagCompound());
        }
        currentInterfacePatternHash = 31 * currentInterfacePatternHash + stackHash;
    }

    private int computeNbtSignature(NBTTagCompound tag) {
        if (tag == null) {
            return 0;
        }
        int hash = tag.getKeySet().hashCode();
        if (tag.hasKey("InputOreName")) {
            hash = 31 * hash + tag.getString("InputOreName").hashCode();
        }
        if (tag.hasKey("OutputOreName")) {
            hash = 31 * hash + tag.getString("OutputOreName").hashCode();
        }
        if (tag.hasKey("EncodedItem")) {
            hash = 31 * hash + tag.getString("EncodedItem").hashCode();
        }
        if (tag.hasKey("InputOreNames")) {
            hash = 31 * hash + tag.getTagList("InputOreNames", 8).tagCount();
        }
        if (tag.hasKey("OutputOreNames")) {
            hash = 31 * hash + tag.getTagList("OutputOreNames", 8).tagCount();
        }
        if (tag.hasKey("InputFluids")) {
            hash = 31 * hash + tag.getTagList("InputFluids", 8).tagCount();
        }
        if (tag.hasKey("OutputFluids")) {
            hash = 31 * hash + tag.getTagList("OutputFluids", 8).tagCount();
        }
        if (tag.hasKey("InputGases")) {
            hash = 31 * hash + tag.getTagList("InputGases", 8).tagCount();
        }
        if (tag.hasKey("OutputGases")) {
            hash = 31 * hash + tag.getTagList("OutputGases", 8).tagCount();
        }
        if (tag.hasKey("InputGasAmounts")) {
            hash = 31 * hash + tag.getTagList("InputGasAmounts", 3).tagCount();
        }
        if (tag.hasKey("OutputGasAmounts")) {
            hash = 31 * hash + tag.getTagList("OutputGasAmounts", 3).tagCount();
        }
        if (tag.hasKey("InputGasItems")) {
            hash = 31 * hash + tag.getTagList("InputGasItems", 10).tagCount();
        }
        if (tag.hasKey("OutputGasItems")) {
            hash = 31 * hash + tag.getTagList("OutputGasItems", 10).tagCount();
        }
        return hash;
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

                DualityInterface duality = null;
                if (host instanceof appeng.tile.misc.TileInterface) {
                    duality = getDualityFromTile(host);
                } else {
                    try {
                        Class<?> tileDualInterfaceClass = Class.forName("com.glodblock.github.common.tile.TileDualInterface");
                        if (tileDualInterfaceClass.isInstance(host)) {
                            duality = getDualityFromTile(host);
                        }
                    } catch (ClassNotFoundException e) {
                        // AE2FC未安装，跳过
                    }
                }

                if (duality == null) {
                    continue;
                }

                Field patternsField = resolvePatternsField(duality);
                if (patternsField == null) {
                    continue;
                }
                Object patternsObj = patternsField.get(duality);
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
                        stackHash = 31 * stackHash + computeNbtSignature(patternStack.getTagCompound());
                    }
                    hash = 31 * hash + stackHash;
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
            debugLog("开始扫描样板");
            // 清空之前的展开样板
            expandedPatterns.clear();
            interfacePatternMap.clear();
            currentInterfacePatternHash = 1;
            currentInterfacePatternCount = 0;

            // 遍历网络中的所有节点
            for (IGridNode node : grid.getNodes()) {
                Object host = node.getMachine();
                
                // 检查是否是TileInterface（官方ME接口）
                if (host instanceof appeng.tile.misc.TileInterface) {
                    appeng.tile.misc.TileInterface tileInterface = (appeng.tile.misc.TileInterface) host;
                    DualityInterface duality = getDualityFromTile(tileInterface);
                    if (duality != null) {
                        expandPatternsFromInterface(duality, duality);
                    } else {
                        System.err.println("[PatternExpander] AE2 interface duality not found for host: " + host.getClass().getName());
                    }
                } else {
                    // 检查是否是AE2FC的TileDualInterface（流体接口）
                    try {
                        Class<?> tileDualInterfaceClass = Class.forName("com.glodblock.github.common.tile.TileDualInterface");
                        Class<?> tileTrioInterfaceClass = tryLoadClass("com.glodblock.github.common.tile.TileTrioInterface");
                        Class<?> partTrioInterfaceClass = tryLoadClass("com.glodblock.github.common.part.PartTrioInterface");
                        boolean isAe2fcInterface = tileDualInterfaceClass.isInstance(host)
                            || (tileTrioInterfaceClass != null && tileTrioInterfaceClass.isInstance(host))
                            || (partTrioInterfaceClass != null && partTrioInterfaceClass.isInstance(host));
                        if (isAe2fcInterface) {
                            DualityInterface duality = getDualityFromTile(host);
                            if (duality != null) {
                                // Use tile/part as push target to support AE2FC interface execution.
                                expandPatternsFromInterface(host, duality);
                            } else {
                                System.err.println("[PatternExpander] AE2FC duality not found for host: " + host.getClass().getName());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        // AE2FC未安装，跳过
                    }
                }
            }

            int currentCount = expandedPatterns.size();
            int currentHash = computePatternHash(expandedPatterns);

            int smartCount = 0;
            int ae2fcCount = 0;
            int otherCount = 0;
            for (ICraftingPatternDetails detail : expandedPatterns) {
                if (detail == null) {
                    continue;
                }
                if (detail instanceof SmartPatternDetails) {
                    smartCount++;
                    continue;
                }
                if (detail.getClass().getName().equals("com.glodblock.github.util.FluidCraftingPatternDetails")) {
                    ae2fcCount++;
                    continue;
                }
                otherCount++;
            }

            debugLog("扫描完成: expanded=" + currentCount + ", interfaces=" + interfacePatternMap.size()
                + ", smart=" + smartCount + ", ae2fc=" + ae2fcCount + ", other=" + otherCount);

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
    private DualityInterface getDualityFromTile(Object tile) {
        try {
            Field dualityField = resolveDualityField(tile);
            if (dualityField == null) {
                DualityInterface dualityFromMethod = resolveDualityByMethod(tile);
                if (dualityFromMethod != null) {
                    return dualityFromMethod;
                }
                System.err.println("[PatternExpander] No duality field for tile: " + tile.getClass().getName());
                return null;
            }
            return (DualityInterface) dualityField.get(tile);
        } catch (Exception e) {
            System.err.println("[PatternExpander] Failed to read duality: " + e.getMessage());
            return null;
        }
    }

    private DualityInterface resolveDualityByMethod(Object tile) {
        if (tile == null) {
            return null;
        }
        Class<?> current = tile.getClass();
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Method method : current.getDeclaredMethods()) {
                if (method.getParameterCount() != 0) {
                    continue;
                }
                String name = method.getName().toLowerCase();
                if (!name.contains("duality") && !name.contains("interface")) {
                    continue;
                }
                if (method.getReturnType() == void.class) {
                    continue;
                }
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(tile);
                    if (result instanceof DualityInterface) {
                        return (DualityInterface) result;
                    }
                } catch (Exception e) {
                    // Try other methods.
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Field resolveDualityField(Object tile) {
        if (tile == null) {
            return null;
        }
        Class<?> cls = tile.getClass();
        Field cached = cachedDualityFieldByClass.get(cls);
        if (cached != null) {
            return cached;
        }
        initReflectionFields();
        if (cachedDualityField != null && cachedDualityField.getDeclaringClass().isAssignableFrom(cls)) {
            cachedDualityFieldByClass.put(cls, cachedDualityField);
            return cachedDualityField;
        }
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (DualityInterface.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    cachedDualityFieldByClass.put(cls, field);
                    return field;
                }
                if (!field.getType().isPrimitive()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(tile);
                        if (value instanceof DualityInterface) {
                            cachedDualityFieldByClass.put(cls, field);
                            return field;
                        }
                    } catch (Exception e) {
                        // Ignore and continue.
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 从ME接口展开通配符样板
     */
    private void expandPatternsFromInterface(Object pushTarget, DualityInterface duality) {
        try {
            // 获取接口的样板槽位（AppEngInternalInventory类型）
            Field patternsField = resolvePatternsField(duality);
            if (patternsField == null) {
                System.err.println("[PatternExpander] No patterns field for duality: " + duality.getClass().getName());
                return;
            }
            Object patternsObj = patternsField.get(duality);
            
            // AppEngInternalInventory 实现了 IItemHandler 接口
            if (!(patternsObj instanceof net.minecraftforge.items.IItemHandler)) {
                System.err.println("[PatternExpander] Patterns is not IItemHandler: " + patternsObj.getClass().getName());
                return;
            }
            
            net.minecraftforge.items.IItemHandler patterns = (net.minecraftforge.items.IItemHandler) patternsObj;
            List<ICraftingPatternDetails> expandedForThisInterface = new ArrayList<>();
            int added = 0;

            boolean logSlotDetails = isAe2fcTarget(pushTarget);
            for (int i = 0; i < patterns.getSlots(); i++) {
                ItemStack patternStack = patterns.getStackInSlot(i);
                if (patternStack.isEmpty()) {
                    continue;
                }

                // 检查是否是我们的智能样板
                if (!(patternStack.getItem() instanceof ItemTest)) {
                    if (logSlotDetails) {
                        debugLog("AE2FC slot " + i + " 非智能样板: item=" + patternStack.getItem().getClass().getName());
                    }
                    continue;
                }

                if (!ItemTest.hasEncodedItemStatic(patternStack)) {
                    if (logSlotDetails) {
                        debugLog("AE2FC slot " + i + " 未编码: item=" + patternStack.getItem().getClass().getName());
                    }
                    continue;
                }

                recordInterfacePatternSignature(patternStack);

                String inputOre = ItemTest.getInputOreNameStatic(patternStack);
                String outputOre = ItemTest.getOutputOreNameStatic(patternStack);

                // 展开为虚拟样板（通配符）或直接使用原样板（非通配符）
                SmartPatternDetails mainPattern = new SmartPatternDetails(patternStack);
                List<SmartPatternDetails> virtualPatterns = new ArrayList<>();
                if (inputOre.contains("*") || outputOre.contains("*")) {
                    virtualPatterns.addAll(mainPattern.expandToVirtualPatterns());
                } else {
                    virtualPatterns.add(mainPattern);
                }

                for (SmartPatternDetails detail : virtualPatterns) {
                    ICraftingPatternDetails offered = detail;
                    if (detail.hasInputFluids() || detail.hasOutputFluids()) {
                        ICraftingPatternDetails fluidPattern = buildAe2fcFluidPatternDetails(detail);
                        if (fluidPattern != null) {
                            offered = fluidPattern;
                        } else {
                            ICraftingPatternDetails ae2fcPattern = resolveAe2fcPatternFromStack(patternStack);
                            if (ae2fcPattern != null) {
                                offered = ae2fcPattern;
                            } else if (logSlotDetails) {
                                debugLog("AE2FC fluid pattern null: slot=" + i
                                    + ", item=" + patternStack.getItem().getClass().getName()
                                    + ", hasTag=" + patternStack.hasTagCompound());
                            }
                        }
                    }
                    expandedPatterns.add(offered);
                    expandedForThisInterface.add(offered);
                    added++;

                    if (logSlotDetails) {
                        debugLog("AE2FC accept slot " + i + ": detail=" + detail.getClass().getName()
                            + ", offered=" + offered.getClass().getName()
                            + ", fluids=" + (detail.hasInputFluids() || detail.hasOutputFluids()));
                    }
                }
            }

            if (!expandedForThisInterface.isEmpty()) {
                interfacePatternMap.put(pushTarget, expandedForThisInterface);
            }

            debugLog("接口展开: host=" + pushTarget.getClass().getName() + ", slots=" + patterns.getSlots() + ", added=" + added);

        } catch (Exception e) {
            System.err.println("[PatternExpander] 展开样板失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isAe2fcTarget(Object target) {
        if (target == null) {
            return false;
        }
        String name = target.getClass().getName();
        return name.equals("com.glodblock.github.common.tile.TileDualInterface")
            || name.equals("com.glodblock.github.common.tile.TileTrioInterface")
            || name.equals("com.glodblock.github.common.part.PartTrioInterface");
    }

    private Class<?> tryLoadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Field resolvePatternsField(Object duality) {
        if (duality == null) {
            return null;
        }
        Class<?> cls = duality.getClass();
        Field cached = cachedPatternsFieldByClass.get(cls);
        if (cached != null) {
            return cached;
        }
        initReflectionFields();
        if (cachedPatternsField != null && cachedPatternsField.getDeclaringClass().isAssignableFrom(cls)) {
            cachedPatternsFieldByClass.put(cls, cachedPatternsField);
            return cachedPatternsField;
        }
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (net.minecraftforge.items.IItemHandler.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    cachedPatternsFieldByClass.put(cls, field);
                    return field;
                }
                if (field.getName().equals("patterns")) {
                    field.setAccessible(true);
                    cachedPatternsFieldByClass.put(cls, field);
                    return field;
                }
                if (!field.getType().isPrimitive()) {
                    try {
                        field.setAccessible(true);
                        Object value = field.get(duality);
                        if (value instanceof net.minecraftforge.items.IItemHandler) {
                            cachedPatternsFieldByClass.put(cls, field);
                            return field;
                        }
                    } catch (Exception e) {
                        // Ignore and continue.
                    }
                }
            }
            current = current.getSuperclass();
        }
        return null;
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

        if (lastProvideCount != expandedPatterns.size()) {
            lastProvideCount = expandedPatterns.size();
            debugLog("提供样板数量: " + lastProvideCount);
        }
    }

    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting table) {
        // 将合成任务推送到原始的ME接口
        // 找到包含这个样板的接口
        for (Map.Entry<Object, List<ICraftingPatternDetails>> entry : interfacePatternMap.entrySet()) {
            if (entry.getValue().contains(patternDetails)) {
                Object target = entry.getKey();
                
                try {
                    // 调用接口的pushPattern方法
                    ICraftingPatternDetails effectiveDetails = patternDetails;
                    if (shouldUseAe2fcPattern(target, patternDetails)) {
                        effectiveDetails = resolveAe2fcPattern(patternDetails);
                    }
                    debugLog("pushPattern: target=" + target.getClass().getName() + ", details=" + patternDetails.getClass().getName() + ", effective=" + effectiveDetails.getClass().getName());
                    DualityInterface duality = getDualityFromTile(target);
                    Set<ICraftingPatternDetails> craftingList = getCraftingList(duality);
                    boolean injected = false;
                    if (craftingList != null && !craftingList.contains(effectiveDetails)) {
                        craftingList.add(effectiveDetails);
                        injected = true;
                    }

                    boolean result;
                    if (duality != null) {
                        result = duality.pushPattern(effectiveDetails, table);
                    } else if (target instanceof DualityInterface) {
                        result = ((DualityInterface) target).pushPattern(effectiveDetails, table);
                    } else {
                        java.lang.reflect.Method pushMethod = target.getClass().getMethod("pushPattern", ICraftingPatternDetails.class, InventoryCrafting.class);
                        Object raw = pushMethod.invoke(target, effectiveDetails, table);
                        result = raw instanceof Boolean && (Boolean) raw;
                    }

                    if (injected && craftingList != null) {
                        craftingList.remove(effectiveDetails);
                    }

                    if (result && shouldFlushAe2fcFluids(target, effectiveDetails)) {
                        flushAe2fcFluidOutputs(target, duality);
                    }

                    return result;
                } catch (Exception e) {
                    System.err.println("[PatternExpander] 推送样板失败: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }
        }

        System.err.println("[PatternExpander] 未找到样板对应接口: " + patternDetails.getPattern());
        debugLog("pushPattern miss: details=" + patternDetails.getClass().getName());
        
        return false;
    }

    private void debugLog(String message) {
        if (DEBUG_LOG) {
            System.out.println("[PatternExpander] " + message);
        }
    }

    private boolean shouldUseAe2fcPattern(Object target, ICraftingPatternDetails patternDetails) {
        if (!(patternDetails instanceof SmartPatternDetails)) {
            return false;
        }
        SmartPatternDetails smart = (SmartPatternDetails) patternDetails;
        if (!smart.hasInputFluids() && !smart.hasOutputFluids()) {
            return false;
        }
        return isAe2fcTarget(target);
    }

    private boolean shouldFlushAe2fcFluids(Object target, ICraftingPatternDetails patternDetails) {
        if (!isAe2fcTarget(target)) {
            return false;
        }
        if (patternDetails instanceof SmartPatternDetails) {
            SmartPatternDetails smart = (SmartPatternDetails) patternDetails;
            return smart.hasInputFluids() || smart.hasOutputFluids();
        }
        String name = patternDetails.getClass().getName();
        return name.equals("com.glodblock.github.util.FluidPatternDetails")
            || name.equals("com.glodblock.github.util.FluidCraftingPatternDetails");
    }

    private DualityInterface resolveItemDuality(Object target) {
        if (target instanceof DualityInterface) {
            return (DualityInterface) target;
        }
        try {
            Method method = target.getClass().getMethod("getInterfaceDuality");
            Object result = method.invoke(target);
            if (result instanceof DualityInterface) {
                return (DualityInterface) result;
            }
        } catch (Exception e) {
            // Ignore and return null.
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Set<ICraftingPatternDetails> getCraftingList(DualityInterface duality) {
        if (duality == null) {
            return null;
        }
        initReflectionFields();
        if (cachedCraftingListField == null) {
            return null;
        }
        try {
            Object value = cachedCraftingListField.get(duality);
            if (value == null) {
                Set<ICraftingPatternDetails> created = new HashSet<>();
                cachedCraftingListField.set(duality, created);
                return created;
            }
            if (value instanceof Set) {
                return (Set<ICraftingPatternDetails>) value;
            }
        } catch (Exception e) {
            // Ignore and return null.
        }
        return null;
    }

    private void flushAe2fcFluidOutputs(Object target, DualityInterface duality) {
        if (!(target instanceof TileEntity) || duality == null) {
            return;
        }
        initAe2fcHelpers();
        if (cachedFakeFluidCheck == null || cachedFluidAdaptorWrap == null) {
            return;
        }

        EnumSet<EnumFacing> faces = resolveTargets(target);
        if (faces == null || faces.isEmpty()) {
            return;
        }

        List<ItemStack> waiting = getWaitingToSend(duality);
        EnumMap<EnumFacing, List<ItemStack>> waitingFacing = getWaitingToSendFacing(duality);
        TileEntity tile = (TileEntity) target;

        if (waitingFacing != null) {
            for (EnumFacing face : faces) {
                List<ItemStack> list = waitingFacing.get(face);
                if (list == null || list.isEmpty()) {
                    continue;
                }
                drainFakeFluidList(tile, face, list);
            }
        }

        if (waiting != null && !waiting.isEmpty()) {
            drainFakeFluidList(tile, faces, waiting);
        }
    }

    private EnumSet<EnumFacing> resolveTargets(Object target) {
        try {
            Method method = target.getClass().getMethod("getTargets");
            Object result = method.invoke(target);
            if (result instanceof EnumSet) {
                @SuppressWarnings("unchecked")
                EnumSet<EnumFacing> set = (EnumSet<EnumFacing>) result;
                return set;
            }
        } catch (Exception e) {
            // Ignore and return null.
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> getWaitingToSend(DualityInterface duality) {
        initReflectionFields();
        if (cachedWaitingToSendField == null) {
            return null;
        }
        try {
            Object value = cachedWaitingToSendField.get(duality);
            if (value instanceof List) {
                return (List<ItemStack>) value;
            }
        } catch (Exception e) {
            // Ignore and return null.
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private EnumMap<EnumFacing, List<ItemStack>> getWaitingToSendFacing(DualityInterface duality) {
        initReflectionFields();
        if (cachedWaitingToSendFacingField == null) {
            return null;
        }
        try {
            Object value = cachedWaitingToSendFacingField.get(duality);
            if (value instanceof EnumMap) {
                return (EnumMap<EnumFacing, List<ItemStack>>) value;
            }
        } catch (Exception e) {
            // Ignore and return null.
        }
        return null;
    }

    private void drainFakeFluidList(TileEntity tile, EnumFacing face, List<ItemStack> list) {
        Iterator<ItemStack> it = list.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (!isFakeFluidItem(stack)) {
                continue;
            }
            ItemStack remainder = pushToNeighbor(tile, face, stack);
            if (remainder.isEmpty()) {
                it.remove();
            } else {
                stack.setCount(remainder.getCount());
                stack.setTagCompound(remainder.getTagCompound());
            }
        }
    }

    private void drainFakeFluidList(TileEntity tile, EnumSet<EnumFacing> faces, List<ItemStack> list) {
        Iterator<ItemStack> it = list.iterator();
        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (!isFakeFluidItem(stack)) {
                continue;
            }
            ItemStack remainder = stack;
            for (EnumFacing face : faces) {
                remainder = pushToNeighbor(tile, face, remainder);
                if (remainder.isEmpty()) {
                    break;
                }
            }
            if (remainder.isEmpty()) {
                it.remove();
            } else {
                stack.setCount(remainder.getCount());
                stack.setTagCompound(remainder.getTagCompound());
            }
        }
    }

    private ItemStack pushToNeighbor(TileEntity tile, EnumFacing face, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        try {
            TileEntity neighbor = tile.getWorld().getTileEntity(tile.getPos().offset(face));
            if (neighbor == null) {
                return stack;
            }
            Object adaptorObj = cachedFluidAdaptorWrap.invoke(null, neighbor, face.getOpposite());
            if (!(adaptorObj instanceof InventoryAdaptor)) {
                return stack;
            }
            InventoryAdaptor adaptor = (InventoryAdaptor) adaptorObj;
            ItemStack remainder = adaptor.addItems(stack);
            return remainder == null ? ItemStack.EMPTY : remainder;
        } catch (Exception e) {
            return stack;
        }
    }

    private boolean isFakeFluidItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (cachedFakeFluidCheck == null) {
            return false;
        }
        try {
            Object result = cachedFakeFluidCheck.invoke(null, stack);
            return result instanceof Boolean && (Boolean) result;
        } catch (Exception e) {
            return false;
        }
    }

    private ICraftingPatternDetails resolveAe2fcPattern(ICraftingPatternDetails patternDetails) {
        try {
            Class<?> clazz = Class.forName("com.glodblock.github.util.FluidCraftingPatternDetails");
            java.lang.reflect.Method method = clazz.getMethod("GetFluidPattern", ItemStack.class, net.minecraft.world.World.class);
            Object result = method.invoke(null, patternDetails.getPattern(), this.world);
            if (result instanceof ICraftingPatternDetails) {
                return (ICraftingPatternDetails) result;
            }
        } catch (Exception e) {
            System.err.println("[PatternExpander] AE2FC 流体样板转换失败: " + e.getMessage());
        }
        return patternDetails;
    }

    private ICraftingPatternDetails resolveAe2fcPatternFromStack(ItemStack patternStack) {
        if (patternStack == null || patternStack.isEmpty()) {
            return null;
        }
        try {
            Class<?> clazz = Class.forName("com.glodblock.github.util.FluidCraftingPatternDetails");
            java.lang.reflect.Method method = clazz.getMethod("GetFluidPattern", ItemStack.class, net.minecraft.world.World.class);
            Object result = method.invoke(null, patternStack, this.world);
            if (result instanceof ICraftingPatternDetails) {
                ICraftingPatternDetails details = (ICraftingPatternDetails) result;
                if (details.getClass().getName().equals("com.glodblock.github.util.FluidCraftingPatternDetails")) {
                    return details;
                }
                debugLog("AE2FC GetFluidPattern returned non-fluid: " + details.getClass().getName());
            }
        } catch (Exception e) {
            System.err.println("[PatternExpander] AE2FC 流体样板创建失败: " + e.getMessage());
        }
        // Fallback: try direct constructor for FluidCraftingPatternDetails
        try {
            Class<?> clazz = Class.forName("com.glodblock.github.util.FluidCraftingPatternDetails");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(ItemStack.class, net.minecraft.world.World.class);
            Object result = ctor.newInstance(patternStack, this.world);
            if (result instanceof ICraftingPatternDetails) {
                ICraftingPatternDetails details = (ICraftingPatternDetails) result;
                if (details.getClass().getName().equals("com.glodblock.github.util.FluidCraftingPatternDetails")) {
                    debugLog("AE2FC ctor used for fluid pattern");
                    return details;
                }
            }
        } catch (Exception e) {
            System.err.println("[PatternExpander] AE2FC 构造流体样板失败: " + e.getMessage());
        }
        return null;
    }

    private ICraftingPatternDetails buildAe2fcFluidPatternDetails(SmartPatternDetails detail) {
        try {
            Class<?> clazz = Class.forName("com.glodblock.github.util.FluidPatternDetails");
            java.lang.reflect.Constructor<?> ctor = clazz.getConstructor(ItemStack.class);
            Object result = ctor.newInstance(detail.getPattern());
            if (!(result instanceof ICraftingPatternDetails)) {
                return null;
            }
            java.lang.reflect.Method setInputs = clazz.getMethod("setInputs", IAEItemStack[].class);
            java.lang.reflect.Method setOutputs = clazz.getMethod("setOutputs", IAEItemStack[].class);
            boolean inputsOk = (Boolean) setInputs.invoke(result, (Object) detail.getInputs());
            boolean outputsOk = (Boolean) setOutputs.invoke(result, (Object) detail.getOutputs());
            if (!inputsOk || !outputsOk) {
                debugLog("AE2FC FluidPatternDetails set failed: inputs=" + inputsOk + ", outputs=" + outputsOk);
                return null;
            }
            debugLog("AE2FC FluidPatternDetails used");
            return (ICraftingPatternDetails) result;
        } catch (Exception e) {
            System.err.println("[PatternExpander] AE2FC FluidPatternDetails 创建失败: " + e.getMessage());
            return null;
        }
    }

    public boolean isBusy() {
        // 检查所有关联的接口是否繁忙
        for (Object target : interfacePatternMap.keySet()) {
            if (target instanceof DualityInterface) {
                if (((DualityInterface) target).isBusy()) {
                    return true;
                }
                continue;
            }
            try {
                java.lang.reflect.Method busyMethod = target.getClass().getMethod("isBusy");
                Object result = busyMethod.invoke(target);
                if (result instanceof Boolean && (Boolean) result) {
                    return true;
                }
            } catch (Exception e) {
                // Ignore and continue.
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
