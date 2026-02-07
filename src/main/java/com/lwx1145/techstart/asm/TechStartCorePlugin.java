package com.lwx1145.techstart.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * CoreMod插件 - 注册ASM Transformer
 * 这个插件会在Minecraft启动早期加载，注册我们的类转换器
 */
@IFMLLoadingPlugin.Name("TechStartCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
public class TechStartCorePlugin implements IFMLLoadingPlugin {
    
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
            "com.lwx1145.techstart.asm.DualityInterfaceTransformer"
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
