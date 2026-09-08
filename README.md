# MinecraftModsStrasse

Рабочий репозиторий по разработке модов **Minecraft Java Edition**: справочник по
Java/Fabric/Forge/NeoForge и десять готовых к сборке шаблонов модов — от 1.20.1 до 26.2.

Репозиторий не содержит ничего из GRM-проектов: это отдельная площадка только под Minecraft.

> Версии проверены **6 сентября 2026** по официальным maven-репозиториям и промо-эндпоинтам.

## Что внутри

```
docs/         справочник (10 разделов, на русском)
data/         versions.json — машиночитаемая матрица версий
templates/    10 шаблонов модов, каждый — самостоятельный Gradle-проект
tools/        sync-templates.sh, check-versions.sh, overlays/
.github/      CI: сборка всех шаблонов на нужных JDK
```

## Шаблоны

| Шаблон | MC | Java | Загрузчик | Основа |
|---|---|---|---|---|
| `templates/fabric-1.20.1`   | 1.20.1 | 17 | Fabric Loader 0.19.5 + API 0.92.12 | fabric-example-mod |
| `templates/fabric-1.21.1`   | 1.21.1 | 21 | Fabric + API 0.116.17 | fabric-example-mod **+ мод «Волшебная палочка»** |
| `templates/fabric-1.21.8`   | 1.21.8 | 21 | Fabric + API 0.136.1 | fabric-example-mod |
| `templates/fabric-1.21.11`  | 1.21.11 | 21 | Fabric + API 0.141.6 | fabric-example-mod |
| `templates/fabric-26.2`     | 26.2 | 25 | Fabric + API 0.159.0 | fabric-example-mod |
| `templates/forge-1.20.1`    | 1.20.1 | 17 | Forge 47.4.10 | MDK-Forge-ModDevGradle |
| `templates/neoforge-1.21.1` | 1.21.1 | 21 | NeoForge 21.1.250 | NeoForge MDK **+ мод «Волшебная палочка»** |
| `templates/neoforge-1.21.8` | 1.21.8 | 21 | NeoForge 21.8.54 | NeoForge MDK |
| `templates/neoforge-1.21.11`| 1.21.11 | 21 | NeoForge 21.11.45 | NeoForge MDK |
| `templates/neoforge-26.2`   | 26.2 | 25 | NeoForge 26.2.0.79 | NeoForge MDK |

Во всех шаблонах уже проставлены: mod id `strassemods`, пакет `by.strasse.strassemods`,
группа, автор, лицензия MIT.

### Волшебная палочка

Шаблоны **1.21.1** (Fabric и NeoForge) содержат не заготовку, а рабочий мод: предмет
«Волшебная палочка» с моделью ванильной палки и семью заклинаниями в духе Гарри Поттера —
Люмос, Нокс, Алохомора, Вингардиум Левиоса, Акцио, Депульсо, Инсендио.
ПКМ — применить, Shift + ПКМ — сменить заклинание. Алохомора вдобавок подаёт магический
сигнал редстоуна: находит рычаг, кнопку, поршень или раздатчик в радиусе трёх блоков от
прицела — даже за стеной — и переключает его. Логика заклинаний написана на чистом
ванильном API и общая для обоих загрузчиков: [docs/11-magic-wand.md](docs/11-magic-wand.md).

## Быстрый старт

```bash
cd templates/neoforge-1.21.1   # или любой другой шаблон
./gradlew runClient            # запустить клиент с модом
./gradlew build                # собрать jar -> build/libs
```

Нужна JDK нужной версии (17 / 21 / 25 — см. таблицу выше). Gradle ставить не нужно,
в каждом шаблоне есть wrapper.

## Документация

| # | Раздел |
|---|---|
| 01 | [Версии и совместимость](docs/01-versions-matrix.md) — матрица MC ↔ Java ↔ загрузчики, новая схема версий Mojang, конец обфускации |
| 02 | [Окружение разработки](docs/02-dev-environment.md) |
| 03 | [Fabric](docs/03-fabric.md) |
| 04 | [NeoForge](docs/04-neoforge.md) |
| 05 | [Forge](docs/05-forge.md) |
| 06 | [Мультилоадер](docs/06-multiloader.md) |
| 07 | [Порты версий 1.20 → 1.21 → 26.x](docs/07-porting.md) |
| 08 | [Mixins](docs/08-mixins.md) |
| 09 | [Ресурсы и datagen](docs/09-resources-datagen.md) |
| 10 | [Публикация и CI](docs/10-publishing-ci.md) |
| 11 | [Мод «Волшебная палочка»](docs/11-magic-wand.md) — рабочий мод: предмет-палочка с семью заклинаниями для Fabric и NeoForge |

