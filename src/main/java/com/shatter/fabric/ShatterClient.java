/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShatterClient implements ClientModInitializer {
	public static final String MOD_ID = "shatter";
	public static final Logger LOGGER = LoggerFactory.getLogger("Shatter");

	public static ShatterConfig config = new ShatterConfig();

	@Override
	public void onInitializeClient() {
		config = ShatterConfig.load();

		ClientTickEvents.END_CLIENT_TICK.register(ShatterManager::onClientTick);
		WorldRenderEvents.AFTER_ENTITIES.register(ShatterManager::onWorldRender);
	}
}
