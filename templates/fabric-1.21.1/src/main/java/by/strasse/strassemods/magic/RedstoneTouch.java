package by.strasse.strassemods.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.Vec3;

/**
 * «Магическое прикосновение» к механизмам: то, что Алохомора умеет делать
 * с блоком на расстоянии — переключать рычаги, нажимать кнопки, открывать
 * двери и люки, двигать поршни, срабатывать раздатчиками.
 *
 * <p>Поиск ведётся по объёму, а не по лучу, поэтому стены не мешают:
 * достаточно, чтобы механизм оказался в радиусе {@link Wand#REDSTONE_RADIUS}
 * блоков от точки прицела.</p>
 *
 * <p>Только ванильный API — класс общий для Fabric и NeoForge.</p>
 */
public final class RedstoneTouch {
	private RedstoneTouch() {
	}

	/** Реагирует ли блок на магический сигнал. */
	public static boolean isActuatable(BlockState state) {
		return state.getBlock() instanceof LeverBlock
				|| state.getBlock() instanceof ButtonBlock
				|| isPiston(state)
				|| state.is(Blocks.DISPENSER)
				|| state.is(Blocks.DROPPER)
				|| isOpenable(state);
	}

	/**
	 * Ближайший к центру механизм в кубе со стороной {@code 2 * radius + 1}.
	 * Видимость не проверяется: рычаг за стеной находится так же, как открытый.
	 *
	 * @return позиция механизма или {@code null}, если в радиусе ничего нет
	 */
	public static BlockPos findNearest(ServerLevel level, BlockPos center, int radius) {
		Vec3 origin = Vec3.atCenterOf(center);
		BlockPos best = null;
		double bestDistance = Double.MAX_VALUE;

		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
				center.offset(radius, radius, radius))) {
			if (!isActuatable(level.getBlockState(pos))) {
				continue;
			}

			double distance = Vec3.atCenterOf(pos).distanceToSqr(origin);

			if (distance < bestDistance) {
				bestDistance = distance;
				best = pos.immutable();
			}
		}

		return best;
	}

	/**
	 * Подаёт магический сигнал на механизм.
	 *
	 * @return {@code true}, если состояние блока действительно изменилось
	 */
	public static boolean activate(ServerLevel level, BlockPos pos, Player player) {
		BlockState state = level.getBlockState(pos);

		if (state.getBlock() instanceof LeverBlock) {
			return flipLever(level, pos, state);
		}

		if (state.getBlock() instanceof ButtonBlock) {
			return pressButton(level, pos, state);
		}

		if (isPiston(state)) {
			return togglePiston(level, pos, state);
		}

		if (state.is(Blocks.DISPENSER) || state.is(Blocks.DROPPER)) {
			// Раздатчик и выбрасыватель срабатывают из своего же запланированного тика.
			level.scheduleTick(pos, state.getBlock(), 4);
			play(level, pos, SoundEvents.DISPENSER_DISPENSE, 1.0F);
			return true;
		}

		if (isOpenable(state)) {
			return toggleOpenable(level, pos, state, player);
		}

		return false;
	}

	/** Дверь, люк или калитка: открыть, если закрыто, и наоборот. Работает и с железными. */
	public static boolean toggleOpenable(ServerLevel level, BlockPos pos, BlockState state, Player player) {
		if (!isOpenable(state)) {
			return false;
		}

		boolean opened = !state.getValue(BlockStateProperties.OPEN);

		if (state.getBlock() instanceof DoorBlock door) {
			// У двери две половины — вешаем на ванильный метод, он обновит обе.
			door.setOpen(player, level, state, pos, opened);
			return true;
		}

		level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, opened), Block.UPDATE_ALL);
		play(level, pos, opened ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, opened ? 1.1F : 0.9F);
		return true;
	}

	/** Рычаг: переключаем сигнал — именно так меняют состояние поршни, лампы и двери за стеной. */
	private static boolean flipLever(ServerLevel level, BlockPos pos, BlockState state) {
		BlockState flipped = state.cycle(BlockStateProperties.POWERED);
		level.setBlock(pos, flipped, Block.UPDATE_ALL);
		spreadUpdate(level, pos, flipped.getBlock());
		play(level, pos, SoundEvents.LEVER_CLICK, flipped.getValue(BlockStateProperties.POWERED) ? 0.6F : 0.5F);
		return true;
	}

	/** Кнопка: зажимаем и планируем тик — ванильная логика сама её отожмёт. */
	private static boolean pressButton(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.getValue(BlockStateProperties.POWERED)) {
			return false;
		}

		level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, true), Block.UPDATE_ALL);
		level.scheduleTick(pos, state.getBlock(), 20);
		spreadUpdate(level, pos, state.getBlock());
		play(level, pos, SoundEvents.STONE_BUTTON_CLICK_ON, 0.6F);
		return true;
	}

	/**
	 * Поршень без источника питания: выдвигаем или втягиваем штангу напрямую.
	 *
	 * <p>Если поршень запитан по-настоящему, состояние обычно меняют не здесь,
	 * а рычагом или кнопкой рядом — тогда работает штатная ванильная механика
	 * с анимацией и толканием блоков. Магическое положение держится до
	 * следующего обновления цепи.</p>
	 */
	private static boolean togglePiston(ServerLevel level, BlockPos pos, BlockState state) {
		Direction facing = state.getValue(BlockStateProperties.FACING);
		boolean extended = state.getValue(BlockStateProperties.EXTENDED);
		BlockPos headPos = pos.relative(facing);
		BlockState headState = level.getBlockState(headPos);

		if (extended) {
			if (!headState.is(Blocks.PISTON_HEAD) || headState.getValue(BlockStateProperties.FACING) != facing) {
				return false;
			}

			level.removeBlock(headPos, false);
			level.setBlock(pos, state.setValue(BlockStateProperties.EXTENDED, false), Block.UPDATE_ALL);
			play(level, pos, SoundEvents.PISTON_CONTRACT, 0.7F);
		} else {
			if (!headState.canBeReplaced()) {
				// Перед поршнем блок — толкать его магией не беремся, пусть двигает редстоун.
				return false;
			}

			BlockState head = Blocks.PISTON_HEAD.defaultBlockState()
					.setValue(BlockStateProperties.FACING, facing)
					.setValue(BlockStateProperties.PISTON_TYPE,
							state.is(Blocks.STICKY_PISTON) ? PistonType.STICKY : PistonType.DEFAULT);

			level.setBlock(pos, state.setValue(BlockStateProperties.EXTENDED, true), Block.UPDATE_ALL);
			level.setBlock(headPos, head, Block.UPDATE_ALL);
			play(level, pos, SoundEvents.PISTON_EXTEND, 0.6F);
		}

		spreadUpdate(level, pos, state.getBlock());
		return true;
	}

	/** Двери, люки и калитки — но не бочки и прочие блоки со свойством {@code open}. */
	private static boolean isOpenable(BlockState state) {
		return (state.getBlock() instanceof DoorBlock
				|| state.getBlock() instanceof TrapDoorBlock
				|| state.getBlock() instanceof FenceGateBlock)
				&& state.hasProperty(BlockStateProperties.OPEN);
	}

	private static boolean isPiston(BlockState state) {
		return state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON);
	}

	/**
	 * Рассылает обновление на два блока вокруг: сигнал рычага проходит и через
	 * блок, к которому тот прикреплён, поэтому одного {@code UPDATE_NEIGHBORS} мало.
	 */
	private static void spreadUpdate(ServerLevel level, BlockPos pos, Block block) {
		level.updateNeighborsAt(pos, block);

		for (Direction direction : Direction.values()) {
			level.updateNeighborsAt(pos.relative(direction), block);
		}
	}

	private static void play(ServerLevel level, BlockPos pos, SoundEvent sound, float pitch) {
		level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.7F, pitch);
	}
}
