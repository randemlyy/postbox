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
        ItemStack stack = parcel.stack().copy();

        if (!player.getInventory().add(stack)){
            player.spawnAtLocation(player.level(), stack);
            player.sendSystemMessage(Component.literal("Your inventory is full - the parcel is at your feet")
                    .withStyle(ChatFormatting.YELLOW));
        }

        player.sendSystemMessage(Component.literal("Took " + parcel.stack().getHoverName().getString() + " x" + parcel.stack().getCount() + " from " + parcel.senderName()));

        player.level().playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.8f, 1.2f);
    }
}
