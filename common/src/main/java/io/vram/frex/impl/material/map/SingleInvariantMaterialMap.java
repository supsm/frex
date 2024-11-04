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

package io.vram.frex.impl.material.map;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;
import io.vram.frex.api.material.MaterialFinder;
import io.vram.frex.api.material.MaterialMap;
import io.vram.frex.api.material.MaterialTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Internal
class SingleInvariantMaterialMap<T> implements MaterialMap<T> {
	protected final MaterialTransform transform;

	SingleInvariantMaterialMap(MaterialTransform transform) {
		assert transform != null;
		this.transform = transform;
	}

	@Override
	public void map(MaterialFinder finder, T gameObject, @Nullable TextureAtlasSprite sprite) {
		transform.apply(finder);
	}
}
