package by.strasse.strassexray.client;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Клиентская подсветка руд, уже известных клиентскому миру.
 *
 * <p>Мод намеренно не пытается получать скрытые сервером блоки: если anti-xray
 * заменил руду в отправленном чанке камнем, клиент и этот сканер видят камень.
 * Использовать только в одиночной игре или там, где владелец сервера разрешил
 * подобные средства.</p>
 */
public final class StrasseXrayClient implements ClientModInitializer {
    private static final int HORIZONTAL_RADIUS = 24;
    private static final int VERTICAL_RADIUS = 16;
    private static final int RESCAN_TICKS = 12;
    private static final int MAX_MARKERS = 4096;
    private static final double BOX_INSET = 0.04D;

    private static final Registry<Block> BLOCK_REGISTRY = findBlockRegistry();
    private static final Map<Block, OreColor> COLOR_CACHE = new IdentityHashMap<>();

    private static KeyMapping toggleKey;
    private static boolean enabled;
    private static int ticksUntilScan;
    private static List<OreMarker> markers = List.of();

    @Override
    public void onInitializeClient() {
        // 88 = GLFW_KEY_X. После регистрации клавишу можно сменить в обычном меню управления.
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.strassexray.toggle", 88, "key.categories.strassexray"));

        ClientTickEvents.END_CLIENT_TICK.register(StrasseXrayClient::onEndTick);
        WorldRenderEvents.LAST.register(StrasseXrayClient::renderMarkers);
    }

    private static void onEndTick(Minecraft client) {
        while (toggleKey.consumeClick()) {
            enabled = !enabled;
            ticksUntilScan = 0;

            if (enabled && client.level != null && client.player != null) {
                scan(client.level, client.player.blockPosition());
                client.player.displayClientMessage(
                        Component.translatable("message.strassexray.enabled", markers.size()), true);
            } else {
                markers = List.of();

                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.translatable("message.strassexray.disabled"), true);
                }
            }
        }

        if (!enabled || client.level == null || client.player == null) {
            markers = List.of();
            return;
        }

        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }

        scan(client.level, client.player.blockPosition());
    }

    private static void scan(ClientLevel level, BlockPos center) {
        ticksUntilScan = RESCAN_TICKS;
        List<OreMarker> found = new ArrayList<>();
        BlockPos from = center.offset(-HORIZONTAL_RADIUS, -VERTICAL_RADIUS, -HORIZONTAL_RADIUS);
        BlockPos to = center.offset(HORIZONTAL_RADIUS, VERTICAL_RADIUS, HORIZONTAL_RADIUS);

        for (BlockPos cursor : BlockPos.betweenClosed(from, to)) {
            BlockState state = level.getBlockState(cursor);
            OreColor color = colorOf(state);

            if (color != null) {
                BlockPos pos = cursor.immutable();
                found.add(new OreMarker(pos.getX(), pos.getY(), pos.getZ(), color));

                if (found.size() >= MAX_MARKERS) {
                    break;
                }
            }
        }

        markers = List.copyOf(found);
    }

    /**
     * Находит реестр блоков по стабильному идентификатору, а не по имени
     * сгенерированного поля. Это одинаково работает после Fabric-remap.
     */
    @SuppressWarnings("unchecked")
    private static Registry<Block> findBlockRegistry() {
        for (Field field : BuiltInRegistries.class.getFields()) {
            if (!Registry.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                Registry<?> registry = (Registry<?>) field.get(null);

                if (registry != null
                        && "minecraft:block".equals(registry.key().location().toString())) {
                    return (Registry<Block>) registry;
                }
            } catch (IllegalAccessException ignored) {
                // Закрытое поле не является публичным реестром, продолжаем поиск.
            }
        }

        throw new IllegalStateException("Strasse X-Ray: не найден реестр блоков");
    }

    private static OreColor colorOf(BlockState state) {
        Block block = state.getBlock();

        if (COLOR_CACHE.containsKey(block)) {
            return COLOR_CACHE.get(block);
        }

        ResourceLocation id = BLOCK_REGISTRY.getKey(block);
        OreColor color = id == null ? null : switch (id.getPath()) {
            case "coal_ore", "deepslate_coal_ore" -> new OreColor(0.55F, 0.55F, 0.55F);
            case "iron_ore", "deepslate_iron_ore" -> new OreColor(0.90F, 0.68F, 0.48F);
            case "copper_ore", "deepslate_copper_ore" -> new OreColor(1.00F, 0.45F, 0.18F);
            case "gold_ore", "deepslate_gold_ore", "nether_gold_ore" -> new OreColor(1.00F, 0.82F, 0.05F);
            case "redstone_ore", "deepslate_redstone_ore" -> new OreColor(1.00F, 0.08F, 0.08F);
            case "lapis_ore", "deepslate_lapis_ore" -> new OreColor(0.12F, 0.35F, 1.00F);
            case "diamond_ore", "deepslate_diamond_ore" -> new OreColor(0.05F, 1.00F, 1.00F);
            case "emerald_ore", "deepslate_emerald_ore" -> new OreColor(0.05F, 1.00F, 0.28F);
            case "nether_quartz_ore" -> new OreColor(0.95F, 0.95F, 0.95F);
            case "ancient_debris" -> new OreColor(0.72F, 0.25F, 0.72F);
            default -> null;
        };

        COLOR_CACHE.put(block, color);
        return color;
    }

    private static void renderMarkers(WorldRenderContext context) {
        if (!enabled || markers.isEmpty() || context.matrixStack() == null) {
            return;
        }

        PoseStack poseStack = context.matrixStack();
        Vec3 camera = context.camera().getPosition();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.lineWidth(2.5F);

        poseStack.pushPose();

        try {
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.valueOf("LINES"), DefaultVertexFormat.POSITION_COLOR_NORMAL);

            for (OreMarker marker : markers) {
                double x = marker.x() + BOX_INSET;
                double y = marker.y() + BOX_INSET;
                double z = marker.z() + BOX_INSET;
                double maxX = marker.x() + 1.0D - BOX_INSET;
                double maxY = marker.y() + 1.0D - BOX_INSET;
                double maxZ = marker.z() + 1.0D - BOX_INSET;
                OreColor color = marker.color();

                LevelRenderer.renderLineBox(poseStack, buffer,
                        x, y, z, maxX, maxY, maxZ,
                        color.red(), color.green(), color.blue(), 1.0F);
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            poseStack.popPose();
            RenderSystem.lineWidth(1.0F);
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    private record OreMarker(int x, int y, int z, OreColor color) {
    }

    private record OreColor(float red, float green, float blue) {
    }
}
