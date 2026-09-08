package by.strasse.strassemods.compat;

import java.lang.reflect.Field;

import net.minecraft.ChatFormatting;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.phys.HitResult;

/**
 * Мост к ванильным константам для Fabric-сборки в intermediary-именах.
 *
 * <p>Проблема: имена полей вроде {@code Blocks.LIGHT} или {@code SoundEvents.LEVER_CLICK}
 * в intermediary выглядят как {@code field_10524} и вычисляются генератором имён Yarn
 * по содержимому реестров — в текстовых маппингах их просто нет, взять неоткуда.</p>
 *
 * <p>Решение: не обращаться к полям вовсе. Значения берутся из самих реестров по
 * строковым идентификаторам ({@code minecraft:light}), а константы перечислений — через
 * {@code valueOf}. Имена реестровых объектов и элементов перечислений не обфусцируются,
 * поэтому такой код одинаково работает в любом неймспейсе. Ремаппер
 * (tools/offline-build/fabric_remap.py) перенаправляет обращения к ванильным
 * константам на поля этого класса.</p>
 *
 * <p>Класс участвует только в оффлайн-сборке Fabric; при обычной сборке Gradle он не
 * используется — там Loom переименовывает мод сам.</p>
 */
public final class VanillaRefs {

    private VanillaRefs() {
    }

    // --- реестровые объекты -------------------------------------------------

    @SuppressWarnings("unchecked")
    public static final DefaultedRegistry<Item> BuiltInRegistries_ITEM =
            (DefaultedRegistry<Item>) registryById("minecraft:item");

    public static final Block Blocks_LIGHT = block("minecraft:light");
    public static final Block Blocks_PISTON = block("minecraft:piston");
    public static final Block Blocks_STICKY_PISTON = block("minecraft:sticky_piston");
    public static final Block Blocks_PISTON_HEAD = block("minecraft:piston_head");
    public static final Block Blocks_DISPENSER = block("minecraft:dispenser");
    public static final Block Blocks_DROPPER = block("minecraft:dropper");

    public static final SoundEvent SoundEvents_AMETHYST_BLOCK_CHIME = sound("minecraft:block.amethyst_block.chime");
    public static final SoundEvent SoundEvents_BOOK_PAGE_TURN = sound("minecraft:item.book.page_turn");
    public static final SoundEvent SoundEvents_DISPENSER_DISPENSE = sound("minecraft:block.dispenser.dispense");
    public static final SoundEvent SoundEvents_ENCHANTMENT_TABLE_USE = sound("minecraft:block.enchantment_table.use");
    public static final SoundEvent SoundEvents_EVOKER_PREPARE_SUMMON = sound("minecraft:entity.evoker.prepare_summon");
    public static final SoundEvent SoundEvents_EXPERIENCE_ORB_PICKUP = sound("minecraft:entity.experience_orb.pickup");
    public static final SoundEvent SoundEvents_FLINTANDSTEEL_USE = sound("minecraft:item.flintandsteel.use");
    public static final SoundEvent SoundEvents_GENERIC_EXTINGUISH_FIRE = sound("minecraft:entity.generic.extinguish_fire");
    public static final SoundEvent SoundEvents_ILLUSIONER_CAST_SPELL = sound("minecraft:entity.illusioner.cast_spell");
    public static final SoundEvent SoundEvents_IRON_DOOR_OPEN = sound("minecraft:block.iron_door.open");
    public static final SoundEvent SoundEvents_IRON_DOOR_CLOSE = sound("minecraft:block.iron_door.close");
    public static final SoundEvent SoundEvents_ITEM_BREAK = sound("minecraft:entity.item.break");
    public static final SoundEvent SoundEvents_LEVER_CLICK = sound("minecraft:block.lever.click");
    public static final SoundEvent SoundEvents_PISTON_EXTEND = sound("minecraft:block.piston.extend");
    public static final SoundEvent SoundEvents_PISTON_CONTRACT = sound("minecraft:block.piston.contract");
    public static final SoundEvent SoundEvents_STONE_BUTTON_CLICK_ON = sound("minecraft:block.stone_button.click_on");

