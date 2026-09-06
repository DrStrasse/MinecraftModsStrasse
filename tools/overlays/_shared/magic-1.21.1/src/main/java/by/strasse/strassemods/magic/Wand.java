package by.strasse.strassemods.magic;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

/**
 * Общие константы волшебной палочки и доступ к компоненту с выбранным заклинанием.
 *
 * <p>Класс намеренно не зависит ни от Fabric, ни от NeoForge: сам компонент
 * регистрируется загрузчиком (у каждого свой API реестров) и передаётся сюда
 * через {@link #bindComponent(DataComponentType)} на старте мода.</p>
 */
public final class Wand {
	/** Идентификатор мода — используется в ключах локализации. */
	public static final String MOD_ID = "strassemods";

	/** Дальность действия заклинаний в блоках. */
	public static final double RANGE = 24.0D;

	private static DataComponentType<Integer> selectedSpellComponent;

	private Wand() {
	}

	/** Вызывается загрузчиком после регистрации компонента. */
	public static void bindComponent(DataComponentType<Integer> component) {
		selectedSpellComponent = component;
	}

	public static DataComponentType<Integer> component() {
		if (selectedSpellComponent == null) {
			throw new IllegalStateException("Компонент выбранного заклинания ещё не зарегистрирован");
		}

		return selectedSpellComponent;
	}

	/** Заклинание, записанное в предмете (по умолчанию — первое в списке). */
	public static Spell selectedSpell(ItemStack stack) {
		Integer index = stack.get(component());
		return index == null ? Spell.LUMOS : Spell.byIndex(index);
	}

	/** Переключает палочку на следующее заклинание и возвращает его. */
	public static Spell cycleSpell(ItemStack stack) {
		Spell next = selectedSpell(stack).next();
		stack.set(component(), next.ordinal());
		return next;
	}
}
