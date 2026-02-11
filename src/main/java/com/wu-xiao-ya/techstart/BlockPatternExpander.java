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
 * 鏍锋澘鎵╁睍鍣ㄦ柟鍧?
 * 褰撹繛鎺ュ埌AE2缃戠粶鏃讹紝鑷姩妫€娴嬬綉缁滀腑鍚湁閫氶厤绗︽牱鏉跨殑ME鎺ュ彛
 * 骞跺皢閫氶厤绗︽牱鏉垮睍寮€涓?9涓櫄鎷熸牱鏉挎敞鍐屽埌鍚堟垚绯荤粺
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
        // 鍏堣幏鍙朤ileEntity锛岄€氱煡瀹冩柟鍧楄鐮村潖
        TileEntity tileEntity = worldIn.getTileEntity(pos);
        if (tileEntity instanceof TileEntityPatternExpander) {
            ((TileEntityPatternExpander) tileEntity).onBlockDestroyed();
        }
        
        // 鐒跺悗璋冪敤鐖剁被鏂规硶绉婚櫎TileEntity
        super.breakBlock(worldIn, pos, state);
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, 
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (hand != EnumHand.MAIN_HAND) {
            return true;
        }
        // 浠呭湪鏈嶅姟绔墦寮€GUI
        if (!worldIn.isRemote && !playerIn.isSneaking()) {
            TileEntity tileEntity = worldIn.getTileEntity(pos);
            if (tileEntity instanceof TileEntityPatternExpander) {
                // 妫€鏌ョ帺瀹舵槸鍚︽湁鏅鸿兘鏍锋澘鐗╁搧
                if (playerIn.getHeldItem(hand).getItem() instanceof ItemTest) {
                    // 鎵撳紑缂栬緫GUI
                    playerIn.openGui(TechStart.INSTANCE, GuiHandler.PATTERN_EDITOR_GUI, 
                                   worldIn, pos.getX(), pos.getY(), pos.getZ());
                    return true;
                } else {
                    playerIn.sendMessage(new net.minecraft.util.text.TextComponentString(
                        "请手持智能样板物品右键此方块进行编辑"));
                    return true;
                }
            }
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
}
