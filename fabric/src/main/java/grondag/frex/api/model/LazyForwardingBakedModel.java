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

package grondag.frex.api.model;

import java.util.List;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

// WIP: create go-forward version of this for BakedModels if seems to have any use (see request from Pepper)
// WIP: also a non-lazy version

/**
 * Improved base class for specialized model implementations that need to wrap other baked models.
 * Avoids boilerplate code for pass-through methods.}.
 */
@Deprecated
public abstract class LazyForwardingBakedModel implements BakedModel, FabricBakedModel {
	protected BakedModel lazyWrapped;

	/** MUST BE THREAD-SAFE AND INVARIANT. */
	protected abstract BakedModel createWrapped();

	protected BakedModel wrapped() {
		BakedModel wrapped = lazyWrapped;

		if (wrapped == null) {
			wrapped = createWrapped();
			lazyWrapped = wrapped;
		}

		return wrapped;
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
		((FabricBakedModel) wrapped()).emitBlockQuads(blockView, state, pos, randomSupplier, context);
	}

	@Override
	public boolean isVanillaAdapter() {
		return ((FabricBakedModel) wrapped()).isVanillaAdapter();
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context) {
		((FabricBakedModel) wrapped()).emitItemQuads(stack, randomSupplier, context);
	}

	@Override
	public List<BakedQuad> getQuads(BlockState blockState, Direction face, RandomSource rand) {
		return wrapped().getQuads(blockState, face, rand);
	}

	@Override
	public boolean useAmbientOcclusion() {
		return wrapped().useAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return wrapped().isGui3d();
	}

	@Override
	public boolean usesBlockLight() {
		return wrapped().usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return wrapped().isCustomRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleIcon() {
		return wrapped().getParticleIcon();
	}

	@Override
	public ItemTransforms getTransforms() {
		return wrapped().getTransforms();
	}

	@Override
	public ItemOverrides getOverrides() {
		return wrapped().getOverrides();
	}
}
