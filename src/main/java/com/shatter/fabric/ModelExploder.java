/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric;

import com.shatter.fabric.mixin.AnimalModelInvoker;
import com.shatter.fabric.mixin.CompositeEntityModelInvoker;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import net.minecraft.client.render.entity.model.CompositeEntityModel;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Equivalente ao "explode" do iChunUtil: separa o modelo (já na pose atual do mob) em um pedaço
 * por caixa (cuboid), guardando a transformação de cada uma.
 */
public final class ModelExploder {
	private ModelExploder() {
	}

	/** Uma caixa do modelo + a posição/rotação dela dentro do modelo. */
	public record Piece(ModelPart.Cuboid cuboid, Matrix4f position, Matrix3f normal) {
	}

	public static List<Piece> explode(Object model) {
		List<ModelPart> parts = new ArrayList<>();

		if (model instanceof SinglePartEntityModel<?> single) {
			parts.add(single.getPart());
		} else if (model instanceof AnimalModel<?>) {
			AnimalModelInvoker invoker = (AnimalModelInvoker) model;
			addAll(parts, invoker.shatter$getHeadParts());
			addAll(parts, invoker.shatter$getBodyParts());
		} else if (model instanceof CompositeEntityModel<?>) {
			addAll(parts, ((CompositeEntityModelInvoker) model).shatter$getParts());
		}

		if (parts.isEmpty()) {
			reflectParts(model, parts);
		}

		return capture(parts);
	}

	private static void addAll(List<ModelPart> out, Iterable<ModelPart> in) {
		if (in != null) {
			for (ModelPart part : in) {
				if (part != null) {
					out.add(part);
				}
			}
		}
	}

	/** Último recurso para modelos de mods: procura campos do tipo ModelPart (por tipo, não por nome). */
	private static void reflectParts(Object model, List<ModelPart> out) {
		Class<?> clazz = model.getClass();
		while (clazz != null && clazz != Object.class) {
			for (Field field : clazz.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				try {
					field.setAccessible(true);
					collect(field.get(model), out);
				} catch (Throwable ignored) {
					// campo inacessível: ignora
				}
			}
			clazz = clazz.getSuperclass();
		}
	}

	private static void collect(Object value, List<ModelPart> out) {
		if (value instanceof ModelPart part) {
			out.add(part);
		} else if (value instanceof ModelPart[] array) {
			for (ModelPart part : array) {
				if (part != null) {
					out.add(part);
				}
			}
		} else if (value instanceof Iterable<?> iterable) {
			for (Object o : iterable) {
				if (o instanceof ModelPart part) {
					out.add(part);
				}
			}
		} else if (value instanceof Map<?, ?> map) {
			for (Object o : map.values()) {
				if (o instanceof ModelPart part) {
					out.add(part);
				}
			}
		}
	}

	private static List<Piece> capture(List<ModelPart> parts) {
		List<List<Piece>> pieceLists = new ArrayList<>();
		List<Set<Object>> cuboidSets = new ArrayList<>();
		Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());

		for (ModelPart part : parts) {
			if (!seen.add(part) || !part.visible) {
				continue;
			}
			List<Piece> pieces = new ArrayList<>();
			Set<Object> cuboids = Collections.newSetFromMap(new IdentityHashMap<>());
			part.forEachCuboid(new MatrixStack(), (entry, path, index, cuboid) -> {
				pieces.add(new Piece(cuboid, new Matrix4f(entry.getPositionMatrix()), new Matrix3f(entry.getNormalMatrix())));
				cuboids.add(cuboid);
			});
			if (!pieces.isEmpty()) {
				pieceLists.add(pieces);
				cuboidSets.add(cuboids);
			}
		}

		// Descarta partes que já estão dentro de outra (filhas), para não duplicar pedaços.
		List<Piece> result = new ArrayList<>();
		for (int i = 0; i < pieceLists.size(); i++) {
			boolean nested = false;
			for (int j = 0; j < pieceLists.size() && !nested; j++) {
				if (i == j) {
					continue;
				}
				Set<Object> a = cuboidSets.get(i);
				Set<Object> b = cuboidSets.get(j);
				if (b.containsAll(a) && (b.size() > a.size() || j < i)) {
					nested = true;
				}
			}
			if (!nested) {
				result.addAll(pieceLists.get(i));
			}
		}
		return result;
	}
}
