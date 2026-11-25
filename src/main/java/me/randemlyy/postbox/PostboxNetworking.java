package me.randemlyy.postbox;

import me.randemlyy.postbox.net.MailboxRequestPayload;
import me.randemlyy.postbox.net.OpenMailboxPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

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

        int count = ParcelData.of(player.level().getServer())
                .forRecipient(player.getUUID()).size();

        ServerPlayNetworking.send(player, new OpenMailboxPayload(pos, mailbox.ownerName(), count));
    }
}