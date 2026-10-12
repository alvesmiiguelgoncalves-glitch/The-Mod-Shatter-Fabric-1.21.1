/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuração do cliente, salva em config/shatter.json (mesmas opções do mod original). */
public class ShatterConfig {
	/** Despedaçar bosses (desligado por padrão: modelos únicos podem renderizar de forma estranha). */
	public boolean enableBossShatter = false;
	/** Despedaçar jogadores. */
	public boolean enablePlayerShatter = true;
	/** Despedaçar filhotes (desligado por padrão, como no original). */
	public boolean enableChildShatter = false;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("shatter.json");
	}

	public static ShatterConfig load() {
		ShatterConfig config = new ShatterConfig();
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				ShatterConfig loaded = GSON.fromJson(reader, ShatterConfig.class);
				if (loaded != null) {
					config = loaded;
				}
			} catch (Exception e) {
				ShatterClient.LOGGER.warn("Não foi possível ler {}, usando os valores padrão", path, e);
			}
		}
		config.save();
		return config;
	}

	public void save() {
		try {
			Files.writeString(path(), GSON.toJson(this));
		} catch (IOException e) {
			ShatterClient.LOGGER.warn("Não foi possível salvar {}", path(), e);
		}
	}
}
