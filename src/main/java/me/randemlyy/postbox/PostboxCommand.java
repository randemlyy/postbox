package me.randemlyy.postbox;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PostboxCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("postbox").executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal("postbox time baby"), false);
                return Command.SINGLE_SUCCESS;
            }));
        });
    }
}

