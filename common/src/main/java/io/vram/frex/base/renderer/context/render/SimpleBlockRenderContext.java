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

package io.vram.frex.base.renderer.context.render;

import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.BlockModel;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Context for non-terrain block rendering.
 */
public abstract class SimpleBlockRenderContext extends BlockRenderContext<BlockAndTintGetter> {
	@Nullable protected VertexConsumer defaultConsumer;

	public void render(ModelBlockRenderer vanillaRenderer, BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, com.mojang.blaze3d.vertex.PoseStack poseStack, VertexConsumer buffer, boolean checkSides, long seed, int overlay) {
		defaultConsumer = buffer;
		inputContext.prepareForWorld(blockView, checkSides, MatrixStack.fromVanilla(poseStack));
		prepareForBlock(model, state, pos, seed, overlay);
		((BlockModel) model).renderAsBlock(inputContext, emitter());
	}

	@Override
	protected void adjustMaterialForEncoding() {
		if (finder.disableAoIsDefault()) {
			finder.disableAo(true);
		}
	}
}
