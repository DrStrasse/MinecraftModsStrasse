#!/usr/bin/env python3
"""
Ремап Fabric-сборки из Mojang-имён в intermediary.

Зачем: в продакшене Fabric Loader знает классы Minecraft под intermediary-именами
(`net.minecraft.class_1935`), а мод, скомпилированный против Mojang-имён, падает с
`NoClassDefFoundError: net/minecraft/world/level/ItemLike`. При обычной сборке этим
занимается Loom (задача `remapJar`); здесь то же самое делается без Gradle.

Цепочка имён: mojmap -> yarn -> intermediary.
  * yarn -> intermediary берётся из репозитория FabricMC/yarn (ветка 1.21.1),
    файлы enigma: `CLASS class_1937 net/minecraft/world/World`;
  * mojmap -> yarn задан таблицами ниже вручную и **обязательно проверяется**:
    для каждого метода и поля дескриптор, пересчитанный в intermediary, должен
    совпасть с дескриптором из маппингов. Не совпал или имя не найдено — ошибка,
    молча собрать неправильную ссылку нельзя.

Сам ремап делается по пулу констант: имена классов, имён и дескрипторов дописываются
в конец пула новыми UTF8-записями, а ссылки на них переставляются. Индексы старых
записей не меняются, поэтому байткод, StackMapTable и BootstrapMethods остаются
корректными.

Использование:
  python3 tools/offline-build/fabric_remap.py \
      --yarn /tmp/yarn/mappings \
      --in  dist/magic_wand_strasse-fabric-1.21.1-named.jar \
      --out dist/magic_wand_strasse-fabric-1.21.1.jar
"""

from __future__ import annotations

import argparse
import io
import re
import struct
import sys
import zipfile
from pathlib import Path

