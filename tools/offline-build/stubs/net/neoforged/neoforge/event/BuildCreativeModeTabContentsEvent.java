package net.neoforged.neoforge.event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
public final class BuildCreativeModeTabContentsEvent extends Event {
    public ResourceKey<CreativeModeTab> getTabKey() { return null; }
    public void accept(ItemLike item) { }
}
