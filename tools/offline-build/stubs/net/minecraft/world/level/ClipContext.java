package net.minecraft.world.level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
public class ClipContext {
    public enum Block { COLLIDER, OUTLINE, VISUAL, FALLDAMAGE_RESETTING; }
    public enum Fluid { NONE, SOURCE_ONLY, ANY, WATER; }
    public ClipContext(Vec3 from, Vec3 to, ClipContext.Block block, ClipContext.Fluid fluid, Entity entity) { }
}
