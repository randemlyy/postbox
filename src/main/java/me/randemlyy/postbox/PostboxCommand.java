package me.randemlyy.postbox;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PostboxCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("postbox")
                    .executes(context -> {
                        context.getSource().sendSuccess(
                                () -> Component.translatable("postbox.command.help"), false);
                        return Command.SINGLE_SUCCESS;
                    })
                    .then(Commands.literal("cancel").executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean hadLetter = LetterWriter.cancel(player);
                        boolean hadParcel = ParcelWriter.cancel(player);

                        context.getSource().sendSuccess(() -> Component.translatable(
                                hadLetter || hadParcel
                                        ? "postbox.command.cancel.done"
                                        : "postbox.command.cancel.none"), false);
                        return Command.SINGLE_SUCCESS;
                    }))
            );
        });
    }
}