# ---------------------------------------------------------------------------
# mojmap -> yarn: классы
# ---------------------------------------------------------------------------
CLASS_MAP = {
    "net/minecraft/ChatFormatting": "net/minecraft/util/Formatting",
    "net/minecraft/core/BlockPos": "net/minecraft/util/math/BlockPos",
    "net/minecraft/core/Direction": "net/minecraft/util/math/Direction",
    "net/minecraft/core/Holder": "net/minecraft/registry/entry/RegistryEntry",
    "net/minecraft/core/Position": "net/minecraft/util/math/Position",
    "net/minecraft/core/DefaultedRegistry": "net/minecraft/registry/DefaultedRegistry",
    "net/minecraft/core/Registry": "net/minecraft/registry/Registry",
    "net/minecraft/core/Vec3i": "net/minecraft/util/math/Vec3i",
    "net/minecraft/core/component/DataComponentType": "net/minecraft/component/ComponentType",
    "net/minecraft/core/component/DataComponentType$Builder": "net/minecraft/component/ComponentType$Builder",
    "net/minecraft/core/particles/ParticleOptions": "net/minecraft/particle/ParticleEffect",
    "net/minecraft/core/particles/ParticleTypes": "net/minecraft/particle/ParticleTypes",
    "net/minecraft/core/particles/SimpleParticleType": "net/minecraft/particle/SimpleParticleType",
    "net/minecraft/core/registries/BuiltInRegistries": "net/minecraft/registry/Registries",
    "net/minecraft/core/registries/Registries": "net/minecraft/registry/RegistryKeys",
    "net/minecraft/network/chat/Component": "net/minecraft/text/Text",
    "net/minecraft/network/chat/MutableComponent": "net/minecraft/text/MutableText",
    "net/minecraft/network/codec/ByteBufCodecs": "net/minecraft/network/codec/PacketCodecs",
    "net/minecraft/network/codec/StreamCodec": "net/minecraft/network/codec/PacketCodec",
    "net/minecraft/resources/ResourceKey": "net/minecraft/registry/RegistryKey",
    "net/minecraft/resources/ResourceLocation": "net/minecraft/util/Identifier",
    "net/minecraft/server/level/ServerLevel": "net/minecraft/server/world/ServerWorld",
    "net/minecraft/sounds/SoundEvent": "net/minecraft/sound/SoundEvent",
    "net/minecraft/sounds/SoundEvents": "net/minecraft/sound/SoundEvents",
    "net/minecraft/sounds/SoundSource": "net/minecraft/sound/SoundCategory",
    "net/minecraft/stats/Stat": "net/minecraft/stat/Stat",
    "net/minecraft/stats/StatType": "net/minecraft/stat/StatType",
    "net/minecraft/stats/Stats": "net/minecraft/stat/Stats",
    "net/minecraft/world/InteractionHand": "net/minecraft/util/Hand",
    "net/minecraft/world/InteractionResultHolder": "net/minecraft/util/TypedActionResult",
    "net/minecraft/world/effect/MobEffect": "net/minecraft/entity/effect/StatusEffect",
    "net/minecraft/world/effect/MobEffectInstance": "net/minecraft/entity/effect/StatusEffectInstance",
    "net/minecraft/world/effect/MobEffects": "net/minecraft/entity/effect/StatusEffects",
    "net/minecraft/world/entity/Entity": "net/minecraft/entity/Entity",
    "net/minecraft/world/entity/EquipmentSlot": "net/minecraft/entity/EquipmentSlot",
    "net/minecraft/world/entity/LivingEntity": "net/minecraft/entity/LivingEntity",
    "net/minecraft/world/entity/player/Abilities": "net/minecraft/entity/player/PlayerAbilities",
    "net/minecraft/world/entity/player/Player": "net/minecraft/entity/player/PlayerEntity",
    "net/minecraft/world/item/CreativeModeTab": "net/minecraft/item/ItemGroup",
    "net/minecraft/world/item/CreativeModeTabs": "net/minecraft/item/ItemGroups",
    "net/minecraft/world/item/Item": "net/minecraft/item/Item",
    "net/minecraft/world/item/Item$Properties": "net/minecraft/item/Item$Settings",
    "net/minecraft/world/item/Item$TooltipContext": "net/minecraft/item/Item$TooltipContext",
    "net/minecraft/world/item/ItemCooldowns": "net/minecraft/entity/player/ItemCooldownManager",
    "net/minecraft/world/item/ItemStack": "net/minecraft/item/ItemStack",
    "net/minecraft/world/item/TooltipFlag": "net/minecraft/item/tooltip/TooltipType",
    "net/minecraft/world/level/BlockGetter": "net/minecraft/world/BlockView",
    "net/minecraft/world/level/ClipContext": "net/minecraft/world/RaycastContext",
    "net/minecraft/world/level/ClipContext$Block": "net/minecraft/world/RaycastContext$ShapeType",
    "net/minecraft/world/level/ClipContext$Fluid": "net/minecraft/world/RaycastContext$FluidHandling",
    "net/minecraft/world/level/ItemLike": "net/minecraft/item/ItemConvertible",
    "net/minecraft/world/level/Level": "net/minecraft/world/World",
    "net/minecraft/world/level/block/BaseFireBlock": "net/minecraft/block/AbstractFireBlock",
    "net/minecraft/world/level/block/Block": "net/minecraft/block/Block",
    "net/minecraft/world/level/block/Blocks": "net/minecraft/block/Blocks",
    "net/minecraft/world/level/block/ButtonBlock": "net/minecraft/block/ButtonBlock",
    "net/minecraft/world/level/block/DoorBlock": "net/minecraft/block/DoorBlock",
    "net/minecraft/world/level/block/FenceGateBlock": "net/minecraft/block/FenceGateBlock",
    "net/minecraft/world/level/block/LeverBlock": "net/minecraft/block/LeverBlock",
    "net/minecraft/world/level/block/LightBlock": "net/minecraft/block/LightBlock",
    "net/minecraft/world/level/block/TrapDoorBlock": "net/minecraft/block/TrapdoorBlock",
    "net/minecraft/world/level/block/state/BlockState": "net/minecraft/block/BlockState",
    "net/minecraft/world/level/block/state/properties/BlockStateProperties": "net/minecraft/state/property/Properties",
    "net/minecraft/world/level/block/state/properties/BooleanProperty": "net/minecraft/state/property/BooleanProperty",
    "net/minecraft/world/level/block/state/properties/DirectionProperty": "net/minecraft/state/property/DirectionProperty",
    "net/minecraft/world/level/block/state/properties/EnumProperty": "net/minecraft/state/property/EnumProperty",
    "net/minecraft/world/level/block/state/properties/IntegerProperty": "net/minecraft/state/property/IntProperty",
    "net/minecraft/world/level/block/state/properties/PistonType": "net/minecraft/block/enums/PistonType",
    "net/minecraft/world/level/block/state/properties/Property": "net/minecraft/state/property/Property",
    "net/minecraft/world/phys/AABB": "net/minecraft/util/math/Box",
    "net/minecraft/world/phys/BlockHitResult": "net/minecraft/util/hit/BlockHitResult",
    "net/minecraft/world/phys/HitResult": "net/minecraft/util/hit/HitResult",
    "net/minecraft/world/phys/HitResult$Type": "net/minecraft/util/hit/HitResult$Type",
    "net/minecraft/world/phys/Vec3": "net/minecraft/util/math/Vec3d",
}

