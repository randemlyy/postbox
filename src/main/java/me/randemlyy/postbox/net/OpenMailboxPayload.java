package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record OpenMailboxPayload(BlockPos pos, String ownerName, int parcelCount) implements CustomPacketPayload {

    public static final Type<OpenMailboxPayload> TYPE = new Type<>(Postbox.id("open_mailbox"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenMailboxPayload::pos,
                    ByteBufCodecs.STRING_UTF8, OpenMailboxPayload::ownerName,
                    ByteBufCodecs.VAR_INT, OpenMailboxPayload::parcelCount,
                    OpenMailboxPayload::new
            );

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
