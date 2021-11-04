/*
 * Copyright © Contributing Authors
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
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package io.vram.frex.pastel;

import static io.vram.frex.base.renderer.util.EncoderUtil.colorizeQuad;

import org.jetbrains.annotations.Nullable;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.math.PackedSectionPos;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.model.util.ColorUtil;
import io.vram.frex.base.renderer.ao.AoCalculator;
import io.vram.frex.base.renderer.context.input.BaseBlockInputContext;
import io.vram.frex.base.renderer.context.render.BlockRenderContext;
import io.vram.frex.base.renderer.util.EncoderUtil;
import io.vram.frex.pastel.util.RenderChunkRegionExt;

public class PastelTerrainRenderContext extends BlockRenderContext<RenderChunkRegion> {
	protected RenderChunkRegionExt regionExt;
	protected ChunkBufferBuilderPack buffers;

	private final AoCalculator aoCalc = new AoCalculator() {
		@Override
		protected int ao(int cacheIndex) {
			return regionExt.frx_cachedAoLevel(cacheIndex);
		}

		@Override
		protected int brightness(int cacheIndex) {
			return regionExt.frx_cachedBrightness(cacheIndex);
		}

		@Override
		protected boolean isOpaque(int cacheIndex) {
			return regionExt.frx_isClosed(cacheIndex);
		}

		@Override
		protected int cacheIndexFromSectionIndex(int packedSectorIndex) {
			return packedSectorIndex;
		}
	};

	@Override
	protected BaseBlockInputContext<RenderChunkRegion> createInputContext() {
		return new BaseBlockInputContext<>() {
			@Override
			protected int fastBrightness(BlockPos pos) {
				return regionExt.frx_cachedBrightness(pos);
			}

			@Override
			public @Nullable Object blockEntityRenderData(BlockPos pos) {
				return regionExt.frx_getBlockEntityRenderData(pos);
			}
		};
	}

	public PastelTerrainRenderContext prepareForRegion(RenderChunkRegion region, PoseStack poseStack, BlockPos origin, ChunkBufferBuilderPack buffers) {
		inputContext.prepareForWorld(region, true, (MatrixStack) poseStack);
		regionExt = (RenderChunkRegionExt) region;
		regionExt.frx_setContext(this, origin);
		this.buffers = buffers;
		return this;
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, boolean defaultAo, final BlockModel model) {
		aoCalc.prepare(PackedSectionPos.packWithSectionMask(blockPos));
		prepareForFluid(blockState, blockPos, defaultAo);
		renderInner(model);
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, final BakedModel model) {
		defaultConsumer = buffers.builder(ItemBlockRenderTypes.getChunkRenderType(blockState));
		aoCalc.prepare(PackedSectionPos.packWithSectionMask(blockPos));
		prepareForBlock(model, blockState, blockPos);
		renderInner((BlockModel) model);
	}

	private void renderInner(final BlockModel model) {
		try {
			model.renderAsBlock(this.inputContext, emitter());
		} catch (final Throwable var9) {
			final CrashReport crashReport_1 = CrashReport.forThrowable(var9, "Tesselating block in world - Canvas Renderer");
			final CrashReportCategory crashReportElement_1 = crashReport_1.addCategory("Block being tesselated");
			CrashReportCategory.populateBlockDetails(crashReportElement_1, inputContext.blockView(), inputContext.pos(), inputContext.blockState());
			throw new ReportedException(crashReport_1);
		}
	}

	@Override
	protected void shadeQuad() {
		// tint before we apply shading
		colorizeQuad(emitter, this.inputContext);

		if (!emitter.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			aoCalc.compute(emitter);
			final var blockView = inputContext.blockView();

			if (emitter.material().disableDiffuse()) {
				// if diffuse is disabled, some dimensions can still have an ambient shading value.
				final float shade = blockView.getShade(Direction.UP, false);

				if (shade == 1.0f) {
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), emitter.ao[0]));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), emitter.ao[1]));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), emitter.ao[2]));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), emitter.ao[3]));
				} else {
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade * emitter.ao[0]));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade * emitter.ao[1]));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade * emitter.ao[2]));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade * emitter.ao[3]));
				}
			} else {
				if (emitter.hasVertexNormals()) {
					// different shade value per vertex
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), EncoderUtil.normalShade(emitter.packedNormal(0), blockView, true) * emitter.ao[0]));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), EncoderUtil.normalShade(emitter.packedNormal(1), blockView, true) * emitter.ao[1]));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), EncoderUtil.normalShade(emitter.packedNormal(2), blockView, true) * emitter.ao[2]));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), EncoderUtil.normalShade(emitter.packedNormal(3), blockView, true) * emitter.ao[3]));
				} else {
					// same shade value for all vertices
					final float shade = blockView.getShade(emitter.lightFace(), true);

					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade * emitter.ao[0]));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade * emitter.ao[1]));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade * emitter.ao[2]));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade * emitter.ao[3]));
				}
			}
		} else if (PastelRenderer.semiFlatLighting) {
			aoCalc.computeFlat(emitter);
			final var blockView = inputContext.blockView();

			if (emitter.material().disableDiffuse()) {
				// if diffuse is disabled, some dimensions can still have an ambient shading value.
				final float shade = blockView.getShade(Direction.UP, false);

				if (shade != 1.0f) {
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade));
				}
			} else {
				if (emitter.hasVertexNormals()) {
					// different shade value per vertex
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), EncoderUtil.normalShade(emitter.packedNormal(0), blockView, true)));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), EncoderUtil.normalShade(emitter.packedNormal(1), blockView, true)));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), EncoderUtil.normalShade(emitter.packedNormal(2), blockView, true)));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), EncoderUtil.normalShade(emitter.packedNormal(3), blockView, true)));
				} else {
					// same shade value for all vertices
					final float shade = blockView.getShade(emitter.lightFace(), true);

					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade));
				}
			}
		} else {
			EncoderUtil.applyFlatLighting(emitter, inputContext.flatBrightness(emitter));
		}
	}

	@Override
	protected void encodeQuad() {
		// WIP: handle non-default render layers - will need to capture immediate
		EncoderUtil.encodeQuad(emitter, inputContext, defaultConsumer);
	}

	public static final ThreadLocal<PastelTerrainRenderContext> POOL = ThreadLocal.withInitial(PastelTerrainRenderContext::new);
}