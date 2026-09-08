package net.minecraft.world.item;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
public final class ItemStack {
    public <T> T get(DataComponentType<? extends T> type) { return null; }
    public <T> T set(DataComponentType<? super T> type, T value) { return null; }
    public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot) { }
    public Item getItem() { return null; }
}
