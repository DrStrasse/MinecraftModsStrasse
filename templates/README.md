# Шаблоны модов

Каждый подкаталог — **самостоятельный Gradle-проект**. Открывайте в IDE именно его,
а не корень репозитория.

| Каталог | MC | Java | Загрузчик | Команды |
|---|---|---|---|---|
| `fabric-1.20.1`   | 1.20.1 | 17 | Fabric | `runClient`, `runServer`, `build` |
| `fabric-1.21.1`   | 1.21.1 | 21 | Fabric | + рабочий пример предмета `strassemods:strasse_ingot` |
| `fabric-1.21.8`   | 1.21.8 | 21 | Fabric | |
| `fabric-1.21.11`  | 1.21.11 | 21 | Fabric | последняя обфусцированная версия |
| `fabric-26.2`     | 26.2 | 25 | Fabric | без ремаппинга, плагин `net.fabricmc.fabric-loom` |
| `forge-1.20.1`    | 1.20.1 | 17 | Forge 47.4.10 | `runClient`, `runServer`, `runData` |
| `neoforge-1.21.1` | 1.21.1 | 21 | NeoForge 21.1.250 | + `runGameTestServer` |
| `neoforge-1.21.8` | 1.21.8 | 21 | NeoForge 21.8.54 | |
| `neoforge-1.21.11`| 1.21.11 | 21 | NeoForge 21.11.45 | |
| `neoforge-26.2`   | 26.2 | 25 | NeoForge 26.2.0.79 | `clientData`/`serverData` вместо `data` |

Общие параметры во всех шаблонах:

```properties
mod_id     = strassemods
group      = by.strasse.strassemods
mod_name   = Strasse Mods
mod_license= MIT
```

## Как начать свой мод

1. Скопируйте нужный шаблон:
   `cp -r templates/neoforge-1.21.1 ../my-mod && cd ../my-mod`
2. Поменяйте `mod_id`, `group`/`mod_group_id`, имя пакета и `mod_name`.
3. `./gradlew runClient` — проверьте, что всё стартует.
4. Дальше — [docs/04-neoforge.md](../docs/04-neoforge.md) или
   [docs/03-fabric.md](../docs/03-fabric.md).

## Важно: каталог генерируемый

Содержимое `templates/` пересоздаётся скриптом `tools/sync-templates.sh` из официальных
апстрим-репозиториев (fabric-example-mod, NeoForge MDK). Правки, сделанные напрямую
здесь, будут потеряны при следующей синхронизации.

Свой код кладите в `tools/overlays/<имя-шаблона>/` — он копируется поверх шаблона
после каждой синхронизации. Пример: `tools/overlays/fabric-1.21.1/` добавляет
класс `ModItems`, текстуру, модель и локализацию предмета.
