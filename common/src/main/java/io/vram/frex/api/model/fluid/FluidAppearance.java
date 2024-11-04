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

import io.vram.frex.impl.model.FluidAppearanceImpl;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public interface FluidAppearance extends FluidColorProvider, FluidSpriteProvider {
	static FluidAppearance of(FluidColorProvider colorProvider, FluidSpriteProvider spriteProvider) {
		return FluidAppearanceImpl.of(colorProvider, spriteProvider);
	}

	static FluidAppearance get(Fluid fluid) {
		return FluidAppearanceImpl.get(fluid);
	}

	static void register(FluidAppearance appearance, Fluid... fluids) {
		FluidAppearanceImpl.register(appearance, fluids);
	}

	static FluidAppearance register(FluidColorProvider colorProvider, FluidSpriteProvider spriteProvider, Fluid... fluids) {
		final var appearance = of(colorProvider, spriteProvider);
		register(appearance, fluids);
		return appearance;
	}

	FluidAppearance WATER_APPEARANCE = register(FluidColorProvider.WATER_COLOR, FluidSpriteProvider.WATER_SPRITES, Fluids.WATER, Fluids.FLOWING_WATER);
	FluidAppearance LAVA_APPEARANCE = register(FluidColorProvider.WHITE_COLOR, FluidSpriteProvider.LAVA_SPRITES, Fluids.LAVA, Fluids.FLOWING_LAVA);
}
