# Мультилоадер: один мод под Fabric и NeoForge

Когда мод должен выйти и на Fabric, и на NeoForge, есть три рабочих подхода.

## 1. Сравнение подходов

| Подход | Суть | Плюсы | Минусы |
|---|---|---|---|
| **Отдельные репозитории/папки** | два независимых проекта, код копируется | проще всего начать, никакой магии | дублирование, расхождение логики |
| **Multi-project Gradle** (`common` + `fabric` + `neoforge`) | общий модуль компилируется против ванили, платформенные — против загрузчиков | нет дублирования, полный контроль | нужно самому писать абстракции (Service Loader) |
| **Architectury** | тот же multi-project + готовый API-слой и `@ExpectPlatform` | много готовых абстракций (реестры, события, сеть) | зависимость от стороннего API и его сроков обновления |

Для небольшого мода (предметы, блоки, рецепты) разница между вторым и третьим
подходом невелика; для крупного — Architectury экономит недели.

## 2. Структура multi-project

```
strassemods/
├── settings.gradle          include 'common', 'fabric', 'neoforge'
├── gradle.properties        версии всех платформ в одном месте
├── common/                  ванильный код, без API загрузчиков
│   └── src/main/java/by/strasse/strassemods/
├── fabric/                  ModInitializer + fabric.mod.json
└── neoforge/                @Mod + neoforge.mods.toml
```

`settings.gradle`:

```groovy
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        maven { url = 'https://maven.neoforged.net/releases' }
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = 'strassemods'
include 'common', 'fabric', 'neoforge'
```

Платформенные подпроекты подключают общий код исходниками (это надёжнее, чем
зависимость на jar, из-за разных маппингов):

```groovy
// fabric/build.gradle и neoforge/build.gradle
sourceSets.main.java.srcDir project(':common').file('src/main/java')
sourceSets.main.resources.srcDir project(':common').file('src/main/resources')
```

`common` собирается против «голой» ванили: для Fabric-стороны это Loom без Fabric API,
для NeoForge — ModDevGradle в режиме `neoForge { neoFormVersion = ... }` (vanilla-only).

## 3. Абстрагирование платформы

Единственное, что реально различается, — это регистрация, события и конфиг.
Классический приём — Java `ServiceLoader`:

```java
// common
public interface IPlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
    Path configDir();
}

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Не найдена реализация " + clazz));
    }
}
```

Реализации кладутся в `fabric/src/main/java/.../FabricPlatformHelper.java` и
`neoforge/.../NeoForgePlatformHelper.java`, а в
`src/main/resources/META-INF/services/by.strasse.strassemods.IPlatformHelper`
пишется полное имя класса-реализации.

Architectury-вариант того же самого — аннотация `@ExpectPlatform`:

```java
// common
public class Platform {
    @ExpectPlatform
    public static Path getConfigDirectory() { throw new AssertionError(); }
}
// fabric: PlatformImpl.getConfigDirectory() — имя класса + "Impl", ищется автоматически
```

## 4. Реестры в мультилоадере

Самая частая абстракция — обёртка над реестром:

```java
// common
public interface RegistrySupplier<T> extends Supplier<T> { ResourceLocation id(); }

public interface ModRegistry {
    <T extends Item> RegistrySupplier<T> item(String name, Supplier<T> factory);
    void finish();
}
```

Fabric-реализация вызывает `Registry.register(...)` немедленно,
NeoForge-реализация складывает в `DeferredRegister` и регистрирует его на mod bus.

## 5. Что можно (и нужно) держать в common

| В common | Только в платформенных модулях |
|---|---|
| Логика блоков/предметов, `BlockBehaviour`, `Item` | регистрация в реестрах |
| Данные, рецепты, теги, лут-таблицы | точки входа (`ModInitializer`, `@Mod`) |
| Математика, алгоритмы, генерация | сеть (payload-API различается) |
| Компоненты данных, `Codec`'и | конфиги (`ModConfigSpec` vs своя реализация) |
| Клиентский рендер на ванильных API | привязка рендереров к событиям загрузчика |

## 6. Когда мультилоадер не нужен

* мод глубоко завязан на API одного загрузчика (например, NeoForge Capabilities);
* мод состоит в основном из миксинов — они и так пишутся отдельно под каждую платформу
  (разные точки инъекции для разных мод-окружений);
* цель — одна версия и один загрузчик под конкретный модпак.

В этом случае честнее взять готовый шаблон из `templates/` и не усложнять сборку.

## 7. Ссылки

* Architectury Loom и шаблоны: <https://docs.architectury.dev/>
* Пример «ручного» мультилоадера: <https://github.com/jaredlll08/MultiLoader-Template>
* Наши одноплатформенные шаблоны: [`templates/`](../templates)
