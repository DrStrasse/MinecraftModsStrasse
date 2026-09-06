# Fabric

Fabric — лёгкий загрузчик: минимум собственного API, всё расширяется через
Mixin и точечные хуки Fabric API. Быстрее всех обновляется на новые версии игры.

Шаблоны в репозитории: `templates/fabric-1.20.1`, `fabric-1.21.1`, `fabric-1.21.8`,
`fabric-1.21.11`, `fabric-26.2`.

## 1. Из чего состоит Fabric-мод

| Компонент | Назначение |
|---|---|
| **Fabric Loader** | загружает моды, применяет миксины (версия 0.19.5) |
| **Fabric API** | набор хуков и событий поверх ванили (реестры, события, сеть, рендер) |
| **Fabric Loom** | Gradle-плагин: скачивает игру, маппит, запускает, ремапит jar |
| `fabric.mod.json` | манифест мода: id, entrypoints, зависимости, миксин-конфиги |

## 2. Манифест `fabric.mod.json`

```json
{
  "schemaVersion": 1,
  "id": "strassemods",
  "version": "${version}",
  "name": "Strasse Mods",
  "environment": "*",
  "entrypoints": {
    "main":   ["by.strasse.strassemods.StrasseMods"],
    "client": ["by.strasse.strassemods.client.StrasseModsClient"]
  },
  "mixins": [
    "strassemods.mixins.json",
    { "config": "strassemods.client.mixins.json", "environment": "client" }
  ],
  "depends": {
    "fabricloader": ">=0.19.5",
    "minecraft": "~1.21.1",
    "java": ">=21",
    "fabric-api": "*"
  }
}
```

* `environment`: `"*"` (обе стороны), `"client"`, `"server"`.
* `${version}` подставляется в `processResources` из `gradle.properties`.
* Точки входа бывают не только `main`/`client`: `server`, `preLaunch`,
  `fabric-datagen`, а также кастомные, объявленные другими модами.

## 3. Split source sets

Официальный шаблон включает `loom { splitEnvironmentSourceSets() }` — код делится на
`src/main` (обе стороны) и `src/client` (только клиент). Это ловит на этапе компиляции
классическую ошибку «дёрнул `Minecraft.getInstance()` на сервере», которая иначе
падает `NoClassDefFoundError` на выделенном сервере.

## 4. Регистрация контента

Пример из `templates/fabric-1.21.1` (наш overlay, Mojang mappings):

```java
public final class ModItems {
    public static final Item STRASSE_INGOT =
            register("strasse_ingot", new Item(new Item.Properties()));

    private static Item register(String path, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, StrasseMods.id(path), item);
    }

    public static void init() { }
}
```

```java
@Override
public void onInitialize() {
    ModItems.init();
    ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
            .register(entries -> entries.accept(ModItems.STRASSE_INGOT));
}
```

Важно: в Fabric регистрация выполняется **сразу** (никаких `DeferredRegister`, как в
NeoForge), поэтому классы реестров должны быть загружены из `onInitialize`.
Начиная с 1.21.2 конструктор `Item` требует id внутри `Item.Properties`
(`.setId(ResourceKey)`), см. [порты версий](07-porting.md).

Ресурсы предмета:

```
src/main/resources/assets/strassemods/
├── lang/en_us.json, ru_ru.json
├── models/item/strasse_ingot.json
└── textures/item/strasse_ingot.png
```

С 1.21.4 к этому добавляется «client item» — `assets/<ns>/items/<path>.json`.

## 5. Что даёт Fabric API

| Модуль | Для чего |
|---|---|
| `fabric-lifecycle-events` | старт/стоп сервера, тики мира и клиента |
| `fabric-item-group-api` | добавление предметов в творческие вкладки |
| `fabric-networking-api` | кастомные пакеты (payload-типы) |
| `fabric-registry-sync` | синхронизация реестров клиент↔сервер |
| `fabric-resource-loader` | доступ к ресурсам мода, встроенные датапаки |
| `fabric-transfer-api` | универсальный перенос жидкостей/предметов |
| `fabric-renderer-api`, `fabric-rendering` | кастомный рендер блоков и моделей |

Подключается целиком одной зависимостью `fabric-api`; при желании можно
подключить отдельные модули через `fabricApi.module(...)`.

## 6. Loom: ключевые различия по версиям

```groovy
// ≤ 1.21.11 (обфусцированная игра)
plugins { id 'net.fabricmc.fabric-loom-remap' version "${loom_version}" }
dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()          // или net.fabricmc:yarn:...
    modImplementation "net.fabricmc:fabric-loader:${loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
}

// 26.1+ (деобфусцированная игра — ремаппинг не нужен)
plugins { id 'net.fabricmc.fabric-loom' version "${loom_version}" }
dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"
}
```

Обратите внимание: в 26.x исчезают и `mappings`, и префикс `mod` у зависимостей.
Это ровно то, что видно в диффе между `templates/fabric-1.21.11` и `templates/fabric-26.2`.

## 7. Полезные команды

```bash
./gradlew runClient           # клиент
./gradlew runServer           # сервер
./gradlew runDatagen          # если настроен fabric-datagen entrypoint
./gradlew build               # jar + sources jar в build/libs
./gradlew migrateMappings --mappings "..."   # перевод кода на другие маппинги
```

## 8. Ссылки

* Документация: <https://docs.fabricmc.net/>
* Версии и генератор шаблонов: <https://fabricmc.net/develop/>
* Исходники Fabric API: <https://github.com/FabricMC/fabric>
* Пример мода (апстрим наших шаблонов): <https://github.com/FabricMC/fabric-example-mod>
