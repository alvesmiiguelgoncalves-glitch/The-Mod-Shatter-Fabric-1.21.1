/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric;

import com.shatter.fabric.mixin.LivingEntityRendererInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Random;

/**
 * Um mob despedaçado. Equivale à EntityShattered + ModelShattered + RenderShattered do original,
 * mas é um efeito puramente visual no cliente (não registra entidade nenhuma).
 */
public final class ShatterEffect {
	/** Ticks de vida do efeito (o original remove a entidade quando progress > 105). */
	private static final int LIFETIME = 105;

	private static int idCounter = 1;

	private final LivingEntity acquired;
	private final LivingEntityRenderer<?, ?> renderer;
	private final Identifier texture;
	private final List<ModelExploder.Piece> pieces;
	private final float renderYaw;
	private final int id;
	private final Random rand = new Random();

	private double x, y, z;
	private double prevX, prevY, prevZ;
	private double vx, vy, vz;
	private int progress;

	private ShatterEffect(LivingEntity acquired, LivingEntityRenderer<?, ?> renderer, Identifier texture, List<ModelExploder.Piece> pieces) {
		this.acquired = acquired;
		this.renderer = renderer;
		this.texture = texture;
		this.pieces = pieces;
		this.renderYaw = acquired.bodyYaw;
		this.id = idCounter++;

		this.x = this.prevX = acquired.getX();
		this.y = this.prevY = acquired.getY();
		this.z = this.prevZ = acquired.getZ();

		Vec3d motion = acquired.getVelocity();
		this.vx = motion.x * 0.4D;
		this.vy = motion.y * 0.15D;
		this.vz = motion.z * 0.4D;
	}

	/** Cria o efeito a partir de um mob morto; devolve null se não for possível (o mob fica como está). */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static ShatterEffect create(LivingEntity entity) {
		MinecraftClient mc = MinecraftClient.getInstance();
		EntityRenderer<?> raw = mc.getEntityRenderDispatcher().getRenderer(entity);
		if (!(raw instanceof LivingEntityRenderer<?, ?> livingRenderer)) {
			return null;
		}

		try {
			// Renderiza o mob uma vez bem longe da câmera só para o modelo assumir a pose atual
			// (é o que o original faz, em y = -500).
			MatrixStack dummy = new MatrixStack();
			dummy.translate(0.0F, -10000.0F, 0.0F);
			EntityRenderer renderer = raw;
			renderer.render(entity, entity.headYaw, 1.0F, dummy, mc.getBufferBuilders().getEntityVertexConsumers(), 15728880);

			Identifier texture = renderer.getTexture(entity);
			List<ModelExploder.Piece> pieces = ModelExploder.explode(livingRenderer.getModel());
			if (pieces.isEmpty()) {
				return null;
			}
			return new ShatterEffect(entity, livingRenderer, texture, pieces);
		} catch (Throwable t) {
			ShatterClient.LOGGER.warn("Não foi possível despedaçar {}", entity.getType(), t);
			return null;
		}
	}

	/** Um tick de física. Devolve false quando o efeito terminou. */
	public boolean tick() {
		prevX = x;
		prevY = y;
		prevZ = z;

		progress++;
		if (progress > LIFETIME) {
			return false;
		}

		x += vx;
		y += vy * 0.2D;
		z += vz;
		vx *= 0.97D;
		vy *= 0.97D;
		vz *= 0.97D;
		return true;
	}

	public void render(ClientWorld world, MatrixStack stack, VertexConsumerProvider consumers, Vec3d camera, float tickDelta) {
		MinecraftClient mc = MinecraftClient.getInstance();
		// Em primeira pessoa você não vê os pedaços do seu próprio corpo.
		if (acquired == mc.player && mc.options.getPerspective().isFirstPerson()) {
			return;
		}

		// Progresso (0 -> 1) e transparência (1 -> 0), como no original.
		float p = (float) MathHelper.clamp(Math.pow((progress + tickDelta) / 100.0D, 0.99D), 0.0D, 1.0D);
		float alpha = 1.0F - p;
		if (alpha <= 0.0F) {
			return;
		}
		int color = ((int) (alpha * 255.0F) & 0xFF) << 24 | 0xFFFFFF;

		double px = MathHelper.lerp(tickDelta, prevX, x);
		double py = MathHelper.lerp(tickDelta, prevY, y);
		double pz = MathHelper.lerp(tickDelta, prevZ, z);
		int light = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(px, py + 1.0D, pz));

		VertexConsumer consumer = consumers.getBuffer(RenderLayer.getEntityTranslucent(texture));

		stack.push();
		stack.translate(px - camera.x, py - camera.y, pz - camera.z);
		stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
		stack.scale(-1.0F, -1.0F, 1.0F);
		try {
			((LivingEntityRendererInvoker) renderer).shatter$scale(acquired, stack, tickDelta);
		} catch (Throwable ignored) {
			// sem a escala do mob (ex.: slimes), os pedaços saem com o tamanho padrão
		}
		stack.translate(0.0F, -0.75F, 0.0F);

		stack.push();
		stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(renderYaw));

		for (int i = 0; i < pieces.size(); i++) {
			ModelExploder.Piece piece = pieces.get(i);

			stack.push();

			// Cada pedaço tem uma "semente" própria, então o movimento dele é sempre o mesmo a cada frame.
			rand.setSeed(rand.nextInt() * id * i * 1000);

			float a = rand.nextFloat() * (rand.nextFloat() > 0.5F ? -1 : 1) * p;
			double ty = rand.nextDouble() * p * (vy + (rand.nextDouble() - 1.0D));
			float b = rand.nextFloat() * (rand.nextFloat() > 0.5F ? -1 : 1) * p;
			stack.translate(a * vz * 5.0D, ty, b * vx * 5.0D);

			float angle = 180.0F * rand.nextFloat() * p;
			float ax = rand.nextFloat() * (rand.nextFloat() > 0.5F ? -1 : 1) * p;
			float ay = rand.nextFloat() * (rand.nextFloat() > 0.5F ? -1 : 1) * p;
			float az = rand.nextFloat() * (rand.nextFloat() > 0.5F ? -1 : 1) * p;
			stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle * ax));
			stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle * ay));
			stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle * az));

			// Sobe metade da altura do mob de novo: o original gira os pedaços em volta do centro do corpo.
			stack.translate(0.0F, -0.75F, 0.0F);

			MatrixStack.Entry entry = stack.peek();
			entry.getPositionMatrix().mul(piece.position());
			entry.getNormalMatrix().mul(piece.normal());
			piece.cuboid().renderCuboid(entry, consumer, light, OverlayTexture.DEFAULT_UV, color);

			stack.pop();
		}

		stack.pop();
		stack.pop();
	}
}
