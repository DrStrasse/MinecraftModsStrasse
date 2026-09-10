#!/usr/bin/env python3
"""
Оффлайн-сборка jar-файлов мода без Gradle, интернета и полноценного JDK.

Зачем это нужно: обычная сборка (`./gradlew build`) требует доступа к maven-репозиториям
Fabric/NeoForge/Mojang и десятков мегабайт зависимостей. Когда их нет, мод всё равно
можно скомпилировать — против набора API-заглушек (`tools/offline-build/stubs`),
описывающих ровно те классы и сигнатуры Minecraft и загрузчика, которые использует мод.
Байткод получается тот же самый: JVM связывает вызовы по имени и дескриптору, а они
у заглушек совпадают с настоящим API 1.21.1.

Требуется:
  * JVM (Java 17+) — например, из pip-пакета jdk4py;
  * ECJ (Eclipse Compiler for Java) — jar с org.eclipse.jdt.internal.compiler.batch.Main.
Оба ставит `tools/offline-build/fetch-toolchain.sh`.

Использование:
  python3 tools/offline-build/build.py --template neoforge-1.21.1
  python3 tools/offline-build/build.py --all --out dist

Ограничения:
  * Fabric-сборка получается в «именованном» (Mojang) неймспейсе — для продакшена
    её должен ремапить Loom. NeoForge 1.21.1 работает на Mojang-именах и в проде,
    поэтому его jar пригоден к запуску как есть.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STUBS = ROOT / "tools" / "offline-build" / "stubs"
WORK = Path(os.environ.get("OFFLINE_BUILD_WORK", "/tmp/strassemods-offline"))

ECJ_MAIN = "org.eclipse.jdt.internal.compiler.batch.Main"

# Какие исходники не идут в оффлайн-сборку: миксины требуют org.spongepowered
# и refmap, которые генерирует Loom, — их заглушками не заменить.
TEMPLATES = {
    "neoforge-1.21.1": {
        "source_dirs": ["src/main/java"],
        "resource_dirs": ["src/main/resources"],
        "exclude": [],
        "metadata": "neoforge",
        "jar": "magic_wand_strasse-neoforge-1.21.1.jar",
    },
    "fabric-1.21.1": {
        "source_dirs": ["src/main/java", "src/client/java"],
        "extra_sources": ["tools/offline-build/fabric-extra"],
        "resource_dirs": ["src/main/resources", "src/client/resources"],
        "exclude": ["**/mixin/**"],
        "metadata": "fabric",
        "jar": "magic_wand_strasse-fabric-1.21.1-named.jar",
        "remap": "magic_wand_strasse-fabric-1.21.1.jar",
    },
    "xray-fabric-1.21.1": {
        "path": "mods/xray-fabric-1.21.1",
        "source_dirs": ["src/client/java"],
        "resource_dirs": ["src/main/resources", "src/client/resources"],
        "exclude": [],
        "metadata": "fabric",
        "implementation_title": "xray_strasse",
        "mapping_log": "tools/offline-build/xray-fabric-mappings.txt",
        "jar": "xray_strasse-fabric-1.21.1-named.jar",
        "remap": "xray_strasse-fabric-1.21.1.jar",
    },
}


def log(message: str) -> None:
    print(f"\033[36m[offline]\033[0m {message}")


def find_java() -> str:
    if os.environ.get("OFFLINE_BUILD_JAVA"):
        return os.environ["OFFLINE_BUILD_JAVA"]

    for candidate in (WORK / "jdk" / "jdk4py" / "java-runtime" / "bin" / "java",):
        if candidate.exists():
            return str(candidate)

    found = shutil.which("java")
    if found:
        return found

    sys.exit("Не найдена JVM. Запустите tools/offline-build/fetch-toolchain.sh "
             "или задайте OFFLINE_BUILD_JAVA=/путь/к/java")


def find_ecj() -> str:
    if os.environ.get("OFFLINE_BUILD_ECJ"):
        return os.environ["OFFLINE_BUILD_ECJ"]

    candidate = WORK / "ecj.jar"
    if candidate.exists():
        return str(candidate)

    sys.exit("Не найден ECJ. Запустите tools/offline-build/fetch-toolchain.sh "
             "или задайте OFFLINE_BUILD_ECJ=/путь/к/ecj.jar")


def read_gradle_properties(template_dir: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    path = template_dir / "gradle.properties"

    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()

    return props


def collect_sources(template_dir: Path, config: dict) -> list[Path]:
    sources: list[Path] = []

    for rel in config.get("extra_sources", []):
        sources.extend(sorted((ROOT / rel).rglob("*.java")))

    for rel in config["source_dirs"]:
        base = template_dir / rel
        if not base.exists():
            continue
        for path in sorted(base.rglob("*.java")):
            if any(path.match(pattern) for pattern in config["exclude"]):
                continue
            sources.append(path)

    return sources


def run_ecj(java: str, ecj: str, args: list[str]) -> None:
    result = subprocess.run([java, "-cp", ecj, ECJ_MAIN,
                             "-source", "21", "-target", "21",
                             "-nowarn", "-proc:none", *args],
                            capture_output=True, text=True)

    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr, file=sys.stderr)
        sys.exit("Компиляция не удалась")


def compile_stubs(java: str, ecj: str) -> Path:
    """Заглушки компилируются отдельно и подключаются как classpath.

    Так в каталоге с классами мода не остаётся ни одного файла заглушки:
    попади они в jar — перекрыли бы настоящие классы Minecraft.
    """
    out_dir = WORK / "stub-classes"

    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)

    stub_sources = sorted(str(path) for path in STUBS.rglob("*.java"))
    log(f"заглушек API: {len(stub_sources)}")
    run_ecj(java, ecj, ["-d", str(out_dir), *stub_sources])
    return out_dir


def compile_sources(java: str, ecj: str, sources: list[Path], out_dir: Path, stub_classes: Path) -> None:
    if out_dir.exists():
        shutil.rmtree(out_dir)
    out_dir.mkdir(parents=True)

    run_ecj(java, ecj, ["-classpath", str(stub_classes), "-d", str(out_dir),
                        *[str(path) for path in sources]])


def expand_placeholders(text: str, props: dict[str, str]) -> str:
    def replace(match: re.Match[str]) -> str:
        key = match.group(1)
        if key not in props:
            sys.exit(f"В gradle.properties нет значения для ${{{key}}}")
        return props[key]

    return re.sub(r"\$\{([A-Za-z0-9_]+)\}", replace, text)


def fabric_mod_json(raw: str, props: dict[str, str]) -> str:
    """Подставляет версию и убирает миксины, которых нет в оффлайн-сборке."""
    data = json.loads(raw.replace("${version}", props.get("mod_version", "1.0.0")))
    data.pop("mixins", None)
    return json.dumps(data, ensure_ascii=False, indent=2) + "\n"


def yarn_mappings() -> Path | None:
    """Каталог маппингов yarn для ремапа Fabric-сборки."""
    for candidate in (Path(os.environ["OFFLINE_BUILD_YARN"]) if os.environ.get("OFFLINE_BUILD_YARN") else None,
                      WORK / "yarn" / "mappings", Path("/tmp/yarn/mappings")):
        if candidate and candidate.exists():
            return candidate
    return None


def build_jar(template: str, out_dir: Path, java: str, ecj: str, stub_classes: Path) -> Path:
    config = TEMPLATES[template]
    template_dir = ROOT / config.get("path", f"templates/{template}")
    props = read_gradle_properties(template_dir)
    props.setdefault("mod_version", "1.0.0")

    sources = collect_sources(template_dir, config)
    log(f"{template}: исходников — {len(sources)}")

    classes_dir = WORK / "classes" / template
    compile_sources(java, ecj, sources, classes_dir, stub_classes)
    class_files = sorted(classes_dir.rglob("*.class"))
    log(f"{template}: скомпилировано классов — {len(class_files)}")

    out_dir.mkdir(parents=True, exist_ok=True)
    needs_remap = "remap" in config
    jar_path = (WORK / config["jar"]) if needs_remap else (out_dir / config["jar"])

    # Фиксированная дата — чтобы одинаковый вход давал побайтово одинаковый jar.
    stamp = (2026, 1, 1, 0, 0, 0)

    with zipfile.ZipFile(jar_path, "w", zipfile.ZIP_DEFLATED) as jar:
        def write(name: str, data: bytes) -> None:
            info = zipfile.ZipInfo(name, date_time=stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            jar.writestr(info, data)

        manifest = (
            "Manifest-Version: 1.0\r\n"
            f"Implementation-Title: {config.get('implementation_title', 'magic_wand_strasse')}\r\n"
            f"Specification-Title: {props.get('mod_name', 'Strasse Mods')}\r\n"
            f"Implementation-Version: {props['mod_version']}\r\n"
            f"Implementation-Vendor: {props.get('mod_authors', 'DrStrasse')}\r\n"
            "Built-By: tools/offline-build/build.py\r\n"
            "\r\n"
        )
        write("META-INF/MANIFEST.MF", manifest.encode("utf-8"))

        for class_file in class_files:
            write(str(class_file.relative_to(classes_dir)).replace(os.sep, "/"),
                  class_file.read_bytes())

        for rel in config["resource_dirs"]:
            base = template_dir / rel
            if not base.exists():
                continue
            for path in sorted(base.rglob("*")):
                if not path.is_file():
                    continue
                name = str(path.relative_to(base)).replace(os.sep, "/")
                if name == "fabric.mod.json":
                    write(name, fabric_mod_json(path.read_text(encoding="utf-8"), props).encode("utf-8"))
                elif name.endswith(".mixins.json"):
                    continue
                else:
                    write(name, path.read_bytes())

        if config["metadata"] == "neoforge":
            toml_template = template_dir / "src" / "main" / "templates" / "META-INF" / "neoforge.mods.toml"
            write("META-INF/neoforge.mods.toml",
                  expand_placeholders(toml_template.read_text(encoding="utf-8"), props).encode("utf-8"))

    if not needs_remap:
        log(f"{template}: готов {jar_path.relative_to(ROOT)} ({jar_path.stat().st_size // 1024} КиБ)")
        return jar_path

    # Fabric в продакшене работает с intermediary-именами — переименовываем.
    mappings = yarn_mappings()

    if mappings is None:
        sys.exit("нет маппингов yarn для ремапа Fabric-сборки — запустите "
                 "tools/offline-build/fetch-toolchain.sh")

    sys.path.insert(0, str(ROOT / "tools" / "offline-build"))
    import fabric_remap

    target = out_dir / config["remap"]
    mapping_log = ROOT / config.get("mapping_log", "tools/offline-build/fabric-mappings.txt")
    fabric_remap.remap_jar(mappings, jar_path, target, mapping_log)
    fabric_remap.remap_stub_classes(fabric_remap.Yarn(mappings), stub_classes,
                                    WORK / "stub-classes-inter")
    log(f"{template}: готов {target.relative_to(ROOT)} ({target.stat().st_size // 1024} КиБ)")
    return target


def main() -> None:
    parser = argparse.ArgumentParser(description="Оффлайн-сборка jar-модов Strasse")
    parser.add_argument("--template", action="append", choices=sorted(TEMPLATES),
                        help="какой шаблон собрать (можно несколько раз)")
    parser.add_argument("--all", action="store_true", help="собрать все поддерживаемые шаблоны")
    parser.add_argument("--out", default="dist", help="каталог для jar (по умолчанию dist)")
    args = parser.parse_args()

    templates = sorted(TEMPLATES) if args.all or not args.template else args.template
    java, ecj = find_java(), find_ecj()
    log(f"JVM: {java}")
    log(f"ECJ: {ecj}")

    stub_classes = compile_stubs(java, ecj)

    for template in templates:
        build_jar(template, ROOT / args.out, java, ecj, stub_classes)


if __name__ == "__main__":
    main()
