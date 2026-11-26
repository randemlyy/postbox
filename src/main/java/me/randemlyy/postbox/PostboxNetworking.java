package me.randemlyy.postbox;

import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import me.randemlyy.postbox.net.ParcelEntry;
import me.randemlyy.postbox.net.TakeParcelPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class PostboxNetworking {

    private PostboxNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(OpenMailboxPayload.TYPE, OpenMailboxPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MailboxRequestPayload.TYPE, MailboxRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TakeParcelPayload.TYPE, TakeParcelPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MailboxRequestPayload.TYPE, (payload, context) -> {
            Postbox.LOGGER.info("Mailbox refresh requested at {}", payload.pos());
            sendMailbox(context.player(), payload.pos());
        });

        ServerPlayNetworking.registerGlobalReceiver(TakeParcelPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOwnMailbox(player, payload.pos())) {
                return;
            }

            Postbox.LOGGER.info("Take parcel requested: index {} at {}", payload.index(), payload.pos());

            ParcelData.of(player.level().getServer())
                    .takeAt(player.getUUID(), payload.index()).ifPresent(parcel -> ParcelDelivery.deliver(player, parcel, payload.pos()));

            sendMailbox(player, payload.pos());
        });
    }

    public static void sendMailbox(ServerPlayer player, BlockPos pos) {
        if (!ServerPlayNetworking.canSend(player, OpenMailboxPayload.TYPE)) {
            return;
        }
        if (!isOwnMailbox(player, pos)) {
            return;
        }

        MailboxBlockEntity mailbox = (MailboxBlockEntity) player.level().getBlockEntity(pos);

        List<Parcel> waiting = ParcelData.of(player.level().getServer())
                .forRecipient(player.getUUID());

        List<ParcelEntry> preview = new ArrayList<>();
        for (int i = 0; i < Math.min(waiting.size(), OpenMailboxPayload.PREVIEW_COUNT); i++) {
            Parcel parcel = waiting.get(i);
            preview.add(new ParcelEntry(parcel.senderName(), parcel.stack()));
        }

        ServerPlayNetworking.send(player,
                new OpenMailboxPayload(pos, mailbox.ownerName(), waiting.size(), preview));
    }

    private static boolean isOwnMailbox(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof MailboxBlockEntity mailbox)) {
            return false;
        }
        return mailbox.ownerId() != null && mailbox.ownerId().equals(player.getUUID());
    }
}