package net.minecraft.core;
public class BlockPos extends Vec3i {
    public BlockPos(int x, int y, int z) { super(x, y, z); }
    public BlockPos offset(int dx, int dy, int dz) { return null; }
    public BlockPos relative(Direction direction) { return null; }
    public BlockPos relative(Direction direction, int distance) { return null; }
    public BlockPos immutable() { return null; }
    public static BlockPos containing(Position position) { return null; }
    public static Iterable<BlockPos> betweenClosed(BlockPos first, BlockPos second) { return null; }
}
