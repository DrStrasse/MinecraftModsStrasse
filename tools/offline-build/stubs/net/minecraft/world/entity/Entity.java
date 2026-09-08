package net.minecraft.world.entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
public abstract class Entity {
    public boolean hurtMarked;
    public boolean hasImpulse;
    public float fallDistance;
    public Level level() { return null; }
    public Vec3 position() { return null; }
    public BlockPos blockPosition() { return null; }
    public AABB getBoundingBox() { return null; }
    public Vec3 getDeltaMovement() { return null; }
    public void setDeltaMovement(Vec3 movement) { }
    public void setDeltaMovement(double x, double y, double z) { }
    public Vec3 getEyePosition() { return null; }
    public Vec3 getViewVector(float partialTicks) { return null; }
    public float getBbHeight() { return 0.0F; }
    public boolean isSpectator() { return false; }
    public double getX() { return 0.0D; }
    public double getY() { return 0.0D; }
    public double getZ() { return 0.0D; }
}
