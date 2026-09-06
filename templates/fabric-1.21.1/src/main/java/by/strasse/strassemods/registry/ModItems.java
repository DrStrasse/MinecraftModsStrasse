package by.strasse.strassemods.registry;

import by.strasse.strassemods.StrasseMods;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Реестр предметов мода.
 *
 * В 1.21.1 (Mojang mappings) предметы регистрируются напрямую через
 * {@link Registry#register}. Начиная с 1.21.2+ конструктор Item требует
 * {@code Item.Properties#setId(...)} — см. docs/07-porting.md.
 */
public final class ModItems {
	public static final Item STRASSE_INGOT = register("strasse_ingot", new Item(new Item.Properties()));

	private ModItems() {
	}

	private static Item register(String path, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, StrasseMods.id(path), item);
	}

	/** Вызывается из onInitialize, чтобы гарантированно загрузить класс. */
	public static void init() {
	}
}
