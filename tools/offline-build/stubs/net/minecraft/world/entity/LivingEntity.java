package net.minecraft.world.entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
public abstract class LivingEntity extends Entity {
    public boolean addEffect(MobEffectInstance instance) { return false; }
    public static EquipmentSlot getSlotForHand(InteractionHand hand) { return null; }
}
