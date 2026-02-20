package com.lwx1145.sampleintegration;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BlockPatternExpander extends Block implements ITileEntityProvider {

    public BlockPatternExpander() {
        super(Material.IRON);
        setTranslationKey("sampleintegration.pattern_expander");
        setRegistryName("sampleintegration", "pattern_expander");
        setCreativeTab(CreativeTabs.MISC);
        setHardness(3.0F);
        setResistance(10.0F);
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TileEntityPatternExpander();
    }

    @Override
    public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityPatternExpander) {
            ((TileEntityPatternExpander) tileEntity).onBlockDestroyed();
        }
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }

        // Always consume this block interaction so held items won't try to place/use.
        if (playerIn.isSneaking()) {
            return true;
        }
        if (worldIn.isRemote) {
            return true;
        }

        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityPatternExpander) {
            if (playerIn.getHeldItem(hand).getItem() instanceof ItemTest) {
                playerIn.openGui(TechStart.INSTANCE, GuiHandler.PATTERN_EDITOR_GUI,
                    worldIn, pos.getX(), pos.getY(), pos.getZ());
            } else {
                playerIn.sendMessage(new TextComponentTranslation("message.sampleintegration.no_pattern"));
            }
        }

        return true;
    }
}


