package com.lwx1145.techstart.asm;


import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * CoreMod鎻掍欢 - 娉ㄥ唽ASM Transformer
 * 杩欎釜鎻掍欢浼氬湪Minecraft鍚姩鏃╂湡鍔犺浇锛屾敞鍐屾垜浠殑绫昏浆鎹㈠櫒
 */
@IFMLLoadingPlugin.Name("TechStartCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class TechStartCorePlugin implements IFMLLoadingPlugin {
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
            "com.lwx1145.techstart.asm.DualityInterfaceTransformer",
            "com.lwx1145.techstart.asm.DSurroundItemClassTransformer"
        };
    }
    
    @Override
    public String getModContainerClass() {
        return null;
    }
    
    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }
    
    @Override
    public void injectData(Map<String, Object> data) {
    }
    
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
