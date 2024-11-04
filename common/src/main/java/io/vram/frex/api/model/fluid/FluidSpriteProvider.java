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

package io.vram.frex.api.model.fluid;

import org.jetbrains.annotations.Nullable;
import io.vram.frex.impl.model.SimpleFluidSpriteProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;

/**
 * Get the sprites for a fluid being rendered at a given position.
 * For optimal performance, the sprites should be loaded as part of a
 * resource reload and *not* looked up every time the method is called!
 *
 * @param view  The world view pertaining to the fluid. May be null!
 * @param pos   The position of the fluid in the world. May be null!
 * @param state The current state of the fluid.
 * @return An array of size two or three: the first entry contains the "still" sprite,
 * while the second entry contains the "flowing" sprite. If the array is size three,
 * the third sprite is the "overlay" sprite and its presence indicates an overlay
 * sprite should be used.
 */
@FunctionalInterface
public interface FluidSpriteProvider {
	TextureAtlasSprite[] getFluidSprites(@Nullable BlockAndTintGetter view, @Nullable BlockPos pos, FluidState state);

	static FluidSpriteProvider of(String stillSpriteName, String flowingSpriteName, @Nullable String overlaySpriteName) {
		return SimpleFluidSpriteProvider.of(ResourceLocation.tryParse(stillSpriteName), ResourceLocation.tryParse(flowingSpriteName), overlaySpriteName == null ? null : ResourceLocation.tryParse(overlaySpriteName));
	}

	static FluidSpriteProvider of(String stillSpriteName, String flowingSpriteName) {
		return of(stillSpriteName, flowingSpriteName, null);
	}

	static FluidSpriteProvider of(ResourceLocation stillSpriteName, ResourceLocation flowingSpriteName, @Nullable ResourceLocation overlaySpriteName) {
		return SimpleFluidSpriteProvider.of(stillSpriteName, flowingSpriteName, overlaySpriteName);
	}

	static FluidSpriteProvider of(ResourceLocation stillSpriteName, ResourceLocation flowingSpriteName) {
		return of(stillSpriteName, flowingSpriteName, null);
	}

	FluidSpriteProvider WATER_SPRITES = of("minecraft:block/water_still", "minecraft:block/water_flow", "minecraft:block/water_overlay");
	FluidSpriteProvider LAVA_SPRITES = of("minecraft:block/lava_still", "minecraft:block/lava_flow");
}
