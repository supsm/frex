/*
 * This file is part of FREX and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.vram.frex.pastel;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.base.renderer.context.render.ItemRenderContext;
import io.vram.frex.base.renderer.util.EncoderUtil;

public class PastelItemRenderContext extends ItemRenderContext {
	private static final Supplier<ThreadLocal<PastelItemRenderContext>> POOL_FACTORY = () -> ThreadLocal.withInitial(() -> {
		final PastelItemRenderContext result = new PastelItemRenderContext();
		return result;
	});

	private static ThreadLocal<PastelItemRenderContext> POOL = POOL_FACTORY.get();

	public static void reload() {
		POOL = POOL_FACTORY.get();
	}

	public static PastelItemRenderContext get() {
		return POOL.get();
	}

	protected MultiBufferSource vertexConsumers;

	public PastelItemRenderContext() {
		super();
	}

	@Override
	protected void encodeQuad() {
		final var mat = emitter.material();
		final VertexConsumer consumer;

		if (mat.foilOverlay() && inputContext.itemStack().is(ItemTags.COMPASSES)) {
			// C'mon Mojang...
			final var matrixStack = inputContext.matrixStack();
			matrixStack.push();

			if (inputContext.mode() == ItemDisplayContext.GUI) {
				matrixStack.modelMatrix().scale(0.5f);
			} else if (inputContext.mode().firstPerson()) {
				matrixStack.modelMatrix().scale(0.75F);
			}

			if (inputContext.drawTranslucencyToMainTarget() || !Minecraft.useShaderTransparency()) {
				consumer = ItemRenderer.getCompassFoilBufferDirect(vertexConsumers, Sheets.cutoutBlockSheet(), matrixStack.toVanilla().last());
			} else {
				final RenderType renderType = mat.cutout() == MaterialConstants.CUTOUT_NONE ? Sheets.solidBlockSheet() : Sheets.cutoutBlockSheet();
				consumer = ItemRenderer.getCompassFoilBuffer(vertexConsumers, renderType, matrixStack.toVanilla().last());
			}

			matrixStack.pop();
		} else if (mat.transparency() != MaterialConstants.TRANSPARENCY_NONE) {
			if (inputContext.drawTranslucencyToMainTarget() || !Minecraft.useShaderTransparency()) {
				consumer = ItemRenderer.getFoilBufferDirect(vertexConsumers, Sheets.translucentCullBlockSheet(), true, mat.foilOverlay());
			} else {
				consumer = ItemRenderer.getFoilBuffer(vertexConsumers, Sheets.translucentItemSheet(), true, mat.foilOverlay());
			}
		} else {
			final RenderType renderType = mat.cutout() == MaterialConstants.CUTOUT_NONE ? Sheets.solidBlockSheet() : Sheets.cutoutBlockSheet();
			consumer = ItemRenderer.getFoilBufferDirect(vertexConsumers, renderType, true, mat.foilOverlay());
		}

		EncoderUtil.encodeQuad(emitter, inputContext, consumer);
	}

	@Override
	protected void prepareEncoding(MultiBufferSource vertexConsumers) {
		this.vertexConsumers = vertexConsumers;
	}

	@Override
	protected void renderCustomModel(BlockEntityWithoutLevelRenderer builtInRenderer, MultiBufferSource vertexConsumers) {
		builtInRenderer.renderByItem(inputContext.itemStack(), inputContext.mode(), inputContext.matrixStack().toVanilla(), vertexConsumers, inputContext.lightmap(), inputContext.overlay());
	}
}
