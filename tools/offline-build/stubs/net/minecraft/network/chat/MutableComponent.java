package net.minecraft.network.chat;
import net.minecraft.ChatFormatting;
public class MutableComponent implements Component {
    public MutableComponent withStyle(ChatFormatting formatting) { return this; }
}
