package by.strasse.strassemods.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Набор заклинаний волшебной палочки.
 *
 * <p>Каждое заклинание — константа перечисления со своей реализацией
 * {@link #cast(ServerLevel, Player, ItemStack)}. Метод возвращает {@code false},
 * если применить заклинание не к чему: тогда палочка не тратит прочность
 * и не уходит в перезарядку.</p>
 *
 * <p>Все заклинания выполняются только на сервере.</p>
 */
public enum Spell {
	/** Люмос — ставит источник света там, куда смотрит игрок. */
	LUMOS("lumos", 20) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			BlockHitResult hit = MagicRay.blockTarget(player, Wand.RANGE);
			BlockPos target;

			if (hit.getType() == HitResult.Type.BLOCK) {
				target = hit.getBlockPos().relative(hit.getDirection());
			} else {
				target = BlockPos.containing(player.getEyePosition().add(player.getViewVector(1.0F).scale(4.0D)));
			}

			if (!level.isEmptyBlock(target)) {
				return false;
			}

			level.setBlockAndUpdate(target, Blocks.LIGHT.defaultBlockState()
					.setValue(LightBlock.LEVEL, LightBlock.MAX_LEVEL));
			MagicRay.trail(level, wandTip(player), Vec3.atCenterOf(target), ParticleTypes.END_ROD);
			MagicRay.burst(level, Vec3.atCenterOf(target), ParticleTypes.END_ROD,
					SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F);
			return true;
		}
	},

	/** Нокс — гасит все огоньки Люмоса вокруг игрока. */
	NOX("nox", 20) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			BlockPos center = player.blockPosition();
			int removed = 0;

			for (BlockPos pos : BlockPos.betweenClosed(center.offset(-10, -10, -10), center.offset(10, 10, 10))) {
				if (level.getBlockState(pos).is(Blocks.LIGHT)) {
					level.removeBlock(pos.immutable(), false);
					removed++;
				}
			}

			if (removed == 0) {
				return false;
			}

			MagicRay.burst(level, player.position().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE,
					SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.2F);
			return true;
		}
	},

	/**
	 * Алохомора — открывает замки и подаёт магический сигнал редстоуна.
	 *
	 * <p>Сначала пробует блок прямо под прицелом, затем ищет ближайший механизм
	 * в радиусе {@link Wand#REDSTONE_RADIUS} блоков вокруг точки прицела — по объёму,
	 * а не по лучу, поэтому рычаг за стенкой тоже сработает.</p>
	 */
	ALOHOMORA("alohomora", 30) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			BlockHitResult hit = MagicRay.blockTarget(player, Wand.RANGE);
			BlockPos aim = hit.getType() == HitResult.Type.BLOCK
					? hit.getBlockPos()
					: BlockPos.containing(player.getEyePosition().add(player.getViewVector(1.0F).scale(Wand.RANGE)));

			// 1. Точное попадание: дверь, люк, калитка, рычаг, кнопка, поршень, раздатчик.
			BlockPos target = RedstoneTouch.isActuatable(level.getBlockState(aim)) ? aim : null;

			// 2. Иначе — ближайший механизм в радиусе, даже спрятанный за стеной.
			if (target == null) {
				target = RedstoneTouch.findNearest(level, aim, Wand.REDSTONE_RADIUS);
			}

			if (target == null || !RedstoneTouch.activate(level, target, player)) {
				return false;
			}

			Vec3 center = Vec3.atCenterOf(target);
			MagicRay.trail(level, wandTip(player), center, ParticleTypes.ENCHANT);
			MagicRay.burst(level, center, ParticleTypes.ELECTRIC_SPARK, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F);
			return true;
		}
	},

	/** Вингардиум Левиоса — поднимает в воздух сущность или предмет под прицелом. */
	WINGARDIUM_LEVIOSA("wingardium_leviosa", 40) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			Entity target = MagicRay.entityTarget(player, Wand.RANGE);

			if (target == null) {
				return false;
			}

			if (target instanceof LivingEntity living) {
				// Левитация корректно синхронизируется с клиентом и мягко опускает цель обратно.
				living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 120, 0, false, true));
			} else {
				target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.55D, 0.0D));
				target.hasImpulse = true;
				target.hurtMarked = true;
			}

			target.fallDistance = 0.0F;

			MagicRay.trail(level, wandTip(player), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
					ParticleTypes.ENCHANT);
			MagicRay.burst(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
					ParticleTypes.END_ROD, SoundEvents.ILLUSIONER_CAST_SPELL, 1.4F);
			return true;
		}
	},

	/** Акцио — притягивает цель к заклинателю. */
	ACCIO("accio", 30) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			Entity target = MagicRay.entityTarget(player, Wand.RANGE);

			if (target == null) {
				return false;
			}

			Vec3 pull = player.position().add(0.0D, 0.4D, 0.0D).subtract(target.position()).normalize().scale(0.85D);
			target.setDeltaMovement(pull.x, pull.y + 0.25D, pull.z);
			target.hasImpulse = true;
			target.hurtMarked = true;
			target.fallDistance = 0.0F;

			MagicRay.trail(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D), wandTip(player),
					ParticleTypes.GLOW);
			MagicRay.burst(level, wandTip(player), ParticleTypes.GLOW, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.3F);
			return true;
		}
	},

	/** Депульсо — отталкивает цель от заклинателя. */
	DEPULSO("depulso", 30) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			Entity target = MagicRay.entityTarget(player, Wand.RANGE);

			if (target == null) {
				return false;
			}

			Vec3 push = target.position().subtract(player.position()).normalize().scale(1.15D);
			target.setDeltaMovement(push.x, 0.45D, push.z);
			target.hasImpulse = true;
			target.hurtMarked = true;

			MagicRay.trail(level, wandTip(player), target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
					ParticleTypes.WITCH);
			MagicRay.burst(level, target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D),
					ParticleTypes.WITCH, SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F);
			return true;
		}
	},

	/** Инсендио — поджигает блок под прицелом. */
	INCENDIO("incendio", 60) {
		@Override
		public boolean cast(ServerLevel level, Player player, ItemStack wand) {
			BlockHitResult hit = MagicRay.blockTarget(player, Wand.RANGE);

			if (hit.getType() != HitResult.Type.BLOCK) {
				return false;
			}

			BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());

			if (!level.isEmptyBlock(firePos)) {
				return false;
			}

			level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
			MagicRay.trail(level, wandTip(player), Vec3.atCenterOf(firePos), ParticleTypes.FLAME);
			MagicRay.burst(level, Vec3.atCenterOf(firePos), ParticleTypes.FLAME,
					SoundEvents.FLINTANDSTEEL_USE, 1.0F);
			return true;
		}
	};

	private static final Spell[] VALUES = values();

	private final String id;
	private final int cooldownTicks;

	Spell(String id, int cooldownTicks) {
		this.id = id;
		this.cooldownTicks = cooldownTicks;
	}

	/**
	 * Применяет заклинание.
	 *
	 * @return {@code true}, если заклинание сработало
	 */
	public abstract boolean cast(ServerLevel level, Player player, ItemStack wand);

	public String id() {
		return this.id;
	}

	public int cooldownTicks() {
		return this.cooldownTicks;
	}

	public String translationKey() {
		return "spell." + Wand.MOD_ID + "." + this.id;
	}

	public MutableComponent displayName() {
		return Component.translatable(this.translationKey());
	}

	public Spell next() {
		return VALUES[(this.ordinal() + 1) % VALUES.length];
	}

	public static Spell byIndex(int index) {
		return VALUES[Math.floorMod(index, VALUES.length)];
	}

	/** Точка, из которой вылетают частицы — чуть ниже и правее глаз игрока. */
	static Vec3 wandTip(Player player) {
		return player.getEyePosition().subtract(0.0D, 0.15D, 0.0D);
	}
}
