package com.lwx1145.techstart.event;

import com.lwx1145.techstart.command.CommandGenerateVirtual;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

/**
 * 服务器事件处理器
 * 在服务器启动时注册命令
 */
public class ServerEventHandler {
    
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        // 注册虚拟样板生成命令
        event.registerServerCommand(new CommandGenerateVirtual());
    }
}
