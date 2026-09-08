package net.minecraft.server.level;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
public class ServerLevel extends Level {
    public <T extends ParticleOptions> int sendParticles(T type, double x, double y, double z, int count,
            double xDist, double yDist, double zDist, double speed) { return 0; }
}
