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
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.math.MatrixStack;
import io.vram.frex.api.model.BlockModel;
import io.vram.frex.base.renderer.context.input.BaseBlockInputContext;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Context used when blocks are rendered as part of an entity.
 * Vanilla examples include blocks held be endermen, blocks in minecarts,
 * flowers held by iron golems and Mooshroom mushrooms.
 *
 * <p>Also handle rendering of the item frame which looks and acts like a block
 * and has a block JSON model but is an entity.
 */
public abstract class EntityBlockRenderContext extends BlockRenderContext<BlockAndTintGetter> {
	protected int light;
	protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	protected Level level;
	protected float tickDelta;
	@Nullable protected VertexConsumer defaultConsumer;

	@Override
	protected BaseBlockInputContext<BlockAndTintGetter> createInputContext() {
		return new BaseBlockInputContext<>() {
			@Override
			protected int fastBrightness(BlockPos pos) {
				return light;
			}
		};
	}

	public void tickDelta(float tickDelta) {
		this.tickDelta = tickDelta;
	}

	public void setPosAndWorldFromEntity(Entity entity) {
		if (entity != null) {
			final float tickDelta = this.tickDelta;
			final double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
			final double y = Mth.lerp(tickDelta, entity.yo, entity.getY()) + entity.getEyeHeight();
			final double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
			pos.set(x, y, z);
			level = entity.getCommandSenderWorld();
		}
	}

	protected abstract void prepareEncoding(MultiBufferSource vertexConsumers);

	/**
	 * Assumes region and block pos set earlier via {@link #setPosAndWorldFromEntity(Entity)}.
	 */
	public void render(ModelBlockRenderer vanillaRenderer, BakedModel model, BlockState state, com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource consumers, int overlay, int light) {
		defaultConsumer = consumers.getBuffer(ItemBlockRenderTypes.getRenderType(state, false));
		this.light = light;
		inputContext.prepareForWorld(level, false, MatrixStack.fromVanilla(poseStack));
		prepareForBlock(model, state, pos, 42L, overlay);
		prepareEncoding(consumers);
		((BlockModel) model).renderAsBlock(inputContext, emitter());
		defaultConsumer = null;
	}

	// item frames don't have a block state but render like a block
	public void renderItemFrame(ModelBlockRenderer modelRenderer, BakedModel model, com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource consumers, int overlay, int light, ItemFrame itemFrameEntity) {
		defaultConsumer = consumers.getBuffer(Sheets.solidBlockSheet());
		this.light = light;
		inputContext.prepareForWorld(level, false, MatrixStack.fromVanilla(poseStack));
		pos.set(itemFrameEntity.getX(), itemFrameEntity.getY(), itemFrameEntity.getZ());
		inputContext.prepareForBlock(model, Blocks.AIR.defaultBlockState(), pos, 42L, overlay);
		materialMap = MaterialMap.identity();
		prepareEncoding(consumers);
		((BlockModel) model).renderAsBlock(inputContext, emitter());
		defaultConsumer = null;
	}

	@Override
	protected void adjustMaterialForEncoding() {
		if (finder.disableAoIsDefault()) {
			finder.disableAo(true);
		}
	}
}
