#!/usr/bin/env bash
# Ставит инструменты для оффлайн-сборки jar: JVM и компилятор ECJ.
#
# Ни JDK, ни maven-репозитории при этом не нужны — всё берётся из мест,
# доступных даже в изолированной среде:
#   * JVM — pip-пакет jdk4py (Temurin, собран jlink'ом);
#   * ECJ — jar Eclipse-компилятора из git-дерева публичного репозитория.
#
# Итог кладётся в $OFFLINE_BUILD_WORK (по умолчанию /tmp/strassemods-offline).
set -euo pipefail

WORK="${OFFLINE_BUILD_WORK:-/tmp/strassemods-offline}"
ECJ_REPO="Moonshine-IDE/Moonshine-IDE"
ECJ_BLOB="609cd06503a7e2307d1294453481336135876abf"   # org.eclipse.jdt.core.compiler.batch_3.39.0

log() { printf '\033[36m[toolchain]\033[0m %s\n' "$*"; }

mkdir -p "$WORK"

if [ ! -x "$WORK/jdk/jdk4py/java-runtime/bin/java" ]; then
  log "ставлю JVM (pip jdk4py) -> $WORK/jdk"
  pip install --quiet --target "$WORK/jdk" jdk4py
fi
"$WORK/jdk/jdk4py/java-runtime/bin/java" -version 2>&1 | head -1

if [ ! -f "$WORK/ecj.jar" ]; then
  log "качаю компилятор ECJ через GitHub API"
  gh api "repos/$ECJ_REPO/git/blobs/$ECJ_BLOB" --jq '.content' | base64 -d > "$WORK/ecj.jar"
fi
"$WORK/jdk/jdk4py/java-runtime/bin/java" -cp "$WORK/ecj.jar" \
  org.eclipse.jdt.internal.compiler.batch.Main -version

log "готово. Теперь: python3 tools/offline-build/build.py --all"
