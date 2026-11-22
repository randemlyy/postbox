package me.randemlyy.postbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.item.ItemStack;

public final class Courier {
    private static final long FLIGHT_TICKS = 100L; // 5 seconds
    private static final long EXPIRY_TICKS = 20L * 24_000L; // 20 ig days

    private static final long RETURN_DUE_TICKS = 100L;
    private static final long RETURN_GIVE_UP_TICKS = 24_000;

    private static final List<Return> RETURNS = new ArrayList<>();

    private record Return(UUID senderID, ParrotIdentity identity, long dueAt, long giveUpAt) {}

    private static final List<Delivery> IN_FLIGHT = new ArrayList<>();

    private record Delivery(UUID senderID, String senderName, UUID recipientID, String recipientName, ItemStack letter, long arriveAt, long expireAt) {}

    private Courier() {}

    public static void init()                                                                                                              {
        ServerTickEvents.END_SERVER_TICK.register(Courier::tick);

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            IN_FLIGHT.clear();
            RETURNS.clear();
            ParrotTheatre.reset();
        });
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getLevelData().getGameTime();
    }

    public static void send(ServerPlayer sender, ServerPlayer recipient, ItemStack stack, Parrot parrot) {
        MinecraftServer server = sender.level().getServer();

        long now = now(server);

        ItemStack letter = stack.copy();

        IN_FLIGHT.add(new Delivery(
                sender.getUUID(), sender.getScoreboardName(),
                recipient.getUUID(), recipient.getScoreboardName(),
                letter, now + FLIGHT_TICKS, now + EXPIRY_TICKS
        ));

        RETURNS.add(new Return(
                sender.getUUID(),
                ParrotIdentity.of(parrot),
                now + RETURN_DUE_TICKS,
                now + RETURN_GIVE_UP_TICKS
        ));

        if (!sender.hasInfiniteMaterials()) {
            stack.shrink(1);
        }

        sender.sendSystemMessage(Component.literal("Your parrot has taken flight for " + recipient.getScoreboardName())
                .withStyle(ChatFormatting.GOLD));
        recipient.sendSystemMessage(Component.literal("A parrot is on its way with a letter from " + sender.getScoreboardName())
                .withStyle(ChatFormatting.GOLD));

        sender.level().playSound(null, sender.blockPosition(), SoundEvents.PARROT_FLY, SoundSource.NEUTRAL, 1.0f, 1.2f);

        ParrotTheatre.depart(parrot, sender);
    }

    private static void tick(MinecraftServer server) {
        long now = now(server);
        Iterator<Delivery> it = IN_FLIGHT.iterator();

        while (it.hasNext()) {
            Delivery d = it.next();

            if (now < d.arriveAt()) {
                continue;
            }

            ServerPlayer recipient = server.getPlayerList().getPlayer(d.recipientID());

            if (recipient != null) {
                deliver(recipient, d);
                it.remove();
                continue;
            }

            if (now >= d.expireAt()) {
                expire(server, d);
                it.remove();
            }

        }

        Iterator<Return> returns = RETURNS.iterator();

        while (returns.hasNext()) {
            Return r = returns.next();

            if (now < r.dueAt()) {
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(r.senderID());

            if (owner == null) {
                if (now >= r.giveUpAt()) {
                    returns.remove();
                }
                continue;
            }

            ParrotTheatre.returnTo(owner, r.identity());
            owner.sendSystemMessage(Component.literal("Your parrot is back").withStyle(ChatFormatting.GRAY));
            returns.remove();
        }

        ParrotTheatre.tick(server, now);

    }

    private static void deliver(ServerPlayer recipient, Delivery d) {
        ItemStack letter = d.letter.copy();

        if (!recipient.getInventory().add(letter)) {
            recipient.spawnAtLocation(recipient.level(), letter);
            recipient.sendSystemMessage(Component.literal("Your hands are full - the letter is dropped")
                    .withStyle(ChatFormatting.YELLOW));
        }

        recipient.sendSystemMessage(Component.literal("A letter from " + d.senderName() + " arrives."));
        recipient.level().playSound(null, recipient.blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.NEUTRAL, 1.0f, 1.0f);

        ParrotTheatre.arrive(recipient);
    }

    private static void expire(MinecraftServer server, Delivery d) {
        ServerPlayer sender = server.getPlayerList().getPlayer(d.senderID());

        if (sender == null) {
            Postbox.LOGGER.info("Dropped an expired letter from {} to {}: sender offline",
                    d.senderName(), d.recipientName());
            return;
        }

        ItemStack letter = d.letter().copy();

        if (!sender.getInventory().add(letter)) {
            sender.spawnAtLocation(sender.level(), letter);
        }

        sender.sendSystemMessage(Component.literal("Your letter to " + d.recipientName()
                        + " came back undelivered")
                .withStyle(ChatFormatting.GRAY));
    }
}
