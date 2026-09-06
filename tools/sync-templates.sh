#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# sync-templates.sh — пересобирает содержимое templates/ из официальных
# апстрим-шаблонов и применяет брендирование Strasse Mods.
#
# Апстримы:
#   Fabric   : https://github.com/FabricMC/fabric-example-mod (ветки по версиям)
#   NeoForge : https://github.com/NeoForgeMDKs/MDK-<ver>-ModDevGradle
#   Forge    : https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle
#
# Использование:  ./tools/sync-templates.sh [каталог-шаблона ...]
# Без аргументов — пересобираются все шаблоны.
#
# ВАЖНО: скрипт перезаписывает содержимое templates/<name>/, поэтому свои
# правки в шаблонах делайте только после синка (или коммитьте до запуска).
# ---------------------------------------------------------------------------
set -euo pipefail

MOD_ID="strassemods"
MOD_NAME="Strasse Mods"
MOD_GROUP="by.strasse.strassemods"
MOD_PKG_PATH="by/strasse/strassemods"
MOD_AUTHORS="DrStrasse"
MOD_LICENSE="MIT"

# ---------------------------------------------------------------------------
# VERSION PINS — версии загрузчиков, которые ставятся поверх апстрим-шаблонов.
# Апстрим-MDK нередко отстаёт от maven на несколько сборок; здесь фиксируем
# актуальные (проверено 2026-09-06, см. data/versions.json).
# Формат: <каталог шаблона>|<ключ в gradle.properties>|<значение>
# ---------------------------------------------------------------------------
PINS="
neoforge-1.21.1|neo_version|21.1.250
neoforge-26.2|neo_version|26.2.0.79
forge-1.20.1|forge_version|47.4.10
"

apply_pins() {
  local name="$1" props="$ROOT/templates/$1/gradle.properties" line dir key value
  while IFS='|' read -r dir key value; do
    [ -z "${dir:-}" ] && continue
    [ "$dir" = "$name" ] || continue
    if grep -q "^$key=" "$props"; then
      sed -i "s|^$key=.*|$key=$value|" "$props"
      log "  pin: $key=$value"
    fi
  done <<< "$PINS"
}


ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

FABRIC_TEMPLATES="1.20.1 1.21.1 1.21.8 1.21.11 26.2"
NEOFORGE_TEMPLATES="1.21.1 1.21.8 1.21.11 26.2"

log() { printf '\033[36m[sync]\033[0m %s\n' "$*"; }

# Накладывает tools/overlays/<template>/ поверх апстрим-шаблона (наш собственный код).
apply_overlay() {
  local name="$1"
  local ov="$ROOT/tools/overlays/$name"
  [ -d "$ov" ] || return 0
  log "  overlay: tools/overlays/$name"
  cp -r "$ov/." "$ROOT/templates/$name/"
}

clone_fabric() {
  local branch="$1" dest="$2"
  [ -d "$WORK/fabric" ] || git clone --quiet https://github.com/FabricMC/fabric-example-mod.git "$WORK/fabric"
  rm -rf "$dest"; mkdir -p "$dest"
  git -C "$WORK/fabric" archive "origin/$branch" | tar -x -C "$dest"
}

clone_mdk() {
  local repo="$1" dest="$2"
  rm -rf "$dest"; mkdir -p "$dest"
  git clone --quiet --depth 1 "https://github.com/NeoForgeMDKs/$repo.git" "$WORK/$repo"
  (cd "$WORK/$repo" && git archive HEAD) | tar -x -C "$dest"
  rm -rf "$WORK/$repo"
}

brand_fabric() {
  local dir="$1"
  # пакеты com.example.* -> by.strasse.strassemods.*
  mkdir -p "$dir/src/main/java/$MOD_PKG_PATH" "$dir/src/client/java/$MOD_PKG_PATH"
  cp -r "$dir/src/main/java/com/example/." "$dir/src/main/java/$MOD_PKG_PATH/"
  cp -r "$dir/src/client/java/com/example/." "$dir/src/client/java/$MOD_PKG_PATH/"
  rm -rf "$dir/src/main/java/com" "$dir/src/client/java/com"
  # классы Example* -> StrasseMods*
  mv "$dir/src/main/java/$MOD_PKG_PATH/ExampleMod.java" "$dir/src/main/java/$MOD_PKG_PATH/StrasseMods.java"
  mv "$dir/src/main/java/$MOD_PKG_PATH/mixin/ExampleMixin.java" "$dir/src/main/java/$MOD_PKG_PATH/mixin/StrasseModsMixin.java"
  mv "$dir/src/client/java/$MOD_PKG_PATH/client/ExampleModClient.java" "$dir/src/client/java/$MOD_PKG_PATH/client/StrasseModsClient.java"
  mv "$dir/src/client/java/$MOD_PKG_PATH/client/mixin/ExampleClientMixin.java" "$dir/src/client/java/$MOD_PKG_PATH/client/mixin/StrasseModsClientMixin.java"
  # ресурсы modid.* -> strassemods.*
  mv "$dir/src/main/resources/modid.mixins.json" "$dir/src/main/resources/$MOD_ID.mixins.json"
  mv "$dir/src/client/resources/modid.client.mixins.json" "$dir/src/client/resources/$MOD_ID.client.mixins.json"
  mv "$dir/src/main/resources/assets/modid" "$dir/src/main/resources/assets/$MOD_ID"
  # текстовые замены
  find "$dir" -type f \( -name '*.java' -o -name '*.json' -o -name '*.gradle' -o -name '*.properties' \) -print0 |
    xargs -0 sed -i \
      -e "s/com\.example/$MOD_GROUP/g" \
      -e "s/\bExampleClientMixin\b/StrasseModsClientMixin/g" \
      -e "s/\bExampleMixin\b/StrasseModsMixin/g" \
      -e "s/\bExampleModClient\b/StrasseModsClient/g" \
      -e "s/\bExampleMod\b/StrasseMods/g" \
      -e "s/\bmodid\b/$MOD_ID/g" \
      -e "s/\"Example Mod\"/\"$MOD_NAME\"/g"
  sed -i -e "s/^group=.*/group=$MOD_GROUP/" "$dir/gradle.properties"
  python3 - "$dir/src/main/resources/fabric.mod.json" <<'PY'
import json, sys, collections
p = sys.argv[1]
with open(p) as f:
    data = json.load(f, object_pairs_hook=collections.OrderedDict)
data["name"] = "Strasse Mods"
data["description"] = "Strasse Mods — базовый мод-шаблон для Fabric."
data["authors"] = ["DrStrasse"]
data["contact"] = {"homepage": "https://github.com/DrStrasse/MinecraftModsStrasse",
                   "sources": "https://github.com/DrStrasse/MinecraftModsStrasse"}
data["license"] = "MIT"
with open(p, "w") as f:
    json.dump(data, f, indent="\t", ensure_ascii=False)
    f.write("\n")
PY
  rm -rf "$dir/.github"
}

