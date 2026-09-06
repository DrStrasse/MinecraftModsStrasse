package by.strasse.strassemods.magic;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Прицеливание и визуальные эффекты заклинаний.
 * Только ванильный API — код одинаково работает и на Fabric, и на NeoForge.
 */
public final class MagicRay {
	private MagicRay() {
	}

	/** Луч по блокам из глаз игрока. */
	public static BlockHitResult blockTarget(Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getViewVector(1.0F).scale(range));
		return player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
	}

	/**
	 * Ближайшая сущность на луче взгляда: мобы, игроки, лодки, выпавшие предметы.
	 * Возвращает {@code null}, если под прицелом никого нет.
	 */
	public static Entity entityTarget(Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getViewVector(1.0F);
		Vec3 end = eye.add(look.scale(range));
		AABB searchBox = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);

		Entity closest = null;
		double closestDistance = range * range;

		List<Entity> candidates = player.level().getEntities(player, searchBox, entity -> !entity.isSpectator());

		for (Entity candidate : candidates) {
			Optional<Vec3> hit = candidate.getBoundingBox().inflate(0.35D).clip(eye, end);

			if (hit.isEmpty()) {
				continue;
			}

			double distance = eye.distanceToSqr(hit.get());

			if (distance < closestDistance) {
				closestDistance = distance;
				closest = candidate;
			}
		}

		return closest;
	}

	/** Вспышка частиц и звук в точке применения заклинания. */
	public static void burst(ServerLevel level, Vec3 pos, ParticleOptions particle, SoundEvent sound, float pitch) {
		level.sendParticles(particle, pos.x, pos.y, pos.z, 16, 0.28D, 0.28D, 0.28D, 0.02D);
		level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, 0.8F, pitch);
	}

	/** Дорожка частиц от палочки до цели. */
	public static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
		Vec3 delta = to.subtract(from);
		int steps = (int) Math.max(6.0D, Math.min(48.0D, delta.length() * 2.5D));

		for (int step = 0; step <= steps; step++) {
			Vec3 point = from.add(delta.scale((double) step / (double) steps));
			level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}
}
