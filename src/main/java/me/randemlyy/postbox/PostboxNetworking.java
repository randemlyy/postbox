package me.randemlyy.postbox;

import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import me.randemlyy.postbox.net.ParcelEntry;
import me.randemlyy.postbox.net.ParcelEntry;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.ArrayList;

public final class PostboxNetworking {

    private PostboxNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(OpenMailboxPayload.TYPE, OpenMailboxPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MailboxRequestPayload.TYPE, MailboxRequestPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MailboxRequestPayload.TYPE, (payload, context) -> {
            Postbox.LOGGER.info("Mailbox refresh requested at {}", payload.pos());
            sendMailbox(context.player(), payload.pos());
        });
    }

    public static void sendMailbox(ServerPlayer player, BlockPos pos) {
        if (!ServerPlayNetworking.canSend(player, OpenMailboxPayload.TYPE)) {
            return;
        }
        if (!(player.level().getBlockEntity(pos) instanceof MailboxBlockEntity mailbox)) {
            return;
        }
        if (mailbox.ownerId() == null || !mailbox.ownerId().equals(player.getUUID())) {
            return;
        }

        List<Parcel> waiting = ParcelData.of(player.level().getServer()).forRecipient(player.getUUID());

        List<ParcelEntry> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(waiting.size(), OpenMailboxPayload.PREVIEW_COUNT); i++) {
            Parcel parcel = waiting.get(i);
            preview.add(new ParcelEntry(parcel.senderName(), parcel.stack()));
        }

        ServerPlayNetworking.send(player,
                new OpenMailboxPayload(pos, mailbox.ownerName(), waiting.size(), preview));
    }
}