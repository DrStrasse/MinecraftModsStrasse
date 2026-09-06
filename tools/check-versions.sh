#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# check-versions.sh — сверяет версии, зафиксированные в data/versions.json,
# с актуальными в maven-репозиториях загрузчиков.
#
# Требуется доступ в интернет к maven.neoforged.net, maven.fabricmc.net,
# files.minecraftforge.net и meta.fabricmc.net.
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

hdr() { printf '\n\033[1;36m== %s\033[0m\n' "$*"; }

hdr "NeoForge — последние сборки по версиям Minecraft"
curl -sSf --max-time 60 https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml |
  grep -oE '<version>[^<]+</version>' | sed -E 's/<\/?version>//g' |
  grep -vE 'beta|alpha|snapshot' |
  awk -F. '{ key=$1"."$2; if ($1 ~ /^26$/) key=$1"."$2"."$3; store[key]=$0 } END { for (k in store) print k" -> "store[k] }' |
  sort -V

hdr "Forge — promotions_slim.json (1.20+)"
curl -sSf --max-time 60 https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json |
  python3 -c '
import json,sys
promos = json.load(sys.stdin)["promos"]
for k in sorted(promos):
    if k.startswith(("1.20","1.21","26.")):
        print(f"{k:28} {promos[k]}")'

hdr "Fabric — loader / стабильные версии игры"
curl -sSf --max-time 60 https://meta.fabricmc.net/v2/versions/loader |
  python3 -c 'import json,sys; d=json.load(sys.stdin); print("loader:", d[0]["version"])'
curl -sSf --max-time 60 https://meta.fabricmc.net/v2/versions/game |
  python3 -c 'import json,sys; d=json.load(sys.stdin); print("MC stable:", ", ".join(g["version"] for g in d if g["stable"])[:200])'

hdr "Fabric API — последние опубликованные"
curl -sSf --max-time 60 https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml |
  grep -oE '<version>[^<]+</version>' | sed -E 's/<\/?version>//g' | tail -12

hdr "Зафиксировано в data/versions.json"
python3 -c '
import json
d = json.load(open("'"$ROOT"'/data/versions.json"))
print("checked_at:", d["checked_at"])
for v in d["versions"]:
    forge = v.get("forge") or {}
    print(f"  MC {v[\"minecraft\"]:<8} java {v[\"java\"]:<3} neoforge {str(v.get(\"neoforge\")):<14} forge {str(forge.get(\"latest\")):<10} {v[\"status\"]}")'

printf '\nЕсли что-то разошлось — обновите data/versions.json, docs/01-версии-и-совместимость.md\nи пины в tools/sync-templates.sh, затем запустите ./tools/sync-templates.sh\n'
