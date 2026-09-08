package net.minecraft.world.level.block.state;
import net.minecraft.world.level.block.Block;
public abstract class BlockBehaviour {
    public abstract static class BlockStateBase extends StateHolder<Block, BlockState> {
        public Block getBlock() { return null; }
        public boolean is(Block block) { return false; }
        public boolean canBeReplaced() { return false; }
    }
    public static class Properties {
        public static BlockBehaviour.Properties of() { return null; }
    }
}