# ---------------------------------------------------------------------------
# mojmap -> yarn: методы и поля, ключ (mojmap-класс, mojmap-имя)
# ---------------------------------------------------------------------------
MEMBER_MAP = {
    # --- мир ---
    ("net/minecraft/world/level/Level", "getBlockState"): ("getBlockState", "net/minecraft/world/BlockView"),
    ("net/minecraft/world/level/Level", "isEmptyBlock"): ("isAir", "net/minecraft/world/WorldView"),
    ("net/minecraft/world/level/Level", "setBlock"): "setBlockState",
    ("net/minecraft/world/level/Level", "setBlockAndUpdate"): "setBlockState",
    ("net/minecraft/world/level/Level", "removeBlock"): "removeBlock",
    ("net/minecraft/world/level/Level", "clip"): "raycast",
    ("net/minecraft/world/level/Level", "getEntities"): "getOtherEntities",
    ("net/minecraft/world/level/Level", "playSound"): "playSound",
    ("net/minecraft/world/level/Level", "scheduleTick"): "scheduleBlockTick",
    ("net/minecraft/world/level/Level", "updateNeighborsAt"): "updateNeighborsAlways",
    ("net/minecraft/server/level/ServerLevel", "sendParticles"): "spawnParticles",
    # --- сущности ---
    ("net/minecraft/world/entity/Entity", "position"): ("getPos", "net/minecraft/entity/Entity"),
    ("net/minecraft/world/entity/Entity", "blockPosition"): ("getBlockPos", "net/minecraft/world/entity/EntityLike"),
    ("net/minecraft/world/entity/Entity", "getBoundingBox"): ("getBoundingBox", "net/minecraft/world/entity/EntityLike"),
    ("net/minecraft/world/entity/Entity", "getDeltaMovement"): "getVelocity",
    ("net/minecraft/world/entity/Entity", "setDeltaMovement"): "setVelocity",
    ("net/minecraft/world/entity/Entity", "getEyePosition"): "getEyePos",
    ("net/minecraft/world/entity/Entity", "getViewVector"): "getRotationVec",
    ("net/minecraft/world/entity/Entity", "getBbHeight"): "getHeight",
    ("net/minecraft/world/entity/Entity", "isSpectator"): "isSpectator",
    ("net/minecraft/world/entity/Entity", "level"): ("getWorld", "net/minecraft/entity/Entity"),
    ("net/minecraft/world/entity/Entity", "getX"): ("getX", "net/minecraft/entity/Entity"),
    ("net/minecraft/world/entity/Entity", "getY"): ("getY", "net/minecraft/entity/Entity"),
    ("net/minecraft/world/entity/Entity", "getZ"): ("getZ", "net/minecraft/entity/Entity"),
    ("net/minecraft/world/entity/Entity", "hurtMarked"): "velocityModified",
    ("net/minecraft/world/entity/Entity", "hasImpulse"): "velocityDirty",
    ("net/minecraft/world/entity/Entity", "fallDistance"): "fallDistance",
    ("net/minecraft/world/entity/LivingEntity", "addEffect"): "addStatusEffect",
    ("net/minecraft/world/entity/LivingEntity", "getSlotForHand"): "getSlotForHand",
    ("net/minecraft/world/entity/player/Player", "getItemInHand"): "getStackInHand",
    ("net/minecraft/world/entity/player/Player", "isSecondaryUseActive"): "shouldCancelInteraction",
    ("net/minecraft/world/entity/player/Player", "displayClientMessage"): "sendMessage",
    ("net/minecraft/world/entity/player/Player", "getCooldowns"): "getItemCooldownManager",
    ("net/minecraft/world/entity/player/Player", "awardStat"): "incrementStat",
    ("net/minecraft/world/entity/player/Player", "getAbilities"): "getAbilities",
    ("net/minecraft/world/entity/player/Abilities", "instabuild"): "creativeMode",
    ("net/minecraft/world/effect/MobEffects", "LEVITATION"): "LEVITATION",
    # --- предметы ---
    ("net/minecraft/world/item/Item", "use"): "use",
    ("net/minecraft/world/item/Item", "appendHoverText"): "appendTooltip",
    ("net/minecraft/world/item/Item", "isFoil"): "hasGlint",
    ("net/minecraft/world/item/Item", "asItem"): "asItem",
    ("net/minecraft/world/item/Item$Properties", "stacksTo"): "maxCount",
    ("net/minecraft/world/item/Item$Properties", "durability"): "maxDamage",
    ("net/minecraft/world/item/Item$Properties", "component"): "component",
    ("net/minecraft/world/item/ItemStack", "get"): ("get", "net/minecraft/component/ComponentHolder"),
    ("net/minecraft/world/item/ItemStack", "set"): "set",
    ("net/minecraft/world/item/ItemStack", "hurtAndBreak"): "damage",
    ("net/minecraft/world/item/ItemStack", "getItem"): "getItem",
    ("net/minecraft/world/item/ItemCooldowns", "addCooldown"): "set",
    ("net/minecraft/world/item/CreativeModeTabs", "TOOLS_AND_UTILITIES"): "TOOLS",
    ("net/minecraft/world/item/CreativeModeTabs", "INGREDIENTS"): "INGREDIENTS",
    ("net/minecraft/stats/Stats", "ITEM_USED"): "USED",
    ("net/minecraft/stats/StatType", "get"): "getOrCreateStat",
    # --- блоки и состояния ---
    ("net/minecraft/world/level/block/Block", "defaultBlockState"): "getDefaultState",
    ("net/minecraft/world/level/block/BaseFireBlock", "getState"): "getState",
    ("net/minecraft/world/level/block/DoorBlock", "setOpen"): "setOpen",
    ("net/minecraft/world/level/block/LightBlock", "LEVEL"): "LEVEL_15",
    ("net/minecraft/world/level/block/state/BlockState", "getBlock"): ("getBlock", "net/minecraft/block/AbstractBlock$AbstractBlockState"),
    ("net/minecraft/world/level/block/state/BlockState", "is"): "isOf",
    ("net/minecraft/world/level/block/state/BlockState", "getValue"): "get",
    ("net/minecraft/world/level/block/state/BlockState", "setValue"): "with",
    ("net/minecraft/world/level/block/state/BlockState", "cycle"): "cycle",
    ("net/minecraft/world/level/block/state/BlockState", "hasProperty"): "contains",
    ("net/minecraft/world/level/block/state/BlockState", "canBeReplaced"): "isReplaceable",
    # --- математика ---
    ("net/minecraft/core/BlockPos", "offset"): "add",
    ("net/minecraft/core/BlockPos", "relative"): "offset",
    ("net/minecraft/core/BlockPos", "immutable"): "toImmutable",
    ("net/minecraft/core/BlockPos", "containing"): "ofFloored",
    ("net/minecraft/core/BlockPos", "betweenClosed"): "iterate",
    ("net/minecraft/world/phys/Vec3", "add"): "add",
    ("net/minecraft/world/phys/Vec3", "subtract"): "subtract",
    ("net/minecraft/world/phys/Vec3", "scale"): "multiply",
    ("net/minecraft/world/phys/Vec3", "normalize"): "normalize",
    ("net/minecraft/world/phys/Vec3", "length"): "length",
    ("net/minecraft/world/phys/Vec3", "distanceToSqr"): "squaredDistanceTo",
    ("net/minecraft/world/phys/Vec3", "atCenterOf"): "ofCenter",
    ("net/minecraft/world/phys/Vec3", "x"): "x",
    ("net/minecraft/world/phys/Vec3", "y"): "y",
    ("net/minecraft/world/phys/Vec3", "z"): "z",
    ("net/minecraft/world/phys/AABB", "expandTowards"): "stretch",
    ("net/minecraft/world/phys/AABB", "inflate"): "expand",
    ("net/minecraft/world/phys/AABB", "clip"): "raycast",
    ("net/minecraft/world/phys/BlockHitResult", "getBlockPos"): "getBlockPos",
    ("net/minecraft/world/phys/BlockHitResult", "getDirection"): "getSide",
    ("net/minecraft/world/phys/BlockHitResult", "getType"): "getType",
    # --- текст, реестры, компоненты ---
    ("net/minecraft/network/chat/Component", "translatable"): "translatable",
    ("net/minecraft/network/chat/MutableComponent", "withStyle"): "formatted",
    ("net/minecraft/world/InteractionResultHolder", "success"): "success",
    ("net/minecraft/world/InteractionResultHolder", "consume"): "consume",
    ("net/minecraft/world/InteractionResultHolder", "fail"): "fail",
    ("net/minecraft/world/InteractionResultHolder", "pass"): "pass",
    ("net/minecraft/core/Registry", "register"): "register",
    ("net/minecraft/core/Registry", "get"): "get",
    ("net/minecraft/core/Registry", "key"): "getKey",
    ("net/minecraft/core/Registry", "wrapAsHolder"): "getEntry",
    ("net/minecraft/resources/ResourceKey", "location"): "getValue",
    ("net/minecraft/resources/ResourceLocation", "parse"): "of",
    ("net/minecraft/resources/ResourceLocation", "fromNamespaceAndPath"): "of",
    ("net/minecraft/core/component/DataComponentType", "builder"): "builder",
    ("net/minecraft/core/component/DataComponentType$Builder", "persistent"): "codec",
    ("net/minecraft/core/component/DataComponentType$Builder", "networkSynchronized"): "packetCodec",
    ("net/minecraft/core/component/DataComponentType$Builder", "build"): "build",
    ("net/minecraft/network/codec/ByteBufCodecs", "VAR_INT"): "VAR_INT",
    ("net/minecraft/core/registries/Registries", "ITEM"): "ITEM",
    ("net/minecraft/core/registries/Registries", "DATA_COMPONENT_TYPE"): "DATA_COMPONENT_TYPE",
    ("net/minecraft/core/registries/BuiltInRegistries", "ITEM"): "ITEM",
    ("net/minecraft/core/registries/BuiltInRegistries", "DATA_COMPONENT_TYPE"): "DATA_COMPONENT_TYPE",
    # --- звуки: в yarn у полей есть префикс категории ---
    ("net/minecraft/sounds/SoundEvents", "AMETHYST_BLOCK_CHIME"): "BLOCK_AMETHYST_BLOCK_CHIME",
    ("net/minecraft/sounds/SoundEvents", "BOOK_PAGE_TURN"): "ITEM_BOOK_PAGE_TURN",
    ("net/minecraft/sounds/SoundEvents", "DISPENSER_DISPENSE"): "BLOCK_DISPENSER_DISPENSE",
    ("net/minecraft/sounds/SoundEvents", "ENCHANTMENT_TABLE_USE"): "BLOCK_ENCHANTMENT_TABLE_USE",
    ("net/minecraft/sounds/SoundEvents", "EVOKER_PREPARE_SUMMON"): "ENTITY_EVOKER_PREPARE_SUMMON",
    ("net/minecraft/sounds/SoundEvents", "EXPERIENCE_ORB_PICKUP"): "ENTITY_EXPERIENCE_ORB_PICKUP",
    ("net/minecraft/sounds/SoundEvents", "FLINTANDSTEEL_USE"): "ITEM_FLINTANDSTEEL_USE",
    ("net/minecraft/sounds/SoundEvents", "GENERIC_EXTINGUISH_FIRE"): "ENTITY_GENERIC_EXTINGUISH_FIRE",
    ("net/minecraft/sounds/SoundEvents", "ILLUSIONER_CAST_SPELL"): "ENTITY_ILLUSIONER_CAST_SPELL",
    ("net/minecraft/sounds/SoundEvents", "IRON_DOOR_OPEN"): "BLOCK_IRON_DOOR_OPEN",
    ("net/minecraft/sounds/SoundEvents", "IRON_DOOR_CLOSE"): "BLOCK_IRON_DOOR_CLOSE",
    ("net/minecraft/sounds/SoundEvents", "ITEM_BREAK"): "ENTITY_ITEM_BREAK",
    ("net/minecraft/sounds/SoundEvents", "LEVER_CLICK"): "BLOCK_LEVER_CLICK",
    ("net/minecraft/sounds/SoundEvents", "PISTON_EXTEND"): "BLOCK_PISTON_EXTEND",
    ("net/minecraft/sounds/SoundEvents", "PISTON_CONTRACT"): "BLOCK_PISTON_CONTRACT",
    ("net/minecraft/sounds/SoundEvents", "STONE_BUTTON_CLICK_ON"): "BLOCK_STONE_BUTTON_CLICK_ON",
}

