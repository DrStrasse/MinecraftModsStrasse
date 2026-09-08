package net.minecraft.world;
public class InteractionResultHolder<T> {
    public static <T> InteractionResultHolder<T> success(T object) { return null; }
    public static <T> InteractionResultHolder<T> consume(T object) { return null; }
    public static <T> InteractionResultHolder<T> pass(T object) { return null; }
    public static <T> InteractionResultHolder<T> fail(T object) { return null; }
}
