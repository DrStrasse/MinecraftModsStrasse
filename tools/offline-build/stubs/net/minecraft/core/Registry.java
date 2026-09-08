package net.minecraft.core;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
public interface Registry<T> {
    static <V, T extends V> T register(Registry<V> registry, ResourceLocation name, T value) { return value; }
    T get(ResourceLocation name);
    ResourceKey<? extends Registry<T>> key();
    Holder<T> wrapAsHolder(T value);
}
