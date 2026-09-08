#!/usr/bin/env python3
"""
Проверка собранного jar без Minecraft.

Делает две вещи:

1. Разбирает пул констант каждого класса мода и выписывает все обращения к чужому
   API — классы, поля и методы Minecraft, NeoForge и Fabric вместе с дескрипторами.
   Это главный способ убедиться, что оффлайн-сборка «сцепится» с настоящей игрой:
   JVM ищет метод по имени и дескриптору, поэтому отчёт можно построчно сверить
   с исходниками 1.21.1.

2. Просит JVM загрузить и верифицировать каждый класс мода (с заглушками на
   classpath). Это ловит битый байткод, несуществующие суперклассы и прочие
   структурные ошибки.

Использование:
  python3 tools/offline-build/verify.py dist/strassemods-neoforge-1.21.1.jar
  python3 tools/offline-build/verify.py --report tools/offline-build/api-usage.txt dist/*.jar
"""

from __future__ import annotations

import argparse
import io
import os
import struct
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORK = Path(os.environ.get("OFFLINE_BUILD_WORK", "/tmp/strassemods-offline"))

EXTERNAL_PREFIXES = ("net/minecraft/", "net/neoforged/", "net/fabricmc/",
                     "com/mojang/", "org/slf4j/", "org/spongepowered/")


def parse_constant_pool(data: bytes) -> tuple[list, int]:
    stream = io.BytesIO(data)

    if stream.read(4) != b"\xca\xfe\xba\xbe":
        raise ValueError("не class-файл")

    minor, major = struct.unpack(">HH", stream.read(4))
    count = struct.unpack(">H", stream.read(2))[0]
    pool: list = [None] * count
    index = 1

    while index < count:
        tag = stream.read(1)[0]

        if tag == 1:
            length = struct.unpack(">H", stream.read(2))[0]
            pool[index] = ("utf8", stream.read(length).decode("utf-8", "replace"))
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, struct.unpack(">H", stream.read(2))[0])
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, *struct.unpack(">HH", stream.read(4)))
        elif tag in (3, 4):
            pool[index] = (tag, stream.read(4))
        elif tag in (5, 6):
            pool[index] = (tag, stream.read(8))
            index += 1  # long и double занимают две ячейки
        elif tag == 15:
            pool[index] = (tag, *struct.unpack(">BH", stream.read(3)))
        else:
            raise ValueError(f"неизвестный тег пула констант: {tag}")

        index += 1

    return pool, major


def utf8(pool: list, index: int) -> str:
    entry = pool[index]
    return entry[1] if entry and entry[0] == "utf8" else f"<#{index}>"


def class_name(pool: list, index: int) -> str:
    return utf8(pool, pool[index][1])


def collect_references(data: bytes) -> tuple[set[str], int]:
    pool, major = parse_constant_pool(data)
    references: set[str] = set()

    for entry in pool:
        if not entry or entry[0] not in (9, 10, 11):
            continue

        kind = {9: "поле  ", 10: "метод ", 11: "метод*"}[entry[0]]
        owner = class_name(pool, entry[1])
        name_and_type = pool[entry[2]]
        member = utf8(pool, name_and_type[1])
        descriptor = utf8(pool, name_and_type[2])

        if owner.startswith(EXTERNAL_PREFIXES):
            references.add(f"{kind} {owner}#{member} {descriptor}")

    return references, major


def java_binary() -> str:
    if os.environ.get("OFFLINE_BUILD_JAVA"):
        return os.environ["OFFLINE_BUILD_JAVA"]

    candidate = WORK / "jdk" / "jdk4py" / "java-runtime" / "bin" / "java"
    return str(candidate) if candidate.exists() else "java"


def load_classes(jar: Path) -> bool:
    """Загружает все классы мода в JVM — проверка структуры и верификации байткода."""
    stub_classes = WORK / "stub-classes"

    if not stub_classes.exists():
        print("  ! нет скомпилированных заглушек — пропускаю загрузку в JVM")
        return True

    loader_src = WORK / "LoadAll.java"
    loader_src.write_text('''
import java.util.*; import java.util.zip.*; import java.io.*;
public class LoadAll {
    public static void main(String[] args) throws Exception {
        ZipFile zip = new ZipFile(args[0]);
        int ok = 0, failed = 0;
        for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements();) {
            String name = e.nextElement().getName();
            if (!name.endsWith(".class")) continue;
            String cls = name.substring(0, name.length() - 6).replace('/', '.');
            try {
                // initialize=false: статические инициализаторы дёргают API, а вместо
                // Minecraft подключены заглушки. Нас интересует только загрузка,
                // связывание и верификация байткода, поэтому класс не инициализируем.
                Class<?> c = Class.forName(cls, false, LoadAll.class.getClassLoader());
                c.getDeclaredMethods(); c.getDeclaredFields(); c.getSuperclass();
                ok++;
            }
            catch (Throwable t) { failed++; System.out.println("  ! " + cls + " -> " + t); }
        }
        System.out.println("  загружено классов: " + ok + ", с ошибками: " + failed);
        if (failed > 0) System.exit(1);
    }
}
''', encoding="utf-8")

    ecj = os.environ.get("OFFLINE_BUILD_ECJ", str(WORK / "ecj.jar"))
    subprocess.run([java_binary(), "-cp", ecj, "org.eclipse.jdt.internal.compiler.batch.Main",
                    "-source", "21", "-target", "21", "-nowarn", "-proc:none",
                    "-d", str(WORK / "loader"), str(loader_src)],
                   capture_output=True, text=True, check=True)

    result = subprocess.run(
        [java_binary(), "-cp", f"{WORK / 'loader'}:{stub_classes}:{jar}", "LoadAll", str(jar)],
        capture_output=True, text=True)
    print(result.stdout.rstrip())

    if result.returncode != 0:
        print(result.stderr.rstrip(), file=sys.stderr)

    return result.returncode == 0


def main() -> None:
    parser = argparse.ArgumentParser(description="Проверка оффлайн-собранного jar")
    parser.add_argument("jars", nargs="+", type=Path)
    parser.add_argument("--report", type=Path, help="куда записать отчёт об использовании API")
    args = parser.parse_args()

    report_lines: list[str] = []
    everything_ok = True

    for jar in args.jars:
        print(f"\n=== {jar}")
        references: set[str] = set()
        versions: set[int] = set()
        classes = 0

        with zipfile.ZipFile(jar) as archive:
            for name in sorted(archive.namelist()):
                if not name.endswith(".class"):
                    continue
                classes += 1
                found, major = collect_references(archive.read(name))
                references |= found
                versions.add(major)

        print(f"  классов: {classes}, версия class-файлов: {sorted(versions)} "
              f"(65 = Java 21)")
        print(f"  внешних обращений к API: {len(references)}")
        everything_ok &= load_classes(jar)

        report_lines.append(f"### {jar.name}")
        report_lines.append(f"классов: {classes}, версия байткода: {sorted(versions)}")
        report_lines.extend(sorted(references))
        report_lines.append("")

    if args.report:
        args.report.write_text(
            "# Обращения мода к внешнему API (owner#member descriptor)\n"
            "# Сгенерировано tools/offline-build/verify.py — сверяйте с исходниками 1.21.1.\n\n"
            + "\n".join(report_lines) + "\n", encoding="utf-8")
        print(f"\nотчёт: {args.report}")

    sys.exit(0 if everything_ok else 1)


if __name__ == "__main__":
    main()
