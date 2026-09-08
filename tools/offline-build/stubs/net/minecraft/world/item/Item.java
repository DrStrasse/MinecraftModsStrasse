package net.minecraft.world.item;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
public class Item implements ItemLike {
    public Item(Item.Properties properties) { }
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) { return null; }
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) { }
    public boolean isFoil(ItemStack stack) { return false; }
    public ItemStack getDefaultInstance() { return null; }
    public Item asItem() { return this; }

    public interface TooltipContext { }

    public static class Properties {
        public Item.Properties stacksTo(int maxStackSize) { return this; }
        public Item.Properties durability(int durability) { return this; }
        public Item.Properties fireResistant() { return this; }
        public <T> Item.Properties component(DataComponentType<T> type, T value) { return this; }
    }
}
