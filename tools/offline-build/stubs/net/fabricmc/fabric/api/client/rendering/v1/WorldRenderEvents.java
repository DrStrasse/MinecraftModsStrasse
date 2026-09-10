package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;

public final class WorldRenderEvents {
    public static final Event<Last> LAST = null;

    @FunctionalInterface
    public interface Last {
        void onLast(WorldRenderContext context);
    }
}
