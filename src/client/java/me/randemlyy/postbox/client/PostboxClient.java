package me.randemlyy.postbox.client;

import me.randemlyy.postbox.net.OpenMailboxPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class PostboxClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(OpenMailboxPayload.TYPE, (payload, context) ->
				Minecraft.getInstance().setScreenAndShow(new MailboxScreen(payload)));
	}
}