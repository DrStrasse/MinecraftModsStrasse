package com.mojang.blaze3d.systems;

import java.util.function.Supplier;
import net.minecraft.client.renderer.ShaderInstance;

public final class RenderSystem {
    public static void enableBlend() { }
    public static void disableBlend() { }
    public static void defaultBlendFunc() { }
    public static void disableDepthTest() { }
    public static void enableDepthTest() { }
    public static void depthMask(boolean mask) { }
    public static void lineWidth(float width) { }
    public static void setShader(Supplier<ShaderInstance> shader) { }
}
