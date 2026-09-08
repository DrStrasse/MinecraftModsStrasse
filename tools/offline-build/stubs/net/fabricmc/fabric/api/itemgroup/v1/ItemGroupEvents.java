package net.fabricmc.fabric.api.itemgroup.v1;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
public final class ItemGroupEvents {
    public static Event<ItemGroupEvents.ModifyEntries> modifyEntriesEvent(ResourceKey<CreativeModeTab> tab) { return null; }

    @FunctionalInterface
    public interface ModifyEntries {
        void modifyEntries(FabricItemGroupEntries entries);
    }
}
