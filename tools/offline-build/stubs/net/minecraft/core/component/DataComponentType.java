package net.minecraft.core.component;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.StreamCodec;
public interface DataComponentType<T> {
    static <T> DataComponentType.Builder<T> builder() { return null; }
    public static class Builder<T> {
        public DataComponentType.Builder<T> persistent(Codec<T> codec) { return this; }
        public DataComponentType.Builder<T> networkSynchronized(StreamCodec<?, T> codec) { return this; }
        public DataComponentType<T> build() { return null; }
    }
}
