package net.minecraft.world.level.block;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
public class Block implements ItemLike {
    public static final int UPDATE_NEIGHBORS = 1;
    public static final int UPDATE_CLIENTS = 2;
    public static final int UPDATE_ALL = 3;
    public BlockState defaultBlockState() { return null; }
    public Item asItem() { return null; }
}
