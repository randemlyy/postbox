package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record TakeAllParcelsPayload(BlockPos pos, int page) implements CustomPacketPayload {

    public static final Type<TakeAllParcelsPayload> TYPE = new Type<>(Postbox.id("take_all_parcels"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeAllParcelsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TakeAllParcelsPayload::pos,
                    ByteBufCodecs.VAR_INT, TakeAllParcelsPayload::page,
                    TakeAllParcelsPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}