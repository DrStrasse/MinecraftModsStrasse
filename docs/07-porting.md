# Порты между версиями: 1.20 → 1.21 → 26.x

Сводка ломающих изменений, из-за которых мод не собирается или падает при переносе.
Первоисточник — праймеры NeoForged (CC BY 4.0):
<https://github.com/neoforged/.github/tree/main/primers>.

## Быстрая карта «где больно»

| Переход | Главная боль |
|---|---|
| 1.20.1 → 1.20.2 | разделение Forge/NeoForge, сетевой стек |
| 1.20.4 → 1.20.5/6 | **Data Components вместо NBT**, Java 21 |
| 1.20.6 → 1.21 | зачарования стали data-driven |
| 1.21.1 → 1.21.2 | **HolderSet везде**, id предмета в `Item.Properties`, EntityRenderState |
| 1.21.3 → 1.21.4 | **Client Items** — модели предметов в `assets/<ns>/items/` |
| 1.21.4 → 1.21.5 | рендер-пайплайн (RenderPipeline/GpuBuffer), удаление BlockEntity |
| 1.21.5 → 1.21.6 | **GUI: prepare/render через GuiRenderState** |
| 1.21.8 → 1.21.9 | overhaul отладочной системы, рендер |
| 1.21.10 → 1.21.11 | **массовые переименования**: `ResourceLocation` → `Identifier` |
| 1.21.11 → 26.1 | **Java 25 + деобфускация**, конец Yarn/Intermediary |
| 26.1 → 26.2 | Vulkan, `GpuFormat`, `BlendFactor` — Blaze3d переписан |

---

## 1.20.5/1.20.6 — Data Components

NBT у `ItemStack` заменён на типизированные компоненты:

```java
// было (1.20.4)
stack.getOrCreateTag().putInt("charge", 5);

// стало (1.20.5+)
stack.set(DataComponents.DAMAGE, 5);
stack.set(MY_CHARGE_COMPONENT, new ChargeData(5));
```

Свои компоненты регистрируются в `Registries.DATA_COMPONENT_TYPE` и требуют `Codec`
и (для сети) `StreamCodec`. Одновременно поднялась планка Java до 21.

## 1.21 — зачарования как данные

`Enchantment` перестал быть кодовым объектом: теперь это записи датапака в
`data/<ns>/enchantment/*.json`, а в коде используются `Holder<Enchantment>` и
`ResourceKey<Enchantment>`. Прямые ссылки `Enchantments.SHARPNESS` меняются на
поиск через `HolderLookup`.

## 1.21.2 — HolderSet и id предметов

Методы, принимавшие `TagKey` или сырые объекты, теперь принимают `HolderSet`:

```json5
{ "holder_set": "minecraft:apple" }          // прямой набор из одного элемента
{ "holder_set": ["minecraft:apple", "minecraft:stick"] }
{ "holder_set": "#minecraft:planks" }        // ссылка на тег
```

В коде `HolderSet` получают через `Registry` (статические реестры) или `HolderGetter`
(`BootstrapContext#lookup`, `HolderLookup$Provider#lookupOrThrow`,
`MinecraftServer#registryAccess`).

Второе важное: у предмета появился обязательный id в свойствах —

```java
// 1.21.1
new Item(new Item.Properties())
// 1.21.2+
new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id("strasse_ingot"))))
```

В NeoForge `ITEMS.registerSimpleItem("name", props)` проставляет id сам —
это ещё один аргумент в пользу `DeferredRegister`.

Плюс рендер сущностей переехал на **EntityRenderState**: рендерер больше не читает
сущность напрямую, а получает снимок состояния.

## 1.21.4 — Client Items

