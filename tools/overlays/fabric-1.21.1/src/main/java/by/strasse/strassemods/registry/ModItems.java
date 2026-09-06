package by.strasse.strassemods.registry;

import com.mojang.serialization.Codec;

import by.strasse.strassemods.StrasseMods;
import by.strasse.strassemods.magic.MagicWandItem;
import by.strasse.strassemods.magic.Wand;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;

/**
 * Реестр предметов и компонентов данных мода (сторона Fabric).
 *
 * <p>В Fabric регистрация выполняется немедленно, поэтому важен порядок
 * статических полей: компонент создаётся раньше предмета, который его
 * использует как значение по умолчанию.</p>
 */
public final class ModItems {
	/** Компонент с индексом выбранного заклинания; синхронизируется с клиентом для подсказки. */
	public static final DataComponentType<Integer> SELECTED_SPELL = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			StrasseMods.id("selected_spell"),
			DataComponentType.<Integer>builder()
					.persistent(Codec.INT)
					.networkSynchronized(ByteBufCodecs.VAR_INT)
					.build());

	/** Волшебная палочка: внешне обычная палка, внутри — семь заклинаний. */
	public static final Item MAGIC_WAND = register("magic_wand", new MagicWandItem(new Item.Properties()
			.stacksTo(1)
			.durability(256)
			.component(SELECTED_SPELL, 0)));

	public static final Item STRASSE_INGOT = register("strasse_ingot", new Item(new Item.Properties()));

	private ModItems() {
	}

	private static Item register(String path, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, StrasseMods.id(path), item);
	}

	/** Вызывается из onInitialize: подгружает класс и связывает компонент с логикой палочки. */
	public static void init() {
		Wand.bindComponent(SELECTED_SPELL);
	}
}
