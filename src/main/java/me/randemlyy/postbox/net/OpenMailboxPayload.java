package me.randemlyy.postbox.net;

import me.randemlyy.postbox.Postbox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.List;

public record OpenMailboxPayload(BlockPos pos, String ownerName, int totalCount, List<ParcelEntry> parcels)
        implements CustomPacketPayload {

    public static final int PREVIEW_COUNT = 6;

    public static final Type<OpenMailboxPayload> TYPE = new Type<>(Postbox.id("open_mailbox"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ParcelEntry>> PARCELS_CODEC =
            ByteBufCodecs.<RegistryFriendlyByteBuf, ParcelEntry>list(PREVIEW_COUNT)
                    .apply(ParcelEntry.STREAM_CODEC);

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMailboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenMailboxPayload::pos,
                    ByteBufCodecs.STRING_UTF8, OpenMailboxPayload::ownerName,
                    ByteBufCodecs.VAR_INT, OpenMailboxPayload::totalCount,
                    PARCELS_CODEC, OpenMailboxPayload::parcels,
                    OpenMailboxPayload::new);

    public OpenMailboxPayload {
        parcels = List.copyOf(parcels);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
