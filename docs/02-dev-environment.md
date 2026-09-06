# Окружение разработки

## 1. Что поставить

| Инструмент | Требование |
|---|---|
| JDK | по целевой версии игры: 17 / 21 / 25 (см. [матрицу](01-versions-matrix.md)) |
| IDE | IntelliJ IDEA (Community достаточно) ≥ 2025.2 для Java 25; либо Eclipse ≥ 2025-12 |
| Gradle | **не нужен глобально** — в каждом шаблоне лежит wrapper (`./gradlew`) |
| Git | любой свежий |

Проверка:

```bash
java -version         # должно совпасть с целевой версией
./gradlew --version   # wrapper скачает нужный Gradle сам
```

Несколько JDK одновременно — норма. NeoForge-шаблоны используют Gradle toolchains
(`java.toolchain.languageVersion`), и Gradle сам скачает нужную JDK через foojay-resolver,
подключённый в `settings.gradle`. Fabric-шаблоны требуют, чтобы **запускающая** JDK
была не ниже целевой.

## 2. Первый запуск шаблона

```bash
cd templates/fabric-1.21.1     # или любой другой шаблон
./gradlew build                # сборка jar в build/libs
./gradlew runClient            # запуск клиента с модом
./gradlew runServer            # запуск сервера
```

Для NeoForge/Forge дополнительно доступны:

```bash
./gradlew runData              # генерация ресурсов (datagen)
./gradlew runGameTestServer    # прогон gametest'ов
```

Первый запуск скачивает Minecraft, библиотеки и маппинги — это несколько минут
и ~1–2 ГБ трафика. Дальше всё берётся из кэша Gradle.

## 3. Импорт в IntelliJ IDEA

1. `File → Open` → выбрать **папку конкретного шаблона** (не корень репозитория!).
   В корне лежит несколько независимых Gradle-проектов, IDEA не должна пытаться
   импортировать их одним махом.
2. Дождаться Gradle sync.
3. Run-конфигурации `runClient` / `runServer` появятся автоматически
   (для Fabric — через Loom, для NeoForge — через ModDevGradle; при необходимости
   выполните `./gradlew idea` или пересинхронизируйте проект).

Полезные настройки IDEA:

* включить annotation processing (нужно для mixin refmap на версиях ≤ 1.21.11);
* «Build and run using: Gradle» — иначе моды могут не видеть сгенерированные ресурсы;
* file encoding — UTF-8 (в шаблонах в `.gitattributes` уже зафиксированы переводы строк).

## 4. Структура репозитория

```
MinecraftModsStrasse/
├── docs/            — документация (этот раздел)
├── data/            — versions.json: машиночитаемая матрица версий
├── templates/       — 10 готовых шаблонов модов (Fabric/NeoForge/Forge)
├── tools/
│   ├── sync-templates.sh   — пересборка templates/ из апстрим-репозиториев
│   ├── check-versions.sh   — проверка актуальности версий
│   └── overlays/           — наш собственный код поверх апстрим-шаблонов
└── .github/workflows/      — CI: собирает все шаблоны
```

Ключевая идея: **`templates/` — генерируемый каталог**. Свои наработки кладите в
`tools/overlays/<имя-шаблона>/` — при следующем `sync-templates.sh` они будут
наложены поверх свежего апстрима. Так шаблоны всегда остаются актуальными,
а наш код не теряется.

## 5. Кэши и место на диске

| Что | Где | Размер |
|---|---|---|
| Gradle | `~/.gradle` | 2–10 ГБ |
| Loom (Fabric) | `~/.gradle/caches/fabric-loom` | 1–3 ГБ |
| ModDevGradle | `~/.gradle/caches/modules-2` | 1–3 ГБ |
| Run-окружения | `templates/*/run*` | сотни МБ |

`run/`, `build/`, `.gradle/` уже в `.gitignore`.

## 6. Оффлайн и прокси

Сборка требует доступа к:

* `maven.fabricmc.net`, `maven.neoforged.net`, `maven.minecraftforge.net`,
* `repo1.maven.org`, `libraries.minecraft.net`, `piston-meta.mojang.com`,
* `services.gradle.org` (дистрибутив Gradle).

Если сеть ограничена — используйте GitHub Actions
([CI](10-publishing-ci.md)); там сборка проходит в чистом окружении с доступом
ко всем нужным репозиториям.
