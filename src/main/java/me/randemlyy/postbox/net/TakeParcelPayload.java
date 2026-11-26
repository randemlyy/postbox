package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record TakeParcelPayload(BlockPos pos, int index) implements CustomPacketPayload {

    public static final Type<TakeParcelPayload> TYPE = new Type<>(Postbox.id("take_parcel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeParcelPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TakeParcelPayload::pos,
                    ByteBufCodecs.VAR_INT, TakeParcelPayload::index,
                    TakeParcelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}