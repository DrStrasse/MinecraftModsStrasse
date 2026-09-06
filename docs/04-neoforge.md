# NeoForge

NeoForge — форк Forge, отделившийся на 1.20.2 и с тех пор развивающийся независимо.
Для версий 1.20.2+ это де-факто основной «тяжёлый» загрузчик: на нём собраны все
крупные современные модпаки (ATM10, FTB NeoTech и т. д.).

Шаблоны: `templates/neoforge-1.21.1`, `neoforge-1.21.8`, `neoforge-1.21.11`, `neoforge-26.2`.
Все — на **ModDevGradle 2.0.146** (актуальный плагин; NeoGradle считается legacy-вариантом).

## 1. Скелет мода

```java
@Mod(StrasseMods.MODID)
public class StrasseMods {
    public static final String MODID = "strassemods";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items  ITEMS  = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public StrasseMods(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
```

Конструктор мод-класса — точка входа. FML сам подставляет в него параметры
известных типов: `IEventBus`, `ModContainer`, `Dist`, `FMLModContainer`.

## 2. Две шины событий — главное, что надо понять

| Шина | Что на ней | Как подписаться |
|---|---|---|
| **Mod event bus** (передаётся в конструктор) | события загрузки: `FMLCommonSetupEvent`, `RegisterEvent`, `BuildCreativeModeTabContentsEvent`, `RegisterPayloadHandlersEvent`, `EntityRenderersEvent` | `modEventBus.addListener(...)` или `@EventBusSubscriber(bus = Bus.MOD)` |
| **Game event bus** (`NeoForge.EVENT_BUS`) | игровые события: `ServerStartingEvent`, `PlayerEvent.*`, `LevelTickEvent`, `LivingDamageEvent` | `NeoForge.EVENT_BUS.register(obj)` или `@EventBusSubscriber` |

Типичная ошибка новичка — подписать игровое событие на mod bus (или наоборот):
слушатель молча не вызовется.

## 3. Реестры: DeferredRegister

Регистрация в NeoForge **отложенная** — объекты создаются в нужный момент цикла загрузки:

```java
public static final DeferredBlock<Block> EXAMPLE_BLOCK =
        BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));

public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM =
        ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

public static final DeferredItem<Item> EXAMPLE_ITEM =
        ITEMS.registerSimpleItem("example_item", new Item.Properties()
                .food(new FoodProperties.Builder().alwaysEdible().nutrition(1).saturationModifier(2f).build()));
```

`DeferredBlock`/`DeferredItem` — это `Supplier`-подобные холдеры; значение берётся
через `.get()` и **только после** фазы регистрации.

Собственная творческая вкладка:

```java
public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("example_tab",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.strassemods"))
                .withTabsBefore(CreativeModeTabs.COMBAT)
                .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
                .displayItems((params, output) -> output.accept(EXAMPLE_ITEM.get()))
                .build());
```

Добавление в **ванильную** вкладку — через событие:

```java
private void addCreative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
        event.accept(EXAMPLE_BLOCK_ITEM);
    }
}
```

## 4. Манифест `neoforge.mods.toml`

Лежит не в `resources`, а в `src/main/templates/META-INF/neoforge.mods.toml` — ModDevGradle
подставляет туда значения из `gradle.properties` (`${mod_id}`, `${mod_version}`,
`${minecraft_version_range}`, `${neo_version}`). Это позволяет держать все версии в одном месте.

```toml
modLoader="javafml"
loaderVersion="${loader_version_range}"
license="${mod_license}"

[[mods]]
modId="${mod_id}"
version="${mod_version}"
displayName="${mod_name}"

[[dependencies.${mod_id}]]
modId="neoforge"
type="required"
versionRange="[${neo_version},)"
ordering="NONE"
side="BOTH"
```

В 26.x поля `modLoader`/`loaderVersion` стали необязательными и из шаблона убраны.

## 5. Конфиги

```java
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK =
            BUILDER.comment("Whether to log the dirt block on common setup").define("logDirtBlock", true);
    public static final ModConfigSpec SPEC = BUILDER.build();
}
```

Регистрируется через `modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)`.
Типы: `CLIENT` (у каждого игрока свой), `COMMON` (обе стороны, не синхронизируется),
`SERVER` (синхронизируется с клиентом и сохраняется в мире), `STARTUP`.

## 6. build.gradle: что важно

```groovy
plugins { id 'net.neoforged.moddev' version '2.0.146' }

java.toolchain.languageVersion = JavaLanguageVersion.of(21)   // 25 для 26.x

neoForge {
    version = project.neo_version

    parchment {                                   // только ≤ 1.21.11
        mappingsVersion = project.parchment_mappings_version
        minecraftVersion = project.parchment_minecraft_version
    }

    runs {
        client { client() }
        server { server(); programArgument '--nogui' }
        gameTestServer { type = "gameTestServer" }
        data { data() }        // в 26.x — clientData() / serverData()
    }
}
```

Access Transformers (`src/main/resources/META-INF/accesstransformer.cfg`) подхватываются
автоматически — отдельная строка в конфиге больше не нужна.

## 7. Полезные команды

```bash
./gradlew runClient
./gradlew runServer
./gradlew runData              # datagen -> src/generated/resources
./gradlew runGameTestServer    # автотесты в игровом мире
./gradlew build
```

## 8. Ссылки

* Документация: <https://docs.neoforged.net/>
* Праймеры миграции по версиям: <https://github.com/neoforged/.github/tree/main/primers>
* MDK-шаблоны (апстрим наших): <https://github.com/neoforgemdks>
* Версии: <https://projects.neoforged.net/neoforged/neoforge>
