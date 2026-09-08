package net.minecraft.network.codec;
import io.netty.buffer.ByteBuf;
public interface ByteBufCodecs {
    StreamCodec<ByteBuf, Integer> VAR_INT = null;
}
