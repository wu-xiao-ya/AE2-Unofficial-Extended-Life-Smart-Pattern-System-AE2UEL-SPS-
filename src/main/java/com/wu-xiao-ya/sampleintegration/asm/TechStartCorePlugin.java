package com.lwx1145.sampleintegration.asm;


import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 */
@IFMLLoadingPlugin.Name("sampleintegrationCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class TechStartCorePlugin implements IFMLLoadingPlugin {
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
            "com.lwx1145.sampleintegration.asm.DualityInterfaceTransformer",
            "com.lwx1145.sampleintegration.asm.DSurroundItemClassTransformer",
            "com.lwx1145.sampleintegration.asm.MMCEItemStackEmptyCompatTransformer",
            "com.lwx1145.sampleintegration.asm.MMCEJsonUtilsCompatTransformer",
            "com.lwx1145.sampleintegration.asm.MMCEPatternProviderGuiCompatTransformer",
            "com.lwx1145.sampleintegration.asm.MMCEPatternFilterTransformer",
            "com.lwx1145.sampleintegration.asm.MMCEPatternProviderTransformer"
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


