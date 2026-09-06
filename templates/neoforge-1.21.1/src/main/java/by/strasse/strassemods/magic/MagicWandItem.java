package by.strasse.strassemods.magic;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Волшебная палочка.
 *
 * <ul>
 *     <li>ПКМ — применить выбранное заклинание;</li>
 *     <li>Shift + ПКМ — переключиться на следующее заклинание.</li>
 * </ul>
 *
 * Выбранное заклинание хранится в компоненте данных предмета, поэтому
 * переживает перезаход в мир и синхронизируется с клиентом для подсказки.
 */
public class MagicWandItem extends Item {
	public MagicWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		// Вся логика — на сервере; клиент просто проигрывает анимацию руки.
		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResultHolder.success(stack);
		}

		if (player.isSecondaryUseActive()) {
			Spell selected = Wand.cycleSpell(stack);
			player.displayClientMessage(Component.translatable("message." + Wand.MOD_ID + ".spell_selected",
					selected.displayName().withStyle(ChatFormatting.LIGHT_PURPLE)), true);
			serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8F, 1.4F);
			return InteractionResultHolder.consume(stack);
		}

		Spell spell = Wand.selectedSpell(stack);

		if (!spell.cast(serverLevel, player, stack)) {
			player.displayClientMessage(Component.translatable("message." + Wand.MOD_ID + ".spell_failed",
					spell.displayName()).withStyle(ChatFormatting.RED), true);
			serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.4F, 0.7F);
			return InteractionResultHolder.fail(stack);
		}

		player.getCooldowns().addCooldown(this, spell.cooldownTicks());
		player.awardStat(Stats.ITEM_USED.get(this));

		if (!player.getAbilities().instabuild) {
			stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
		}

		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		Spell spell;

		try {
			spell = Wand.selectedSpell(stack);
		} catch (IllegalStateException exception) {
			// Компонент ещё не привязан (например, подсказка запрошена слишком рано).
			spell = Spell.LUMOS;
		}

		tooltip.add(Component.translatable("tooltip." + Wand.MOD_ID + ".magic_wand.spell",
				spell.displayName().withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("tooltip." + Wand.MOD_ID + ".magic_wand.hint")
				.withStyle(ChatFormatting.DARK_GRAY));
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}
}
