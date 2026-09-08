package net.minecraft.world.entity.player;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
public abstract class Player extends LivingEntity {
    public ItemStack getItemInHand(InteractionHand hand) { return null; }
    public boolean isSecondaryUseActive() { return false; }
    public void displayClientMessage(Component message, boolean actionBar) { }
    public ItemCooldowns getCooldowns() { return null; }
    public void awardStat(Stat<?> stat) { }
    public Abilities getAbilities() { return null; }
}