Модель предмета больше не выбирается по `models/item/<name>.json`. Появился отдельный
слой описания: `assets/<namespace>/items/<path>.json`.

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "strassemods:item/strasse_ingot"
  }
}
```

Поддерживаются типы `minecraft:model`, `minecraft:range_dispatch`, `minecraft:select`,
`minecraft:condition`, `minecraft:special`. `overrides` в моделях удалены,
`ItemProperties` — тоже; `CustomModelData` стал списком float/flag/string/color.

## 1.21.5 — рендер и удаление блок-сущностей

* Логика удаления `BlockEntity` разделена: `BlockEntity#preRemoveSideEffects`
  (выбросить содержимое) и `BlockBehaviour#affectNeighborsAfterRemoval` (уведомить соседей)
  вместо старого `onRemove`.
* Рендер переведён на `RenderPipeline`/`GpuBuffer` — прямые вызовы `RenderSystem`
  в основном заменены.

## 1.21.6 — GUI: две фазы

Отрисовка интерфейса разделена на **prepare** и **render**: методы вроде
`Gui#render` теперь не рисуют, а складывают элементы в `GuiRenderState`;
собственно рисованием занимается `GuiRenderer`. Порядок элементов задаётся не
последовательностью вызовов, а стратами и сортировкой.

## 1.21.11 — «переименовательная» версия

* `ResourceLocation` → **`Identifier`** (везде: типы, имена методов, параметры);
  `ResourceLocationException` → `IdentifierException`.
* Утилиты переехали в `net.minecraft.util` (`Util`, `BlockUtil`, `FileUtil`).
* `net.minecraft.advancements.critereon` → `...criterion`.
* `net.minecraft.client.model` и `net.minecraft.world.entity` разбиты на подпакеты.

Механическая, но объёмная правка импортов — удобно делать «Replace in files» + реимпорт.

## 26.1 — Java 25 и деобфускация

* JDK 21 → **25**. Ваниль использует новые языковые возможности (JEP 447).
* Игра больше не обфусцирована: официальные имена «из коробки».
  Yarn и Intermediary **не обновляются после 1.21.11**.
* Для Fabric меняется build-скрипт: плагин `net.fabricmc.fabric-loom` вместо
  `net.fabricmc.fabric-loom-remap`, исчезает блок `mappings`, зависимости
  подключаются обычным `implementation`.
* Лут-таблицы: типы записей, функций и условий «развёрнуты» (loot type unrolling).
* Требуется свежая IDE: IDEA ≥ 2025.2, Eclipse ≥ 2025-12.

## 26.2 — Blaze3d и Vulkan

* Добавлен Vulkan-бэкенд: для классов `com.mojang.blaze3d.opengl.*` появились
  параллели в `com.mojang.blaze3d.vulkan.*`, общий слой стал абстрактнее.
* `DestFactor`/`SourceFactor` → единый `BlendFactor` (+ `BlendOp`, `BlendEquation`).
* `TextureFormat` и форматы `VertexFormatElement` → единый enum `GpuFormat`
  (`R8_UNORM`, `RGBA32_SINT`, `D32_FLOAT_S8_UINT`, …).

---

## Практика порта: чек-лист

1. Поднять `minecraft_version`, `fabric_api_version` / `neo_version`, `parchment_*`
   в `gradle.properties` (актуальные значения — [матрица](01-versions-matrix.md)).
2. Проверить требуемую **Java** и поднять `release`/`toolchain`.
3. Собрать (`./gradlew build`) и разгребать ошибки компиляции по праймеру нужного перехода.
4. Отдельно проверить ресурсы: модели предметов (1.21.4+), лут-таблицы (26.1+),
   форматы датапаков (`pack_format`).
5. Прогнать `runClient` и `runServer` — часть проблем (миксины, реестры)
   вылезает только в рантайме.
6. Миксины переносить в последнюю очередь: сигнатуры целевых методов меняются
   чаще всего, а ошибка проявляется падением на старте.

Порт через несколько версий делайте **пошагово** (1.21.1 → 1.21.4 → 1.21.8 → 1.21.11 → 26.x),
а не одним прыжком: так на каждом шаге понятно, что именно сломалось.
