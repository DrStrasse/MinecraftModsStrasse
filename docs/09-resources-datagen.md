# Ресурсы, датапаки и datagen

## 1. Где что лежит

```
src/main/resources/
├── assets/strassemods/            — клиентские ресурсы (resource pack)
│   ├── lang/en_us.json, ru_ru.json
│   ├── models/item/*.json
│   ├── models/block/*.json
│   ├── blockstates/*.json
│   ├── items/*.json               — client items, с 1.21.4
│   ├── textures/item/*.png
│   └── sounds.json
├── data/strassemods/              — серверные данные (data pack)
│   ├── recipe/*.json              — с 1.21: recipe, не recipes
│   ├── loot_table/**/*.json       — с 1.21: единственное число
│   ├── advancement/*.json
│   ├── tags/**/*.json
│   └── enchantment/*.json         — зачарования как данные, с 1.21
├── fabric.mod.json | META-INF/neoforge.mods.toml
└── pack.mcmeta                    — только если мод грузит доп. паки
```

Внимание: в 1.21 Mojang перевела папки датапака в единственное число
(`recipes` → `recipe`, `loot_tables` → `loot_table`, `advancements` → `advancement`).
При порте с 1.20.x это одна из самых частых «тихих» поломок — файлы просто
перестают читаться.

## 2. Ключевые форматы

Предмет (модель, ≤ 1.21.3 и как база для client item):

```json
{ "parent": "minecraft:item/generated",
  "textures": { "layer0": "strassemods:item/strasse_ingot" } }
```

Client item (обязателен с 1.21.4), `assets/strassemods/items/strasse_ingot.json`:

```json
{ "model": { "type": "minecraft:model", "model": "strassemods:item/strasse_ingot" } }
```

Локализация, `assets/strassemods/lang/ru_ru.json`:

```json
{ "item.strassemods.strasse_ingot": "Слиток Штрассе",
  "itemGroup.strassemods": "Strasse Mods" }
```

Ключи строятся как `<тип>.<namespace>.<path>`: `item.`, `block.`, `entity.`,
`itemGroup.`, `advancements.`, `death.attack.`.

Рецепт, `data/strassemods/recipe/strasse_block.json`:

```json
{
  "type": "minecraft:crafting_shaped",
  "category": "misc",
  "pattern": ["III", "III", "III"],
  "key": { "I": "strassemods:strasse_ingot" },
  "result": { "id": "strassemods:strasse_block", "count": 1 }
}
```

`pack.mcmeta` (если нужен):

```json
{ "pack": { "description": "Strasse Mods", "pack_format": 81 } }
```

`pack_format` зависит от версии игры и меняется почти каждый релиз — проверяйте на
<https://minecraft.wiki/w/Pack_format>. В моде его обычно указывать не требуется:
загрузчик подставляет формат сам.

## 3. Datagen (генерация ресурсов кодом)

Писать сотни JSON руками не нужно — оба загрузчика умеют генерировать их из кода.
Плюс: рецепты/теги/лут-таблицы валидируются компилятором и не «протухают» при
переименованиях.

### NeoForge

```groovy
runs { data { data() } }        // в 26.x: clientData() / serverData()
sourceSets.main.resources { srcDir 'src/generated/resources' }
```

```java
@EventBusSubscriber(modid = StrasseMods.MODID, bus = EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new ModRecipeProvider.Runner(out, event.getLookupProvider()));
        gen.addProvider(event.includeClient(), new ModItemModelProvider(out, event.getExistingFileHelper()));
    }
}
```

Запуск: `./gradlew runData` → результат в `src/generated/resources`, который уже
включён в ресурсы мода.

### Fabric

Точка входа `fabric-datagen` в `fabric.mod.json` + `fabricApi { configureDataGeneration() }`
в `build.gradle`:

```java
public class ModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModModelProvider::new);
    }
}
```

Запуск: `./gradlew runDatagen`.

## 4. Текстуры

* Размер — степень двойки, обычно 16×16 для предметов и блоков.
* PNG с альфа-каналом (RGBA8).
* Анимация — файл `.png.mcmeta` рядом с текстурой:

```json
{ "animation": { "frametime": 4 } }
```

## 5. Проверка

| Что | Как |
|---|---|
| JSON синтаксис | `python3 -m json.tool файл.json` или CI-шаг |
| Валидность датапака | `/datapack list` и логи сервера при запуске |
| Отсутствующие модели | в логах клиента `Unable to load model` |
| Локализация | `/reload` + переключение языка на клиенте |

В CI этого репозитория JSON-файлы шаблонов проверяются автоматически
(см. [публикация и CI](10-publishing-ci.md)).

## 6. Ссылки

* Форматы ресурсов: <https://minecraft.wiki/w/Resource_pack>
* Форматы датапака: <https://minecraft.wiki/w/Data_pack>
* Генератор JSON от Misode: <https://misode.github.io/>
* NeoForge datagen: <https://docs.neoforged.net/docs/resources/>
* Fabric datagen: <https://docs.fabricmc.net/develop/data-generation/setup>
