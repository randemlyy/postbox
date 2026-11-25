package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record MailboxRequestPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<MailboxRequestPayload> TYPE = new Type<>(Postbox.id("mailbox_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MailboxRequestPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, MailboxRequestPayload::pos, MailboxRequestPayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
