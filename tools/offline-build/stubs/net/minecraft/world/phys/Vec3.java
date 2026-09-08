package net.minecraft.world.phys;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
public class Vec3 implements Position {
    public final double x;
    public final double y;
    public final double z;
    public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
    public static Vec3 atCenterOf(Vec3i vector) { return null; }
    public Vec3 add(double dx, double dy, double dz) { return null; }
    public Vec3 add(Vec3 other) { return null; }
    public Vec3 subtract(Vec3 other) { return null; }
    public Vec3 subtract(double dx, double dy, double dz) { return null; }
    public Vec3 scale(double factor) { return null; }
    public Vec3 normalize() { return null; }
    public double length() { return 0.0D; }
    public double distanceToSqr(Vec3 other) { return 0.0D; }
    public double x() { return this.x; }
    public double y() { return this.y; }
    public double z() { return this.z; }
}
