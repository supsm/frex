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

package io.vram.frex.api.renderer;

import org.jetbrains.annotations.Nullable;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.RenderMaterial;
import net.minecraft.resources.ResourceLocation;

public interface MaterialManager {
	/**
	 * Obtain a new {@link MaterialFinder} instance used to retrieve
	 * standard {@link RenderMaterial} instances.
	 *
	 * <p>Renderer does not retain a reference to returned instances and they should be re-used for
	 * multiple materials when possible to avoid memory allocation overhead.
	 */
	MaterialFinder materialFinder();

	@Nullable RenderMaterial materialFromId(ResourceLocation id);

	RenderMaterial materialFromIndex(int index);

	boolean registerMaterial(ResourceLocation id, RenderMaterial material);

	/**
	 * Identical to {@link #registerMaterial(ResourceLocation, RenderMaterial)} except registrations
	 * are replaced if they already exist.  Meant to be used for materials that are loaded
	 * from resources and need to be updated during resource reload.
	 *
	 * <p>Note that mods retaining references to materials obtained from the registry will not
	 * use the new material definition unless they re-query.  Material maps will handle this
	 * automatically but mods must be designed to do so.
	 *
	 * <p>If this feature is not supported by the renderer, behaves like {@link #registerMaterial(ResourceLocation, RenderMaterial)}.
	 *
	 * <p>Returns false if a material with the given identifier was already present.
	 */
	default boolean registerOrUpdateMaterial(ResourceLocation id, RenderMaterial material) {
		return registerMaterial(id, material);
	}

	RenderMaterial defaultMaterial();

	RenderMaterial missingMaterial();
}
