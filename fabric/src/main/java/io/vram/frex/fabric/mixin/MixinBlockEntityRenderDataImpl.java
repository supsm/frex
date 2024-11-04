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

package io.vram.frex.fabric.mixin;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import io.vram.frex.impl.world.BlockEntityRenderDataImpl;

@Mixin(BlockEntityRenderDataImpl.class)
public class MixinBlockEntityRenderDataImpl {
	private static final Function<BlockEntity, Object> FABRIC_PROVIDER = be -> ((RenderDataBlockEntity) be).getRenderData();

	/**
	 * @author Grondag
	 * @reason how we control interop on FAPI
	 */
	@Overwrite(remap = false)
	public static Function<BlockEntity, Object> defaultProvider() {
		return FABRIC_PROVIDER;
	}
}