# Поля-константы enum'ов и статические поля, чьи имена в yarn совпадают с mojmap.
SAME_NAME_OWNERS = (
    "net/minecraft/ChatFormatting",
    "net/minecraft/core/Direction",
    "net/minecraft/core/particles/ParticleTypes",
    "net/minecraft/sounds/SoundSource",
    "net/minecraft/world/InteractionHand",
    "net/minecraft/world/entity/EquipmentSlot",
    "net/minecraft/world/level/ClipContext$Block",
    "net/minecraft/world/level/ClipContext$Fluid",
    "net/minecraft/world/level/block/Blocks",
    "net/minecraft/world/level/block/state/properties/BlockStateProperties",
    "net/minecraft/world/level/block/state/properties/PistonType",
    "net/minecraft/world/phys/HitResult$Type",
)

# Наследование: если член не описан у самого класса, ищем у предков.
HIERARCHY = {
    "net/minecraft/server/level/ServerLevel": ["net/minecraft/world/level/Level"],
    "net/minecraft/world/entity/LivingEntity": ["net/minecraft/world/entity/Entity"],
    "net/minecraft/world/entity/player/Player": ["net/minecraft/world/entity/LivingEntity",
                                                 "net/minecraft/world/entity/Entity"],
    "net/minecraft/core/BlockPos": ["net/minecraft/core/Vec3i"],
    "net/minecraft/world/phys/BlockHitResult": ["net/minecraft/world/phys/HitResult"],
    "net/minecraft/world/level/block/DoorBlock": ["net/minecraft/world/level/block/Block"],
    "net/minecraft/world/level/block/BaseFireBlock": ["net/minecraft/world/level/block/Block"],
    "net/minecraft/world/level/block/LightBlock": ["net/minecraft/world/level/block/Block"],
    "net/minecraft/network/chat/MutableComponent": ["net/minecraft/network/chat/Component"],
    "net/minecraft/core/DefaultedRegistry": ["net/minecraft/core/Registry"],
}

