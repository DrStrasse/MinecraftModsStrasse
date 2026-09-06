# Документация

Материалы по разработке модов Minecraft Java Edition: версии, загрузчики,
инструменты, порты между версиями. Данные проверены **6 сентября 2026**.

| # | Раздел | О чём |
|---|---|---|
| 01 | [Версии и совместимость](01-versions-matrix.md) | матрица Minecraft ↔ Java ↔ Fabric/NeoForge/Forge, новая схема версий Mojang, деобфускация |
| 02 | [Окружение разработки](02-dev-environment.md) | JDK, IDE, Gradle wrapper, структура репозитория, кэши |
| 03 | [Fabric](03-fabric.md) | Loader, Loom, `fabric.mod.json`, split source sets, регистрация контента |
| 04 | [NeoForge](04-neoforge.md) | `@Mod`, две шины событий, `DeferredRegister`, ModDevGradle, конфиги |
| 05 | [Forge](05-forge.md) | когда он ещё нужен, отличия от NeoForge, версии |
| 06 | [Мультилоадер](06-multiloader.md) | common + fabric + neoforge, ServiceLoader, Architectury |
| 07 | [Порты версий](07-porting.md) | ломающие изменения 1.20 → 1.21 → 26.x и чек-лист порта |
| 08 | [Mixins](08-mixins.md) | инъекции, MixinExtras, refmap, отладка |
| 09 | [Ресурсы и datagen](09-resources-datagen.md) | assets/data, форматы, генерация ресурсов кодом |
| 10 | [Публикация и CI](10-publishing-ci.md) | сборка релиза, Modrinth/CurseForge, GitHub Actions |
| 11 | [Мод «Волшебная палочка»](11-magic-wand.md) | рабочий мод: предмет-палочка, семь заклинаний, магический сигнал редстоуна, общий код для Fabric и NeoForge |

## С чего начать

1. Определитесь с версией и загрузчиком — [раздел 01](01-versions-matrix.md),
   таблица «Что выбирать под задачу».
2. Поставьте нужную JDK — [раздел 02](02-dev-environment.md).
3. Возьмите шаблон из [`templates/`](../templates) и запустите `./gradlew runClient`.
4. Дальше — [Fabric](03-fabric.md) или [NeoForge](04-neoforge.md) по выбору.
5. Живой пример готового мода — [раздел 11](11-magic-wand.md), «Волшебная палочка».

## Первоисточники

* Fabric: <https://docs.fabricmc.net/>, <https://fabricmc.net/develop/>
* NeoForge: <https://docs.neoforged.net/>, праймеры миграции —
  <https://github.com/neoforged/.github/tree/main/primers>
* Forge: <https://docs.minecraftforge.net/>, <https://files.minecraftforge.net/>
* Форматы ресурсов и датапаков: <https://minecraft.wiki/>
