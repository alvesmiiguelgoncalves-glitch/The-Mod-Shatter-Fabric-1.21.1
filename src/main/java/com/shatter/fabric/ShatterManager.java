/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.GiantEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.MagmaCubeEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Detecta mobs mortos no cliente, troca eles por um ShatterEffect e desenha os efeitos ativos. */
public final class ShatterManager {
	private ShatterManager() {
	}

	/** Ticks de espera entre a morte e o despedaçamento (o original usa 2). */
	private static final int DELAY_TICKS = 2;

	private static final Map<LivingEntity, Integer> TIMEOUTS = new IdentityHashMap<>();
	/** Mobs mortos que não devem ser despedaçados (já tratados, bosses desligados etc.). Comparados por identidade. */
	private static final Set<LivingEntity> IGNORED = Collections.newSetFromMap(new IdentityHashMap<>());
	private static final List<ShatterEffect> EFFECTS = new ArrayList<>();
	private static ClientWorld lastWorld;

	public static void onClientTick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world != lastWorld) {
			TIMEOUTS.clear();
			IGNORED.clear();
			EFFECTS.clear();
			lastWorld = world;
		}
		if (world == null || client.isPaused()) {
			return;
		}

		EFFECTS.removeIf(effect -> !effect.tick());
		IGNORED.removeIf(Entity::isRemoved);

		// Mobs que já morreram: seguram a animação de morte vanilla e viram efeito quando o tempo acaba.
		Iterator<Map.Entry<LivingEntity, Integer>> iterator = TIMEOUTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<LivingEntity, Integer> entry = iterator.next();
			LivingEntity entity = entry.getKey();
			int remaining = entry.getValue() - 1;
			entry.setValue(remaining);

			// Impede o mob de tombar / ficar vermelho enquanto espera.
			entity.hurtTime = 0;
			entity.deathTime = 0;

			if (remaining > 0) {
				continue;
			}
			if (entity.getWorld() == world && !entity.isRemoved()) {
				ShatterEffect effect = ShatterEffect.create(entity);
				// Em qualquer caso, não trata esse mob de novo (o jogador local continua "morto" no mundo).
				IGNORED.add(entity);
				if (effect != null) {
					EFFECTS.add(effect);
					if (entity == client.player) {
						// Não remove o jogador local do mundo; só deixa invisível até renascer.
						entity.setInvisible(true);
					} else {
						entity.discard();
					}
				}
			}
			iterator.remove();
		}

		// Procura mobs que acabaram de morrer.
		for (Entity entity : world.getEntities()) {
			if (!(entity instanceof LivingEntity living) || !living.isDead() || living.isRemoved()) {
				continue;
			}
			if (TIMEOUTS.containsKey(living) || IGNORED.contains(living)) {
				continue;
			}
			if (shouldShatter(living)) {
				TIMEOUTS.put(living, DELAY_TICKS);
			} else {
				IGNORED.add(living);
			}
		}
	}

	private static boolean shouldShatter(LivingEntity entity) {
		ShatterConfig config = ShatterClient.config;
		if (entity instanceof PlayerEntity && !config.enablePlayerShatter) {
			return false;
		}
		if (!config.enableBossShatter && isBoss(entity)) {
			return false;
		}
		// Evita o efeito nos mobs que podem causar desconforto/fobias e nos seus equivalentes.
		if (isExcludedMob(entity)) {
			return false;
		}
		return config.enableChildShatter || !entity.isBaby();
	}

	private static boolean isBoss(LivingEntity entity) {
		return entity instanceof WitherEntity || entity instanceof EnderDragonEntity;
	}

	private static boolean isExcludedMob(LivingEntity entity) {
		return entity instanceof SpiderEntity
				|| entity instanceof CaveSpiderEntity
				|| entity instanceof RavagerEntity
				|| entity instanceof GuardianEntity
				|| entity instanceof ElderGuardianEntity
				|| entity instanceof SlimeEntity
				|| entity instanceof MagmaCubeEntity
				|| entity instanceof GiantEntity;
	}

	public static void onWorldRender(WorldRenderContext context) {
		if (EFFECTS.isEmpty()) {
			return;
		}
		VertexConsumerProvider consumers = context.consumers();
		MatrixStack stack = context.matrixStack();
		if (consumers == null || stack == null) {
			return;
		}

		Vec3d camera = context.camera().getPos();
		float tickDelta = context.tickCounter().getTickDelta(false);
		for (ShatterEffect effect : EFFECTS) {
			effect.render(context.world(), stack, consumers, camera, tickDelta);
		}
	}
}