# Константы, которых нет в текстовых маппингах: их имена Yarn генерирует по
# содержимому реестров. Обращения к ним перенаправляются на класс-мост, который
# достаёт значения из реестров по идентификаторам (см. fabric-extra/.../VanillaRefs.java).
BRIDGE_CLASS = "by/strasse/strassemods/compat/VanillaRefs"

BRIDGED_CONSTANTS = {
    ("net/minecraft/core/registries/BuiltInRegistries", "ITEM"),
    ("net/minecraft/world/level/block/Blocks", "LIGHT"),
    ("net/minecraft/world/level/block/Blocks", "PISTON"),
    ("net/minecraft/world/level/block/Blocks", "STICKY_PISTON"),
    ("net/minecraft/world/level/block/Blocks", "PISTON_HEAD"),
    ("net/minecraft/world/level/block/Blocks", "DISPENSER"),
    ("net/minecraft/world/level/block/Blocks", "DROPPER"),
    ("net/minecraft/world/effect/MobEffects", "LEVITATION"),
    ("net/minecraft/stats/Stats", "ITEM_USED"),
}

BRIDGED_PREFIXES = (
    "net/minecraft/sounds/SoundEvents",
    "net/minecraft/core/particles/ParticleTypes",
    "net/minecraft/ChatFormatting",
    "net/minecraft/sounds/SoundSource",
    "net/minecraft/world/phys/HitResult$Type",
    "net/minecraft/world/level/ClipContext$Block",
    "net/minecraft/world/level/ClipContext$Fluid",
    "net/minecraft/world/level/block/state/properties/PistonType",
)


def bridge_field(owner: str, name: str) -> str | None:
    """Имя поля класса-моста для ванильной константы, либо None."""
    if (owner, name) in BRIDGED_CONSTANTS or owner in BRIDGED_PREFIXES:
        return owner.split("/")[-1] + "_" + name
    return None


# Имена, которые генерирует компилятор: в маппингах их нет и трогать не нужно.
SYNTHETIC = {"<init>", "<clinit>", "values", "valueOf", "ordinal", "name",
             "equals", "hashCode", "toString", "clone", "compareTo"}


