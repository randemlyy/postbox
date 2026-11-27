package me.randemlyy.postbox;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class ParcelDelivery {

    private ParcelDelivery() {}

    public static void deliver(ServerPlayer player, Parcel parcel, BlockPos pos) {
        deliver(player, parcel, pos, false);
    }

    public static void deliver(ServerPlayer player, Parcel parcel, BlockPos pos, boolean quiet) {
        ItemStack stack = parcel.stack().copy();

        if (!player.getInventory().add(stack)) {
            player.spawnAtLocation(player.level(), stack);
            if (!quiet) {
                player.sendSystemMessage(Component.translatable("postbox.parcel.inventory_full")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        if (quiet) {
            return;
        }

        player.sendSystemMessage(Component.translatable("postbox.parcel.took",
                parcel.stack().getHoverName(), parcel.stack().getCount(), parcel.senderName()));
        player.level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.2f);
    }
}
