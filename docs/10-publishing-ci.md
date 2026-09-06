# Публикация мода и CI

## 1. Что публикуется

| Артефакт | Где взять |
|---|---|
| `strassemods-1.0.0.jar` | `build/libs/` после `./gradlew build` |
| `*-sources.jar` | Fabric-шаблоны собирают автоматически (`withSourcesJar()`) |

Проверьте перед публикацией:

* в jar лежит `fabric.mod.json` / `META-INF/neoforge.mods.toml` с правильными версиями;
* нет `-dev` или `-all` jar'а (Fabric кладёт в `build/libs` и промежуточные файлы);
* мод запускается на **выделенном сервере**, а не только в клиенте;
* версия в `gradle.properties` поднята.

Схема версии — semver + версия игры: `1.2.0+1.21.1`.

## 2. Площадки

| Площадка | Особенности |
|---|---|
| **Modrinth** | открытый API, удобная автопубликация, обязательна лицензия |
| **CurseForge** | больше аудитория и модпаки, нужен API-ключ и project id |
| **GitHub Releases** | простейший вариант, не даёт видимости в лаунчерах |

Автопубликация из Gradle — плагин
[`me.modmuss50.mod-publish-plugin`](https://github.com/modmuss50/mod-publish-plugin):

```groovy
plugins { id 'me.modmuss50.mod-publish-plugin' version '0.8.4' }

publishMods {
    file = remapJar.archiveFile          // для NeoForge: jar.archiveFile
    changelog = file("CHANGELOG.md").text
    type = STABLE
    modLoaders.add("fabric")

    modrinth {
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        projectId = "xxxxxxxx"
        minecraftVersions.add("1.21.1")
    }
    curseforge {
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        projectId = "123456"
        minecraftVersions.add("1.21.1")
    }
}
```

Токены хранятся **только** в GitHub Secrets, никогда в репозитории.

## 3. CI в этом репозитории

`ci/build-templates.yml` (скопируйте его в `.github/workflows/`, чтобы GitHub его увидел)
делает две вещи:

1. **lint** — валидирует все JSON-файлы и синтаксис bash-скриптов;
2. **build** — матрицей собирает все 10 шаблонов на нужной для каждого JDK
   (17 / 21 / 25) и выкладывает jar'ы как артефакты сборки.

```yaml
strategy:
  matrix:
    include:
      - { template: fabric-1.21.1,   java: 21 }
      - { template: neoforge-26.2,   java: 25 }
      # ...
steps:
  - uses: actions/setup-java@v4
    with: { distribution: temurin, java-version: '${{ matrix.java }}' }
  - uses: gradle/actions/setup-gradle@v4
  - run: ./gradlew build --stacktrace --no-daemon
    working-directory: templates/${{ matrix.template }}
```

Зачем: локально сборка требует доступа к maven-репозиториям Mojang/Fabric/NeoForge
и нескольких гигабайт кэша. CI проверяет, что после обновления версий
(`tools/sync-templates.sh`) все шаблоны действительно собираются.

Включение:

```bash
mkdir -p .github/workflows && cp ci/build-templates.yml .github/workflows/
git add .github/workflows/build-templates.yml && git commit -m "ci: сборка шаблонов" && git push
```

Запуск вручную после этого: вкладка **Actions → Build templates → Run workflow**,
или `gh workflow run build-templates.yml`.

## 4. Релизный процесс для собственного мода

```bash
# 1. поднять версию
sed -i 's/^version=.*/version=1.1.0/' gradle.properties      # Fabric
sed -i 's/^mod_version=.*/mod_version=1.1.0/' gradle.properties  # NeoForge

# 2. собрать и проверить
./gradlew clean build
./gradlew runServer     # smoke-тест на сервере

# 3. тег и релиз
git tag v1.1.0+1.21.1 && git push origin v1.1.0+1.21.1
gh release create v1.1.0+1.21.1 build/libs/*.jar -t "1.1.0 для 1.21.1" -F CHANGELOG.md
```

Отдельный workflow на теги может делать это автоматически:

```yaml
on:
  push:
    tags: ["v*"]
```

## 5. Лицензия и метаданные

* Лицензия обязательна на Modrinth и желательна везде. В шаблонах указан `MIT`
  (`mod_license` в `gradle.properties`, `license` в `fabric.mod.json`).
* Заполните `authors`, `contact.homepage`, `contact.sources`, `issueTrackerURL` —
  без них мод выглядит заброшенным.
* Иконка: `assets/<modid>/icon.png` (Fabric), `logoFile` (NeoForge), 128×128 или больше.