# ---------------------------------------------------------------------------
# Разбор маппингов yarn (формат enigma)
# ---------------------------------------------------------------------------
class Yarn:
    def __init__(self, mappings_dir: Path):
        self.class_to_inter: dict[str, str] = {}
        self.members: dict[str, list[tuple[str, str, str, str]]] = {}

        for path in mappings_dir.rglob("*.mapping"):
            stack: list[tuple[int, str, str]] = []

            for raw in path.read_text(encoding="utf-8").splitlines():
                if not raw.strip():
                    continue

                indent = len(raw) - len(raw.lstrip("\t"))
                parts = raw.strip().split(" ")

                while stack and stack[-1][0] >= indent:
                    stack.pop()

                if parts[0] == "CLASS":
                    inter = parts[1]
                    yarn = parts[2] if len(parts) > 2 else inter

                    if stack:
                        inter = stack[-1][1] + "$" + inter.split("/")[-1]
                        yarn = stack[-1][2] + "$" + yarn.split("/")[-1]

                    stack.append((indent, inter, yarn))
                    self.class_to_inter[yarn] = inter
                    self.members.setdefault(inter, [])

                elif parts[0] in ("METHOD", "FIELD") and stack:
                    owner = stack[-1][1]

                    if len(parts) == 4:
                        kind, inter_name, yarn_name, descriptor = parts
                    elif len(parts) == 3:
                        kind, inter_name, descriptor = parts
                        yarn_name = inter_name
                    else:
                        continue

                    self.members.setdefault(owner, []).append(
                        (kind, inter_name, yarn_name, descriptor))

    def intermediary_class(self, yarn_name: str) -> str | None:
        return self.class_to_inter.get(yarn_name)

    def find_member(self, inter_owner: str, yarn_name: str, descriptor: str,
                    strict: bool = False) -> list[tuple[str, str]]:
        """Ищет (интермедиари-имя, класс-владелец) по yarn-имени и дескриптору."""
        found = [(inter_name, inter_owner)
                 for kind, inter_name, y_name, desc in self.members.get(inter_owner, [])
                 if y_name == yarn_name and desc == descriptor]

        if found or strict:
            return found

        # Метод может быть объявлен в суперклассе или интерфейсе — ищем по всем классам.
        found = [(inter_name, owner)
                 for owner, entries in self.members.items()
                 for kind, inter_name, y_name, desc in entries
                 if y_name == yarn_name and desc == descriptor]

        if found:
            return found

        # Ковариантный возврат: наследник сужает тип результата (BlockPos вместо Vec3i),
        # поэтому при неудаче сверяем только типы параметров.
        params = descriptor.split(")")[0] + ")"
        return [(inter_name, owner)
                for owner, entries in self.members.items()
                for kind, inter_name, y_name, desc in entries
                if y_name == yarn_name and desc.startswith(params)]


# ---------------------------------------------------------------------------
# Пересчёт имён
# ---------------------------------------------------------------------------
class Remapper:
    def __init__(self, yarn: Yarn):
        self.yarn = yarn
        self.errors: list[str] = []
        self.bridged: set[str] = set()
        self.class_cache: dict[str, str] = {}
        self.member_log: list[str] = []

    def map_class(self, name: str) -> str:
        if name in self.class_cache:
            return self.class_cache[name]

        result = name

        if name.startswith("net/minecraft/"):
            yarn_name = CLASS_MAP.get(name)

            if yarn_name is None:
                self.errors.append(f"нет соответствия для класса {name}")
            else:
                inter = self.yarn.intermediary_class(yarn_name)
                if inter is None:
                    self.errors.append(f"в маппингах yarn нет класса {yarn_name} (для {name})")
                else:
                    result = inter

        self.class_cache[name] = result
        return result

    def map_descriptor(self, descriptor: str) -> str:
        return re.sub(r"L([^;]+);",
                      lambda m: "L" + self.map_class(m.group(1)) + ";",
                      descriptor)

    def map_member(self, owner: str, name: str, descriptor: str) -> str:
        if not owner.startswith("net/minecraft/") or name in SYNTHETIC:
            return name

        entry = MEMBER_MAP.get((owner, name))

        for ancestor in HIERARCHY.get(owner, []):
            if entry is not None:
                break
            entry = MEMBER_MAP.get((ancestor, name))

        if entry is None and owner in SAME_NAME_OWNERS:
            entry = name

        # Значение таблицы — либо yarn-имя, либо пара (имя, класс-объявитель в yarn).
        declared_hint = None

        if isinstance(entry, tuple):
            yarn_name, declared_hint = entry
        else:
            yarn_name = entry

        if yarn_name is None:
            self.errors.append(f"нет соответствия для {owner}#{name} {descriptor}")
            return name

        inter_owner = self.map_class(owner)
        inter_descriptor = self.map_descriptor(descriptor)

        if declared_hint is not None:
            hint_owner = self.yarn.intermediary_class(declared_hint)

            if hint_owner is None:
                self.errors.append(f"в маппингах yarn нет класса-объявителя {declared_hint}")
                return name

            matches = self.yarn.find_member(hint_owner, yarn_name, inter_descriptor, strict=True)
        else:
            matches = self.yarn.find_member(inter_owner, yarn_name, inter_descriptor)

        if not matches:
            self.errors.append(
                f"в yarn не найден {yarn_name}{inter_descriptor} у {inter_owner} "
                f"(mojmap {owner}#{name} {descriptor})")
            return name

        distinct = {inter_name for inter_name, _ in matches}

        if len(distinct) > 1:
            self.errors.append(
                f"неоднозначное соответствие для {owner}#{name} {descriptor}: "
                f"кандидаты {sorted(distinct)} — укажите класс-объявитель в MEMBER_MAP")
            return name

        inter_name, declared_in = matches[0]
        note = "" if declared_in == inter_owner else f" (объявлен в {declared_in})"
        self.member_log.append(
            f"{owner}#{name} {descriptor}\n    -> {inter_owner}#{inter_name} {inter_descriptor}{note}")
        return inter_name


