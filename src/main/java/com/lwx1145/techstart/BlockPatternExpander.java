package com.lwx1145.techstart;

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
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 样板扩展器方块
 * 当连接到AE2网络时，自动检测网络中含有通配符样板的ME接口
 * 并将通配符样板展开为19个虚拟样板注册到合成系统
 */
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
        // 先获取TileEntity，通知它方块被破坏
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityPatternExpander) {
            ((TileEntityPatternExpander) tileEntity).onBlockDestroyed();
        }
        
        // 然后调用父类方法移除TileEntity
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, 
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        // 仅在服务端打开GUI
        if (!worldIn.isRemote && !playerIn.isSneaking()) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileEntityPatternExpander) {
                // 检查玩家是否有智能样板物品
                if (playerIn.getHeldItem(hand).getItem() instanceof ItemTest) {
                    // 打开编辑GUI
                    playerIn.openGui(TechStart.INSTANCE, GuiHandler.PATTERN_EDITOR_GUI, 
                                   worldIn, pos.getX(), pos.getY(), pos.getZ());
                    return true;
                } else {
                    playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "§c请手持智能样板物品右键此方块进行编辑"));
                    return true;
                }
            }
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
}
