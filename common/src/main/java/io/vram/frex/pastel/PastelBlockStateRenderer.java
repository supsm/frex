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

import com.mojang.blaze3d.vertex.PoseStack;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.world.RenderRegionBakeListener.BlockStateRenderer;
import io.vram.frex.pastel.mixinterface.RenderChunkRegionExt;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PastelBlockStateRenderer implements BlockStateRenderer {
	private PoseStack matrixStack;
	private RenderChunkRegion chunkRendererRegion;

	public void prepare(PoseStack matrixStack, RenderChunkRegion chunkRendererRegion) {
		this.matrixStack = matrixStack;
		this.chunkRendererRegion = chunkRendererRegion;
	}

	@Override
	public void bake(BlockPos pos, BlockState state) {
		final BakedModel model = BlockModel.get(state);

		matrixStack.pushPose();
		matrixStack.translate(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
		((RenderChunkRegionExt) chunkRendererRegion).frx_getContext().renderBlock(state, pos, model);
		matrixStack.popPose();
	}
}
