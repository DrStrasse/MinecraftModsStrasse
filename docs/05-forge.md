# Forge (MinecraftForge)

Forge — исторический «тяжёлый» загрузчик. После отделения NeoForge на 1.20.2 роли
разделились так:

* **Forge** — легаси-версии (1.7.10, 1.12.2, 1.16.5, 1.18.2, 1.19.2, **1.20.1**) и
  всё, что зависит от старых модпаков;
* **NeoForge** — 1.20.2+ и все новые проекты.

При этом Forge не умер: по состоянию на сентябрь 2026 он публикует сборки вплоть
до 26.2 (`65.1.3`). Но экосистема модов на 1.21+ практически целиком ушла в NeoForge,
поэтому браться за Forge стоит только под конкретную задачу (обычно — 1.20.1).

Шаблон в репозитории: `templates/forge-1.20.1` (Java 17, Forge 1.20.1,
ModDevGradle legacyforge 2.0.91).

## 1. Совместимость мод-JAR'ов

| Версия | Forge ↔ NeoForge |
|---|---|
| 1.20.1 и ниже | NeoForge не существует, только Forge |
| 1.20.2+ | **несовместимы**: Forge-мод не загрузится на NeoForge-сервере и наоборот |

Мешать jar'ы двух загрузчиков в одной сборке нельзя — это гарантированный краш.
Моды, поддерживающие оба, публикуют отдельные файлы и явно указывают загрузчик
на CurseForge/Modrinth.

## 2. Отличия от NeoForge в коде (1.20.1)

| Что | Forge 1.20.1 | NeoForge 1.21.x |
|---|---|---|
| Манифест | `META-INF/mods.toml` | `META-INF/neoforge.mods.toml` |
| Пакеты | `net.minecraftforge.*` | `net.neoforged.neoforge.*`, `net.neoforged.fml.*` |
| Точка входа | `@Mod("modid")`, шина через `FMLJavaModLoadingContext.get().getModEventBus()` | `@Mod(MODID)`, `IEventBus` приходит параметром конструктора |
| Реестры | `DeferredRegister.create(ForgeRegistries.ITEMS, MODID)` | `DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID)` |
| Регистрация объекта | `RegistryObject<Item>` | `DeferredItem<Item>` / `DeferredHolder<>` |
| Игровая шина | `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| Конфиги | `ForgeConfigSpec` | `ModConfigSpec` |
| Данные предмета | NBT (`ItemStack#getTag`) | Data Components (с 1.20.5) |

Пример для 1.20.1:

```java
@Mod(StrasseMods.MODID)
public class StrasseMods {
    public static final String MODID = "strassemods";

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final RegistryObject<Item> STRASSE_INGOT =
            ITEMS.register("strasse_ingot", () -> new Item(new Item.Properties()));

    public StrasseMods() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }
}
```

> Наш шаблон `templates/forge-1.20.1` собран на ModDevGradle legacyforge и структурно
> повторяет NeoForge-MDK (`@Mod` с параметрами, современный синтаксис). Это официальный
> путь NeoForged для сборки Forge-модов современным тулингом; если нужен «канонический»
> Forge MDK 1.20.1, берите его с <https://files.minecraftforge.net/>.

## 3. Сборка (ModDevGradle legacyforge)

```groovy
plugins { id 'net.neoforged.moddev.legacyforge' version '2.0.91' }

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

legacyForge {
    version = "${minecraft_version}-${forge_version}"     // напр. 1.20.1-47.4.10
    parchment {
        minecraftVersion = project.parchment_minecraft_version
        mappingsVersion = project.parchment_mappings_version
    }
    runs { client { client() }; server { server() }; data { data() } }
}
```

Классическая альтернатива — ForgeGradle 6:

```groovy
plugins { id 'net.minecraftforge.gradle' version '[6.0,6.2)' }
minecraft { mappings channel: 'official', version: '1.20.1' }
dependencies { minecraft 'net.minecraftforge:forge:1.20.1-47.4.10' }
```

## 4. Версии Forge

| Minecraft | Forge latest / recommended |
|---|---|
| 1.12.2 | 14.23.5.2864 / 14.23.5.2859 |
| 1.16.5 | 36.2.42 / 36.2.34 |
| 1.18.2 | 40.3.12 / 40.3.0 |
| 1.19.2 | 43.5.2 / 43.5.0 |
| **1.20.1** | **47.4.23 / 47.4.10** |
| 1.21.1 | 52.1.16 / 52.1.0 |
| 1.21.8 | 58.1.22 / 58.1.0 |
| 26.2 | 65.1.3 / 65.1.0 |

Для продакшена берите **recommended**, для разработки — latest.
Источник: <https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json>.

## 5. Ссылки

* Файлы и MDK: <https://files.minecraftforge.net/>
* Документация: <https://docs.minecraftforge.net/>
* Forge-исходники: <https://github.com/MinecraftForge/MinecraftForge>
