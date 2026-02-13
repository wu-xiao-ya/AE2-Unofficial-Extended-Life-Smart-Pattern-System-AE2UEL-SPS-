package com.lwx1145.techstart;

// EN: Original comment text was corrupted by encoding.
// ZH: 原注释因编码问题已损坏。

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class GuiHandler implements IGuiHandler {

    public static final int PATTERN_EDITOR_GUI = 0;
    public static final int PATTERN_FILTER_GUI = 1;

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case PATTERN_EDITOR_GUI:
                return new ContainerPatternEditor(player);
            case PATTERN_FILTER_GUI:
                return new ContainerPatternFilter(player);
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
            case PATTERN_FILTER_GUI:
                return new GuiPatternFilter(new ContainerPatternFilter(player), player);
            default:
                return null;
        }
    }
}
