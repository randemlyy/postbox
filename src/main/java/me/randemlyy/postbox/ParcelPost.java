package me.randemlyy.postbox;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ParcelPost {

    public enum Outcome { SENT, NO_MAILBOX, SELF, FULL, EMPTY_HAND}

    public record Result(Outcome outcome, String recipientName, int amount) {}

    private ParcelPost() {}

    public static Result send(ServerPlayer sender, InteractionHand hand, String recipientName, int amount) {
        String name = recipientName.trim();

        MailboxDirectory.Entry target = MailboxDirectory.of(sender.level().getServer()).byName(name).orElse(null);

        if (target == null) {
            return new Result(Outcome.NO_MAILBOX, name, 0);
        }

        if (target.ownerId().equals(sender.getUUID())) {
            return new Result(Outcome.SELF, target.ownerName(), 0);
        }

        ParcelData parcels = ParcelData.of(sender.level().getServer());

        if (parcels.forRecipient(target.ownerId()).size() >= ParcelData.MAX_PER_RECIPIENT) {
            return new Result(Outcome.FULL, target.ownerName(), 0);
        }

        ItemStack held = sender.getItemInHand(hand);
        if (held.isEmpty()) {
            return new Result(Outcome.EMPTY_HAND, target.ownerName(), 0);
        }

        int count = Math.max(1, Math.min(amount, held.getCount()));
        ItemStack contents = sender.hasInfiniteMaterials() ? held.copyWithCount(count) : held.split(count);

        parcels.entries().add(new Parcel(
                sender.getUUID(), sender.getScoreboardName(),
                target.ownerId(), target.ownerName(),
                contents, sender.level().getLevelData().getGameTime()));
        parcels.setDirty();

        ServerPlayer online = sender.level().getServer().getPlayerList().getPlayer(target.ownerId());
        if (online != null) {
            online.sendSystemMessage(Component.literal("A parcel from " + sender.getScoreboardName()
                    + " is waiting in your mailbox").withStyle(ChatFormatting.GOLD));
        }

        return new Result(Outcome.SENT, target.ownerName(), count);
    }
}
