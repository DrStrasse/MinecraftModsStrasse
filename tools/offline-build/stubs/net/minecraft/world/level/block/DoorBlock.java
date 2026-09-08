package net.minecraft.world.level.block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
public class DoorBlock extends Block {
    public static final BooleanProperty OPEN = null;
    public void setOpen(Entity entity, Level level, BlockState state, BlockPos pos, boolean open) { }
}
