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

import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.vram.frex.api.material.MaterialConstants;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.math.FixedMath255;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.math.PackedSectionPos;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.api.model.util.ColorUtil;
import io.vram.frex.base.renderer.ao.AoCalculator;
import io.vram.frex.base.renderer.context.input.BaseBlockInputContext;
import io.vram.frex.base.renderer.context.render.BlockRenderContext;
import io.vram.frex.base.renderer.util.EncoderUtil;
import io.vram.frex.pastel.mixinterface.RenderChunkRegionExt;

public class PastelTerrainRenderContext extends BlockRenderContext<BlockAndTintGetter> {
	protected RenderChunkRegionExt regionExt;
	protected SectionBufferBuilderPack buffers;
	@SuppressWarnings("rawtypes")
	protected Map initializedBuffers;

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
	protected BaseBlockInputContext<BlockAndTintGetter> createInputContext() {
		return new BaseBlockInputContext<>() {
			@Override
			protected int fastBrightness(BlockPos pos) {
				return regionExt.frx_cachedBrightness(pos);
			}

			@Override
			public @Nullable Object blockEntityRenderData(BlockPos pos) {
				return regionExt.frx_getBlockEntityRenderData(pos);
			}

			@Override
			public Biome getBiome(BlockPos pos) {
				return regionExt.frx_getBiome(pos);
			}

			@Override
			public boolean hasBiomeAccess() {
				return true;
			}
		};
	}

	public PastelTerrainRenderContext prepareForRegion(RenderChunkRegion region, com.mojang.blaze3d.vertex.PoseStack poseStack, BlockPos origin, SectionBufferBuilderPack buffers, @SuppressWarnings("rawtypes") Map map) {
		inputContext.prepareForWorld(region, true, MatrixStack.fromVanilla(poseStack));
		regionExt = (RenderChunkRegionExt) region;
		this.initializedBuffers = map;
		regionExt.frx_setContext(this, origin);
		this.buffers = buffers;
		return this;
	}

	public void overrideBlockView(BlockAndTintGetter blockView) {
		inputContext.setWorld(blockView);
	}

	public void renderFluid(BlockState blockState, BlockPos blockPos, final BlockModel model) {
		aoCalc.prepare(PackedSectionPos.packWithSectionMask(blockPos));
		// for whatever reason, Mojang doesn't do section position transformation before invoking fluid render so we do it here
		final var matrixStack = inputContext.matrixStack();
		matrixStack.push();
		matrixStack.translate(blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15);
		prepareForFluid(blockState, blockPos);
		renderInner(model);
		matrixStack.pop();
	}

	public void renderBlock(BlockState blockState, BlockPos blockPos, final BakedModel model) {
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
		emitter.colorize(this.inputContext);

		if (!emitter.material().disableAo() && Minecraft.useAmbientOcclusion()) {
			aoCalc.compute(emitter);
			final var blockView = inputContext.blockView();

			if (emitter.material().disableDiffuse()) {
				// if diffuse is disabled, some dimensions can still have an ambient shading value.
				final float shade = blockView.getShade(Direction.UP, false);

				if (shade == 1.0f) {
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), emitter.ao[0] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), emitter.ao[1] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), emitter.ao[2] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), emitter.ao[3] * FixedMath255.FLOAT_CONVERSION_FACTOR));
				} else {
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade * emitter.ao[0] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade * emitter.ao[1] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade * emitter.ao[2] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade * emitter.ao[3] * FixedMath255.FLOAT_CONVERSION_FACTOR));
				}
			} else {
				if (emitter.hasVertexNormals()) {
					// different shade value per vertex
					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), EncoderUtil.normalShade(emitter.packedNormal(0), blockView, true) * emitter.ao[0] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), EncoderUtil.normalShade(emitter.packedNormal(1), blockView, true) * emitter.ao[1] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), EncoderUtil.normalShade(emitter.packedNormal(2), blockView, true) * emitter.ao[2] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), EncoderUtil.normalShade(emitter.packedNormal(3), blockView, true) * emitter.ao[3] * FixedMath255.FLOAT_CONVERSION_FACTOR));
				} else {
					// same shade value for all vertices
					final float shade = blockView.getShade(emitter.lightFace(), true);

					emitter.vertexColor(0, ColorUtil.multiplyRGB(emitter.vertexColor(0), shade * emitter.ao[0] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(1, ColorUtil.multiplyRGB(emitter.vertexColor(1), shade * emitter.ao[1] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(2, ColorUtil.multiplyRGB(emitter.vertexColor(2), shade * emitter.ao[2] * FixedMath255.FLOAT_CONVERSION_FACTOR));
					emitter.vertexColor(3, ColorUtil.multiplyRGB(emitter.vertexColor(3), shade * emitter.ao[3] * FixedMath255.FLOAT_CONVERSION_FACTOR));
				}
			}
		} else {
			if (PastelRenderer.semiFlatLighting) {
				aoCalc.computeFlat(emitter);
			} else {
				emitter.applyFlatLighting(inputContext.flatBrightness(emitter));
			}

			applySimpleDiffuseShade();
		}
	}

	/** Lazily retrieves output buffer for given layer, initializing as needed. */
	@SuppressWarnings("unchecked")
	protected BufferBuilder getInitializedBuffer(RenderType renderLayer) {
		BufferBuilder result = (BuilderBuffer)initializedBuffers.get(renderLayer);

		if (result == null) {
			result = new BufferBuilder(buffers.buffer(renderLayer), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
			initializedBuffers.put(renderLayer, result);
		}

		return result;
	}

	@Override
	protected void encodeQuad() {
		RenderType renderType;
		final var mat = emitter.material();

		// NB: by the time we get here material should be fully specified - no default preset
		assert mat.preset() != MaterialConstants.PRESET_DEFAULT;

		if (mat.transparency() != MaterialConstants.TRANSPARENCY_NONE) {
			renderType = RenderType.translucent();
		} else if (mat.cutout() == MaterialConstants.CUTOUT_NONE) {
			renderType = RenderType.solid();
		} else {
			renderType = mat.unmipped() ? RenderType.cutout() : RenderType.cutoutMipped();
		}

		EncoderUtil.encodeQuad(emitter, inputContext, getInitializedBuffer(renderType));
	}

	public static final ThreadLocal<PastelTerrainRenderContext> POOL = ThreadLocal.withInitial(PastelTerrainRenderContext::new);
}
