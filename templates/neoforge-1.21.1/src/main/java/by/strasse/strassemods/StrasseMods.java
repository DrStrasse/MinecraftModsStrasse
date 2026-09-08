package by.strasse.strassemods;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;

import by.strasse.strassemods.magic.MagicWandItem;
import by.strasse.strassemods.magic.Wand;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import org.slf4j.Logger;

/**
 * Точка входа мода «Волшебная палочка» на стороне NeoForge.
 *
 * <p>Регистрация идёт через {@link DeferredRegister}: NeoForge заполняет реестры
 * позже, поэтому предметы и компоненты описываются поставщиками, а привязка
 * компонента к общей логике заклинаний выполняется в commonSetup.</p>
 */
@Mod(StrasseMods.MODID)
public class StrasseMods {
    public static final String MODID = "strassemods";

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID);

    /** Компонент с индексом выбранного заклинания; синхронизируется с клиентом. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SELECTED_SPELL =
            DATA_COMPONENTS.register("selected_spell", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Волшебная палочка: внешне обычная палка, внутри — семь заклинаний. */
    public static final DeferredHolder<Item, MagicWandItem> MAGIC_WAND = ITEMS.register("magic_wand",
            () -> new MagicWandItem(new Item.Properties().stacksTo(1).durability(256)));

    public StrasseMods(IEventBus modEventBus, ModContainer modContainer) {
        ITEMS.register(modEventBus);
        DATA_COMPONENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Реестры уже заполнены — отдаём компонент общему коду заклинаний.
        event.enqueueWork(() -> Wand.bindComponent(SELECTED_SPELL.get()));
        LOGGER.info("Strasse Mods инициализирован: волшебная палочка готова");
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MAGIC_WAND.get());
        }
    }
}