## Ключевые факты на сентябрь 2026

* Актуальная версия игры — **26.2**; нумерация теперь `год.дроп.хотфикс`,
  `1.21.11` была последней в старой схеме.
* С **26.1** Minecraft поставляется деобфусцированным и требует **Java 25**;
  Yarn и Intermediary после 1.21.11 больше не обновляются — только Mojang mappings.
* **1.21.1** остаётся LTS-целью: на ней сидят крупнейшие модпаки (ATM10 и др.).
* **NeoForge** — основной «тяжёлый» загрузчик для 1.20.2+; **Forge** актуален
  прежде всего для 1.20.1 и легаси.
* Java по версиям: 1.18–1.20.4 → 17, 1.20.5–1.21.11 → 21, 26.x → 25.

## Обслуживание репозитория

`templates/` — **генерируемый** каталог: он пересобирается из официальных апстрим-шаблонов.

```bash
./tools/check-versions.sh                 # сверить версии с maven (нужен интернет)
./tools/sync-templates.sh                 # пересобрать все шаблоны
./tools/sync-templates.sh fabric-1.21.1   # пересобрать один
```

Собственный код кладите в `tools/overlays/<имя-шаблона>/`, а то, что должно попасть
сразу в несколько шаблонов, — в `tools/overlays/_shared/<имя>/` (карта раскладки —
переменная `SHARED_OVERLAYS` в скрипте). Всё это накладывается поверх
свежего апстрима при каждой синхронизации и потому не теряется. Версии загрузчиков,
которые нужно держать выше апстримовых, задаются в секции `VERSION PINS`
того же скрипта.

### Проверка сборки в CI

Готовых workflow два:

* [`ci/build-templates.yml`](ci/build-templates.yml) — матрица из 10 шаблонов, каждый
  собирается на своей JDK (17/21/25), плюс линт JSON и bash;
* [`ci/build-magic-wand.yml`](ci/build-magic-wand.yml) — сборка мода «Волшебная палочка»
  (Fabric и NeoForge 1.21.1) на JDK 21 с выкладкой готовых **jar в артефакты запуска**.

Файл намеренно лежит **не** в `.github/workflows/` — интеграция, которой сделан этот
коммит, не имеет права `workflows` и не может пушить файлы воркфлоу. Чтобы включить CI:

```bash
mkdir -p .github/workflows
cp ci/build-templates.yml ci/build-magic-wand.yml .github/workflows/
git add .github/workflows && git commit -m "ci: сборка шаблонов и jar палочки" && git push
```

После этого jar-файлы мода берутся так: вкладка **Actions** → запуск
«Волшебная палочка — сборка jar» → раздел **Artifacts** →
`magic-wand-fabric-1.21.1` и `magic-wand-neoforge-1.21.1`. Тот же результат
локально даёт `cd templates/fabric-1.21.1 && ./gradlew build` — jar появится
в `build/libs/`.

## Готовые jar без Gradle

Если ждать CI не хочется, собранные файлы уже лежат в [`dist/`](dist/):
`magic_wand_strasse-neoforge-1.21.1.jar` и `magic_wand_strasse-fabric-1.21.1.jar` —
оба кидаются в `mods/` и работают (Fabric-версии нужен ещё Fabric API).

Они собраны прямо в изолированной среде, без Gradle и maven: компилятор ECJ +
набор API-заглушек с точными сигнатурами 1.21.1, упаковка — питоновским
`zipfile`, а Fabric-версия ещё и переименована в intermediary (то, что обычно
делает Loom). Устройство, воспроизведение и ограничения описаны в
[`tools/offline-build/README.md`](tools/offline-build/README.md):

```bash
./tools/offline-build/fetch-toolchain.sh                       # JVM + ECJ
python3 tools/offline-build/build.py --all --out dist          # сборка jar
python3 tools/offline-build/verify.py dist/*.jar               # проверка байткода
```

> Шаблоны собраны из официальных апстрим-проектов и версии сверены с maven, но
> `./gradlew build` в изолированной среде не запускался. Jar в `dist/` проверен
> иначе: класс-файлы версии 65 загружаются и верифицируются JVM, а каждое обращение
> к API Minecraft/NeoForge/Fabric выписано в `tools/offline-build/api-usage.txt`
> и сверено с сигнатурами 1.21.1. Запуск игры остаётся шагом приёмки.

## Лицензия

Код репозитория — [MIT](LICENSE). Шаблоны наследуют лицензии апстрима
(fabric-example-mod — CC0-1.0, NeoForge MDK — см. `TEMPLATE_LICENSE.txt` внутри шаблона),
праймеры миграции NeoForged, цитируемые в `docs/07-porting.md`, — CC BY 4.0.
