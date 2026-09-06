package by.strasse.strassemods;

import by.strasse.strassemods.registry.ModItems;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Точка входа мода на стороне Fabric (entrypoint "main" из fabric.mod.json).
 * Выполняется и на клиенте, и на сервере.
 */
public class StrasseMods implements ModInitializer {
	public static final String MOD_ID = "strassemods";

	// Логгер именуется по mod id — так в логах сразу видно, кто пишет.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Регистрация контента должна происходить именно здесь: реестры ещё открыты.
		ModItems.init();

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
				.register(entries -> entries.accept(ModItems.STRASSE_INGOT));

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(entries -> entries.accept(ModItems.MAGIC_WAND));

		LOGGER.info("Strasse Mods инициализирован: волшебная палочка готова");
	}

	/** Удобный хелпер: strassemods:<path>. */
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