    public static final SimpleParticleType ParticleTypes_ENCHANT = particle("minecraft:enchant");
    public static final SimpleParticleType ParticleTypes_END_ROD = particle("minecraft:end_rod");
    public static final SimpleParticleType ParticleTypes_FLAME = particle("minecraft:flame");
    public static final SimpleParticleType ParticleTypes_SMOKE = particle("minecraft:smoke");
    public static final SimpleParticleType ParticleTypes_WITCH = particle("minecraft:witch");
    public static final SimpleParticleType ParticleTypes_GLOW = particle("minecraft:glow");
    public static final SimpleParticleType ParticleTypes_ELECTRIC_SPARK = particle("minecraft:electric_spark");

    public static final Holder<MobEffect> MobEffects_LEVITATION = effect("minecraft:levitation");

    @SuppressWarnings("unchecked")
    public static final StatType<Item> Stats_ITEM_USED =
            (StatType<Item>) lookup("minecraft:stat_type", "minecraft:used");

    // --- константы перечислений --------------------------------------------

    public static final ChatFormatting ChatFormatting_LIGHT_PURPLE = ChatFormatting.valueOf("LIGHT_PURPLE");
    public static final ChatFormatting ChatFormatting_GRAY = ChatFormatting.valueOf("GRAY");
    public static final ChatFormatting ChatFormatting_DARK_GRAY = ChatFormatting.valueOf("DARK_GRAY");
    public static final ChatFormatting ChatFormatting_RED = ChatFormatting.valueOf("RED");

    public static final SoundSource SoundSource_PLAYERS = SoundSource.valueOf("PLAYERS");
    public static final SoundSource SoundSource_BLOCKS = SoundSource.valueOf("BLOCKS");

    public static final HitResult.Type HitResult$Type_BLOCK = HitResult.Type.valueOf("BLOCK");
    public static final ClipContext.Block ClipContext$Block_OUTLINE = ClipContext.Block.valueOf("OUTLINE");
    public static final ClipContext.Fluid ClipContext$Fluid_NONE = ClipContext.Fluid.valueOf("NONE");
    public static final PistonType PistonType_DEFAULT = PistonType.valueOf("DEFAULT");
    public static final PistonType PistonType_STICKY = PistonType.valueOf("STICKY");

    // --- вспомогательное ----------------------------------------------------

    /** Ищет реестр по его идентификатору, перебирая публичные поля BuiltInRegistries. */
    private static Registry<?> registryById(String id) {
        for (Field field : BuiltInRegistries.class.getFields()) {
            if (!Registry.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                Registry<?> registry = (Registry<?>) field.get(null);

                if (registry != null && id.equals(registry.key().location().toString())) {
                    return registry;
                }
            } catch (IllegalAccessException ignored) {
                // недоступное поле нам не подходит
            }
        }

        throw new IllegalStateException("Strasse Mods: не найден реестр " + id);
    }

    private static Object lookup(String registryId, String entryId) {
        Object value = registryById(registryId).get(ResourceLocation.parse(entryId));

        if (value == null) {
            throw new IllegalStateException("Strasse Mods: в реестре " + registryId + " нет " + entryId);
        }

        return value;
    }

    private static Block block(String id) {
        return (Block) lookup("minecraft:block", id);
    }

    private static SoundEvent sound(String id) {
        return (SoundEvent) lookup("minecraft:sound_event", id);
    }

    private static SimpleParticleType particle(String id) {
        return (SimpleParticleType) lookup("minecraft:particle_type", id);
    }

    @SuppressWarnings("unchecked")
    private static Holder<MobEffect> effect(String id) {
        Registry<MobEffect> registry = (Registry<MobEffect>) registryById("minecraft:mob_effect");
        return registry.wrapAsHolder((MobEffect) lookup("minecraft:mob_effect", id));
    }
}
