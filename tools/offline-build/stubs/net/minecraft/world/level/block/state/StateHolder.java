package net.minecraft.world.level.block.state;
import net.minecraft.world.level.block.state.properties.Property;
public abstract class StateHolder<O, S> {
    public <T extends Comparable<T>> T getValue(Property<T> property) { return null; }
    public <T extends Comparable<T>, V extends T> S setValue(Property<T> property, V value) { return null; }
    public <T extends Comparable<T>> S cycle(Property<T> property) { return null; }
    public boolean hasProperty(Property<?> property) { return false; }
}
