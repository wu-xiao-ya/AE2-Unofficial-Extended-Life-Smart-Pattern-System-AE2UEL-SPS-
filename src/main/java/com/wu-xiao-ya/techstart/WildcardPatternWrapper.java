package com.lwx1145.techstart;


import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * 閫氶厤绗︽牱鏉垮寘瑁呭櫒 - 褰揂E2闇€瑕佽幏鍙栨牱鏉夸俊鎭椂锛岃嚜鍔ㄥ睍寮€涓鸿櫄鎷熸牱鏉?
 * 杩欐牱鍙互璁╁崟涓€鏍锋澘瀵硅薄鍦ˋE2鍐呴儴鑷姩鍙樻垚19涓櫄鎷熸牱鏉?
 * 
 * 鍘熺悊锛?
 * 1. ItemTest杩斿洖杩欎釜鍖呰鍣ㄨ€屼笉鏄疭martPatternDetails
 * 2. 鍖呰鍣ㄦ寔鏈変竴涓櫄鎷熸牱鏉垮垪琛?
 * 3. 褰揂E2璋冪敤鏂规硶鏃讹紝鍖呰鍣ㄨ繑鍥炵涓€涓櫄鎷熸牱鏉跨殑淇℃伅
 * 4. 褰揂E2灏濊瘯鎵ц鍚堟垚鏃讹紝鍚勪釜铏氭嫙鏍锋澘鍚勫徃鍏惰亴
 */
public class WildcardPatternWrapper implements ICraftingPatternDetails {
    
    private final List<SmartPatternDetails> virtualPatterns;
    private final SmartPatternDetails primaryPattern; // 鐢ㄤ簬鏄剧ず鍜屽熀纭€鎿嶄綔鐨勪富鏍锋澘
    private final ItemStack patternStack;

    public WildcardPatternWrapper(ItemStack patternStack) {
        this.patternStack = patternStack;
        
        // 鍒涘缓涓存椂鏍锋澘浠ヨ幏鍙栭厤鏂瑰垪琛?
        SmartPatternDetails tempPattern = new SmartPatternDetails(patternStack);
        
        // 灞曞紑涓鸿櫄鎷熸牱鏉?
        this.virtualPatterns = tempPattern.expandToVirtualPatterns();
        
        // 浣跨敤绗竴涓櫄鎷熸牱鏉夸綔涓轰富鏍锋澘鐢ㄤ簬鏄剧ず
        this.primaryPattern = (this.virtualPatterns.isEmpty()) ? tempPattern : this.virtualPatterns.get(0);
    }

    @Override
    public ItemStack getPattern() {
        return primaryPattern.getPattern();
    }

    @Override
    public boolean isValidItemForSlot(int slot, ItemStack itemStack, World world) {
        // 妫€鏌ユ槸鍚︿换浣曡櫄鎷熸牱鏉块兘鑳芥帴鍙楄繖涓墿鍝?
        for (SmartPatternDetails vPattern : virtualPatterns) {
            if (vPattern.isValidItemForSlot(slot, itemStack, world)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isCraftable() {
        return primaryPattern.isCraftable();
    }

    @Override
    public IAEItemStack[] getInputs() {
        // 鍙繑鍥炰富鏍锋澘鐨勮緭鍏?
        return primaryPattern.getInputs();
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return primaryPattern.getCondensedInputs();
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return primaryPattern.getCondensedOutputs();
    }

    @Override
    public IAEItemStack getPrimaryOutput() {
        return primaryPattern.getPrimaryOutput();
    }

    @Override
    public IAEItemStack[] getOutputs() {
        // 鍙繑鍥炰富鏍锋澘鐨勮緭鍑猴紙鍚﹀垯AE2浼氭贩娣嗭級
        return primaryPattern.getOutputs();
    }

    @Override
    public boolean canSubstitute() {
        // 鍏佽鏇夸唬 - 杩欐牱鐢ㄦ埛鍙互閫夋嫨鍏朵粬鏉愭枡
        return true;
    }

    @Override
    public List<IAEItemStack> getSubstituteInputs(int slot) {
        // 杩斿洖鎵€鏈夎櫄鎷熸牱鏉跨殑杈撳叆浣滀负鏇夸唬鍝?
        List<IAEItemStack> substitutes = new ArrayList<>();
        for (int i = 0; i < virtualPatterns.size(); i++) {
            SmartPatternDetails vPattern = virtualPatterns.get(i);
            IAEItemStack[] inputs = vPattern.getInputs();
            for (IAEItemStack input : inputs) {
                if (input != null) {
                    substitutes.add(input);
                }
            }
        }
        
        return substitutes;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting inventory, World world) {
        // 鏍规嵁瀹為檯杈撳叆锛屾壘鍒板尮閰嶇殑铏氭嫙鏍锋澘锛岃繑鍥炲叾杈撳嚭
        if (inventory.getSizeInventory() > 0) {
            ItemStack input = inventory.getStackInSlot(0);
            if (!input.isEmpty()) {
                // 妫€鏌ユ瘡涓櫄鎷熸牱鏉挎槸鍚﹁兘鎺ュ彈杩欎釜杈撳叆
                for (int i = 0; i < virtualPatterns.size(); i++) {
                    SmartPatternDetails vPattern = virtualPatterns.get(i);
                    if (vPattern.isValidItemForSlot(0, input, world)) {
                        // 鎵惧埌鍖归厤鐨勮櫄鎷熸牱鏉匡紝杩斿洖鍏惰緭鍑?
                        ItemStack result = vPattern.getOutput(inventory, world);
                        if (!result.isEmpty()) {
                            return result;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getPriority() {
        return primaryPattern.getPriority();
    }

    @Override
    public void setPriority(int priority) {
        primaryPattern.setPriority(priority);
    }
}