# ---------------------------------------------------------------------------
# Правка class-файла
# ---------------------------------------------------------------------------
def remap_class_file(data: bytes, remapper: Remapper) -> bytes:
    stream = io.BytesIO(data)

    if stream.read(4) != b"\xca\xfe\xba\xbe":
        raise ValueError("не class-файл")

    stream.read(4)  # minor/major
    pool_start = stream.tell()
    count = struct.unpack(">H", stream.read(2))[0]

    pool: dict[int, tuple] = {}
    offsets: dict[int, int] = {}
    index = 1

    while index < count:
        offsets[index] = stream.tell()
        tag = stream.read(1)[0]

        if tag == 1:
            length = struct.unpack(">H", stream.read(2))[0]
            pool[index] = (1, stream.read(length).decode("utf-8", "surrogatepass"))
        elif tag in (7, 8, 16, 19, 20):
            pool[index] = (tag, struct.unpack(">H", stream.read(2))[0])
        elif tag in (9, 10, 11, 12, 17, 18):
            pool[index] = (tag, *struct.unpack(">HH", stream.read(4)))
        elif tag in (3, 4):
            pool[index] = (tag, stream.read(4))
        elif tag in (5, 6):
            pool[index] = (tag, stream.read(8))
            index += 1
        elif tag == 15:
            pool[index] = (tag, *struct.unpack(">BH", stream.read(3)))
        else:
            raise ValueError(f"неизвестный тег пула констант: {tag}")

        index += 1

    pool_end = stream.tell()
    rest = data[pool_end:]

    added: list[bytes] = []
    next_index = count
    utf8_cache: dict[str, int] = {}

    def utf8_index(text: str) -> int:
        nonlocal next_index

        if text in utf8_cache:
            return utf8_cache[text]

        encoded = text.encode("utf-8")
        added.append(b"\x01" + struct.pack(">H", len(encoded)) + encoded)
        utf8_cache[text] = next_index
        next_index += 1
        return utf8_cache[text]

    patches: dict[int, bytes] = {}   # смещение в файле -> новые 2 байта

    def text_of(idx: int) -> str:
        return pool[idx][1]

    # 1. CONSTANT_Class: имена классов
    for idx, entry in pool.items():
        if entry[0] != 7:
            continue

        original = text_of(entry[1])
        base = original.lstrip("[")
        prefix = original[:len(original) - len(base)]

        if base.startswith("L") and base.endswith(";"):
            mapped = prefix + "L" + remapper.map_class(base[1:-1]) + ";"
        else:
            mapped = prefix + remapper.map_class(base)

        if mapped != original:
            patches[offsets[idx] + 1] = struct.pack(">H", utf8_index(mapped))

    def class_entry(name: str) -> int:
        """Добавляет CONSTANT_Class с указанным именем и возвращает его индекс."""
        nonlocal next_index
        added.append(b"\x07" + struct.pack(">H", utf8_index(name)))
        next_index += 1
        return next_index - 1

    # 2. Fieldref/Methodref/InterfaceMethodref: своя NameAndType на каждую ссылку
    for idx, entry in pool.items():
        if entry[0] not in (9, 10, 11):
            continue

        owner = text_of(pool[entry[1]][1])
        name_and_type = pool[entry[2]]
        member = text_of(name_and_type[1])
        descriptor = text_of(name_and_type[2])

        # Ванильные константы без имени в маппингах уходят на класс-мост.
        bridged = bridge_field(owner, member) if entry[0] == 9 else None

        if bridged is not None:
            remapper.bridged.add(f"{owner}#{member} -> {BRIDGE_CLASS}#{bridged}")
            new_descriptor = remapper.map_descriptor(descriptor)
            added.append(b"\x0c" + struct.pack(">HH", utf8_index(bridged), utf8_index(new_descriptor)))
            nat_index = next_index
            next_index += 1
            patches[offsets[idx] + 1] = struct.pack(">H", class_entry(BRIDGE_CLASS))
            patches[offsets[idx] + 3] = struct.pack(">H", nat_index)
            continue

        new_member = remapper.map_member(owner, member, descriptor)
        new_descriptor = remapper.map_descriptor(descriptor)

        if (new_member, new_descriptor) == (member, descriptor):
            continue

        added.append(b"\x0c" + struct.pack(">HH", utf8_index(new_member), utf8_index(new_descriptor)))
        nat_index = next_index
        next_index += 1
        patches[offsets[idx] + 3] = struct.pack(">H", nat_index)

    # 3. Дескрипторы собственных полей и методов класса
    tail = io.BytesIO(rest)
    tail.read(6)  # access_flags, this_class, super_class
    interfaces = struct.unpack(">H", tail.read(2))[0]
    tail.read(interfaces * 2)

    def patch_members() -> None:
        member_count = struct.unpack(">H", tail.read(2))[0]

        for _ in range(member_count):
            tail.read(2)                                     # access_flags
            tail.read(2)                                     # name_index
            descriptor_offset = pool_end + tail.tell()
            descriptor_index = struct.unpack(">H", tail.read(2))[0]
            descriptor = text_of(descriptor_index)
            mapped = remapper.map_descriptor(descriptor)

            if mapped != descriptor:
                patches[descriptor_offset] = struct.pack(">H", utf8_index(mapped))

            attribute_count = struct.unpack(">H", tail.read(2))[0]
            for _ in range(attribute_count):
                tail.read(2)
                length = struct.unpack(">I", tail.read(4))[0]
                tail.read(length)

    patch_members()   # поля
    patch_members()   # методы

    # Сборка нового файла: тело с патчами + дописанные записи пула
    body = bytearray(data)
    for offset, value in patches.items():
        body[offset:offset + 2] = value

    body[pool_start:pool_start + 2] = struct.pack(">H", next_index)
    return bytes(body[:pool_end]) + b"".join(added) + bytes(body[pool_end:])


