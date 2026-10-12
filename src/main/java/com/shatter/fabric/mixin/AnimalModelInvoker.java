/*
 * Shatter (Fabric port) - porte para Fabric 1.21.1 do mod "Shatter" de iChun.
 * Mod original: https://ichun.me/mods/shatter/  (c) iChun, GNU LGPL v3.
 * Este porte e distribuido sob a GNU Lesser General Public License v3.0.
 */

package com.shatter.fabric.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.AnimalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Dá acesso às partes do modelo (cabeça/corpo) de modelos baseados em AnimalModel. */
@Mixin(AnimalModel.class)
public interface AnimalModelInvoker {
	@Invoker("getHeadParts")
	Iterable<ModelPart> shatter$getHeadParts();

	@Invoker("getBodyParts")
	Iterable<ModelPart> shatter$getBodyParts();
}
