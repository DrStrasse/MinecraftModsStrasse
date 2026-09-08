package net.minecraft.world.level.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
public abstract class BaseFireBlock extends Block {
    public static BlockState getState(BlockGetter level, BlockPos pos) { return null; }
}
