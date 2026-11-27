package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public record MailboxNoticePayload(String key, String arg, boolean error) implements CustomPacketPayload {

    public static final Type<MailboxNoticePayload> TYPE = new Type<>(Postbox.id("mailbox_notice"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MailboxNoticePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.stringUtf8(64), MailboxNoticePayload::key,
                    ByteBufCodecs.stringUtf8(64), MailboxNoticePayload::arg,
                    ByteBufCodecs.BOOL, MailboxNoticePayload::error,
                    MailboxNoticePayload::new);

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}