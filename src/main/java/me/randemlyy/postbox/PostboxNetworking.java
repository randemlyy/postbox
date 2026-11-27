package me.randemlyy.postbox;

import me.randemlyy.postbox.net.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class PostboxNetworking {

    private static final double MAX_REACH_SQR = 64.0;

    private PostboxNetworking() {}

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(OpenMailboxPayload.TYPE, OpenMailboxPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MailboxNoticePayload.TYPE, MailboxNoticePayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(MailboxRequestPayload.TYPE, MailboxRequestPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TakeParcelPayload.TYPE, TakeParcelPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SendParcelPayload.TYPE, SendParcelPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SealLetterResultPayload.TYPE, SealLetterResultPayload.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SealLetterPayload.TYPE, SealLetterPayload.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(MailboxRequestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOwnMailbox(player, payload.pos()) || !canReach(player, payload.pos())) {
                return;
            }
            sendMailbox(player, payload.pos());
        });

        ServerPlayNetworking.registerGlobalReceiver(TakeParcelPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (!isOwnMailbox(player, payload.pos()) || !canReach(player, payload.pos())) {
                return;
            }

            ParcelData.of(player.level().getServer())
                    .takeAt(player.getUUID(), payload.index())
                    .ifPresent(parcel -> ParcelDelivery.deliver(player, parcel, payload.pos()));

            sendMailbox(player, payload.pos());
        });

        ServerPlayNetworking.registerGlobalReceiver(SealLetterPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            InteractionHand hand = payload.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof LetterItem) || stack.hasNonDefault(DataComponents.LORE)) {
                sealResult(player, false, "postbox.letter.already_sealed", "");
                return;
            }

            String to = payload.to().trim();
            String message = payload.message().trim();

            if (message.isEmpty()) {
                sealResult(player, false, "postbox.letter.empty", "");
                return;
            }
            if (message.length() > LetterWriter.MAX_MESSAGE_LENGTH) {
                sealResult(player, false, "postbox.letter.too_long",
                        String.valueOf(LetterWriter.MAX_MESSAGE_LENGTH));
                return;
            }

            ServerPlayer target = player.level().getServer().getPlayerList().getPlayer(to);
            if (target == null) {
                sealResult(player, false, "postbox.letter.not_online", to);
                return;
            }
            if (target.getUUID().equals(player.getUUID())) {
                sealResult(player, false, "postbox.letter.self", "");
                return;
            }

            LetterWriter.seal(player, stack, target.getScoreboardName(), message);
            sealResult(player, true, "postbox.letter.sealed", target.getScoreboardName());
        });
    }

    private static void sealResult(ServerPlayer player, boolean success, String key, String arg) {
        ServerPlayNetworking.send(player, new SealLetterResultPayload(success, key, arg));
    }

    public static void sendMailbox(ServerPlayer player, BlockPos pos) {
        sendMailbox(player, pos, false);
    }

    public static void sendMailbox(ServerPlayer player, BlockPos pos, boolean initial) {
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

        List<String> mailboxes = new ArrayList<>();
        String self = player.getScoreboardName();
        for (String name : MailboxDirectory.of(player.level().getServer()).names()) {
            if (name.equalsIgnoreCase(self)) {
                continue;
            }
            mailboxes.add(name);
            if (mailboxes.size() >= OpenMailboxPayload.MAX_MAILBOXES) {
                break;
            }
        }

        ServerPlayNetworking.send(player, new OpenMailboxPayload(
                pos, mailbox.ownerName(), waiting.size(), preview, mailboxes, initial));
    }

    private static InteractionHand heldHand(ServerPlayer player) {
        return player.getMainHandItem().isEmpty() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static void notice(ServerPlayer player, String key, String arg, boolean error) {
        if (ServerPlayNetworking.canSend(player, MailboxNoticePayload.TYPE)) {
            ServerPlayNetworking.send(player, new MailboxNoticePayload(key, arg, error));
        }
    }

    private static boolean canReach(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_REACH_SQR;
    }

    private static boolean isOwnMailbox(ServerPlayer player, BlockPos pos) {
        if (!(player.level().getBlockEntity(pos) instanceof MailboxBlockEntity mailbox)) {
            return false;
        }
        return mailbox.ownerId() != null && mailbox.ownerId().equals(player.getUUID());
    }
}