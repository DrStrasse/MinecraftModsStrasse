package net.fabricmc.fabric.api.client.rendering.v1;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

public interface WorldRenderContext {
    PoseStack matrixStack();
    Camera camera();
}
