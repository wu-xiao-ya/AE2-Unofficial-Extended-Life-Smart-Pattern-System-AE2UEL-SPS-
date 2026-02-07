// GUI处理器
package com.lwx1145.techstart;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiHandler implements IGuiHandler {

    public static final int PATTERN_EDITOR_GUI = 0;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case PATTERN_EDITOR_GUI:
                return new ContainerPatternEditor(player);
            default:
                return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case PATTERN_EDITOR_GUI:
                return new GuiPatternEditor(new ContainerPatternEditor(player), player);
            default:
                return null;
        }
    }
}