def remap_stub_classes(yarn: Yarn, source: Path, target: Path) -> int:
    """Переименовывает скомпилированные заглушки — нужны для проверки jar в JVM."""
    import shutil

    shutil.rmtree(target, ignore_errors=True)
    remapper = Remapper(yarn)
    count = 0

    for path in source.rglob("*.class"):
        data = remap_class_file(path.read_bytes(), remapper)
        name = str(path.relative_to(source))[:-6].replace("\\", "/")
        out = target / (remapper.map_class(name) + ".class")
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_bytes(data)
        count += 1

    return count


def remap_jar(yarn_dir: Path, source: Path, target: Path, log: Path | None = None) -> None:
    """Ремапит jar целиком; вызывается и из build.py."""
    yarn = Yarn(yarn_dir)
    remapper = Remapper(yarn)
    stamp = (2026, 1, 1, 0, 0, 0)
    entries: list[tuple[str, bytes]] = []

    with zipfile.ZipFile(source) as archive:
        for name in sorted(archive.namelist()):
            data = archive.read(name)
            if name.endswith(".class"):
                data = remap_class_file(data, remapper)
            elif name == "META-INF/MANIFEST.MF":
                data = data.replace(b"\r\n\r\n", b"\r\nFabric-Mapping-Namespace: intermediary\r\n\r\n")
            entries.append((name, data))

    if remapper.errors:
        print("\n".join(f"  ! {message}" for message in sorted(set(remapper.errors))))
        sys.exit(f"ремап остановлен: неразрешённых имён — {len(set(remapper.errors))}")

    with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as jar:
        for name, data in entries:
            info = zipfile.ZipInfo(name, date_time=stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o644 << 16
            jar.writestr(info, data)

    print(f"[remap] сопоставлено членов: {len(set(remapper.member_log))}, "
          f"констант через класс-мост: {len(remapper.bridged)}")
    print(f"[remap] готов {target} ({target.stat().st_size // 1024} КиБ)")

    if log:
        log.write_text(
            "# Соответствие имён mojmap -> intermediary (Fabric 1.21.1)\n"
            "# Сгенерировано tools/offline-build/fabric_remap.py\n\n"
            + "\n".join(sorted(set(remapper.member_log)))
            + "\n\n# Константы, перенаправленные на класс-мост\n"
            + "\n".join(sorted(remapper.bridged)) + "\n", encoding="utf-8")
        print(f"[remap] лог соответствий: {log}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Ремап Fabric-jar в intermediary")
    parser.add_argument("--yarn", type=Path, default=Path("/tmp/yarn/mappings"))
    parser.add_argument("--in", dest="source", type=Path, required=True)
    parser.add_argument("--out", dest="target", type=Path, required=True)
    parser.add_argument("--log", type=Path, help="куда выписать соответствия имён")
    parser.add_argument("--stubs", type=Path, help="каталог скомпилированных заглушек")
    parser.add_argument("--stubs-out", type=Path, help="куда положить переименованные заглушки")
    args = parser.parse_args()

    if not args.yarn.exists():
        sys.exit(f"нет маппингов yarn: {args.yarn}\n"
                 "git clone --depth 1 -b 1.21.1 https://github.com/FabricMC/yarn.git /tmp/yarn")

    yarn = Yarn(args.yarn)
    print(f"[remap] маппингов yarn: {len(yarn.class_to_inter)} классов")
    remap_jar(args.yarn, args.source, args.target, args.log)

    if args.stubs and args.stubs_out:
        count = remap_stub_classes(yarn, args.stubs, args.stubs_out)
        print(f"[remap] переименовано заглушек: {count}")


if __name__ == "__main__":
    main()
