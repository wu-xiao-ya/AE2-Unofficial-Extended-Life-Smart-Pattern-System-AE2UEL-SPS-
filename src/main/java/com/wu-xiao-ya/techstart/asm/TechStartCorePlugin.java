package com.lwx1145.techstart.asm;


import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 * EN: Original comment text was corrupted by encoding.
 * ZH: 原注释因编码问题已损坏。
 */
@IFMLLoadingPlugin.Name("TechStartCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class TechStartCorePlugin implements IFMLLoadingPlugin {
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
            "com.lwx1145.techstart.asm.DualityInterfaceTransformer",
            "com.lwx1145.techstart.asm.DSurroundItemClassTransformer",
            "com.lwx1145.techstart.asm.MMCEPatternFilterTransformer",
            "com.lwx1145.techstart.asm.MMCEPatternProviderTransformer"
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
