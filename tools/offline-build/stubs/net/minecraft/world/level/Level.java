package net.minecraft.world.level;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
public abstract class Level implements BlockGetter {
    public boolean isClientSide;
    public BlockState getBlockState(BlockPos pos) { return null; }
    public boolean isEmptyBlock(BlockPos pos) { return false; }
    public boolean setBlock(BlockPos pos, BlockState state, int flags) { return false; }
    public boolean setBlockAndUpdate(BlockPos pos, BlockState state) { return false; }
    public boolean removeBlock(BlockPos pos, boolean isMoving) { return false; }
    public BlockHitResult clip(ClipContext context) { return null; }
    public List<Entity> getEntities(Entity except, AABB area, Predicate<? super Entity> filter) { return null; }
    public void playSound(Player player, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch) { }
    public void playSound(Player player, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch) { }
    public void scheduleTick(BlockPos pos, Block block, int delay) { }
    public void updateNeighborsAt(BlockPos pos, Block block) { }
}