brand_mdk() {
  local dir="$1"
  local src="$dir/src/main/java"
  mkdir -p "$src/$MOD_PKG_PATH"
  cp -r "$src/com/example/examplemod/." "$src/$MOD_PKG_PATH/"
  rm -rf "$src/com"
  [ -f "$src/$MOD_PKG_PATH/ExampleMod.java" ] && mv "$src/$MOD_PKG_PATH/ExampleMod.java" "$src/$MOD_PKG_PATH/StrasseMods.java"
  [ -f "$src/$MOD_PKG_PATH/ExampleModClient.java" ] && mv "$src/$MOD_PKG_PATH/ExampleModClient.java" "$src/$MOD_PKG_PATH/StrasseModsClient.java"
  [ -d "$dir/src/main/resources/assets/examplemod" ] &&
    mv "$dir/src/main/resources/assets/examplemod" "$dir/src/main/resources/assets/$MOD_ID"
  find "$dir" -type f \( -name '*.java' -o -name '*.json' -o -name '*.toml' -o -name '*.gradle' -o -name '*.properties' -o -name '*.cfg' \) -print0 |
    xargs -0 sed -i \
      -e "s/com\.example\.examplemod/$MOD_GROUP/g" \
      -e "s/\bExampleModClient\b/StrasseModsClient/g" \
      -e "s/\bExampleMod\b/StrasseMods/g" \
      -e "s/\bexamplemod\b/$MOD_ID/g"
  sed -i \
    -e "s/^mod_id=.*/mod_id=$MOD_ID/" \
    -e "s/^mod_name=.*/mod_name=$MOD_NAME/" \
    -e "s/^mod_license=.*/mod_license=$MOD_LICENSE/" \
    -e "s/^mod_group_id=.*/mod_group_id=$MOD_GROUP/" \
    -e "s/^mod_authors=.*/mod_authors=$MOD_AUTHORS/" \
    "$dir/gradle.properties"
  grep -q '^mod_authors=' "$dir/gradle.properties" || echo "mod_authors=$MOD_AUTHORS" >> "$dir/gradle.properties"
  rm -rf "$dir/.github"
}

want() {
  local needle="$1"; shift
  [ "$#" -eq 0 ] && return 0   # без аргументов — собираем все шаблоны
  for a in "$@"; do [ "$a" = "$needle" ] && return 0; done
  return 1
}

SELECT=("$@")

for v in $FABRIC_TEMPLATES; do
  name="fabric-$v"
  want "$name" "${SELECT[@]+"${SELECT[@]}"}" || continue
  log "fabric-example-mod@$v -> templates/$name"
  branch="$v"   # ветки апстрима называются по версии Minecraft
  clone_fabric "$branch" "$ROOT/templates/$name"
  brand_fabric "$ROOT/templates/$name"
  sed -i "s/^rootProject.name = .*/rootProject.name = '$MOD_ID'/" "$ROOT/templates/$name/settings.gradle"
  apply_pins "$name"
  apply_overlay "$name"
done

for v in $NEOFORGE_TEMPLATES; do
  name="neoforge-$v"
  want "$name" "${SELECT[@]+"${SELECT[@]}"}" || continue
  log "MDK-$v-ModDevGradle -> templates/$name"
  clone_mdk "MDK-$v-ModDevGradle" "$ROOT/templates/$name"
  brand_mdk "$ROOT/templates/$name"
  apply_pins "$name"
  apply_overlay "$name"
done

if want "forge-1.20.1" "${SELECT[@]+"${SELECT[@]}"}"; then
  log "MDK-Forge-1.20.1-ModDevGradle -> templates/forge-1.20.1"
  clone_mdk "MDK-Forge-1.20.1-ModDevGradle" "$ROOT/templates/forge-1.20.1"
  brand_mdk "$ROOT/templates/forge-1.20.1"
  apply_pins "forge-1.20.1"
  apply_overlay "forge-1.20.1"
fi

log "готово. Проверьте git diff и прогоните CI (ci/build-templates.yml)."
