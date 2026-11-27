package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.List;
import io.netty.buffer.ByteBuf;

public record OpenMailboxPayload(BlockPos pos, String ownerName, int totalCount, int page, List<ParcelEntry> parcels, List<String> mailboxes, boolean initial)
        implements CustomPacketPayload {

    public static final int PAGE_SIZE = 6;
    public static final int MAX_MAILBOXES = 256;

    public static final Type<OpenMailboxPayload> TYPE = new Type<>(Postbox.id("open_mailbox"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ParcelEntry>> PARCELS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, ParcelEntry>list(PAGE_SIZE)
                    .apply(ParcelEntry.STREAM_CODEC);

    private static final StreamCodec<ByteBuf, List<String>> MAILBOXES_CODEC =
            ByteBufCodecs.<ByteBuf, String>list(MAX_MAILBOXES)
                    .apply(ByteBufCodecs.stringUtf8(16));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenMailboxPayload::pos,
                    ByteBufCodecs.STRING_UTF8, OpenMailboxPayload::ownerName,
                    ByteBufCodecs.VAR_INT, OpenMailboxPayload::totalCount,
                    ByteBufCodecs.VAR_INT, OpenMailboxPayload::page,
                    PARCELS_CODEC, OpenMailboxPayload::parcels,
                    MAILBOXES_CODEC, OpenMailboxPayload::mailboxes,
                    ByteBufCodecs.BOOL, OpenMailboxPayload::initial,
                    OpenMailboxPayload::new);

    public OpenMailboxPayload {
        parcels = List.copyOf(parcels);
        mailboxes = List.copyOf(mailboxes);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
