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

package io.vram.frex.api.model.provider;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Passed to model providers as a way for them to retrieve models on
 * which they depend. Makes no guarantee regarding the availability
 * of requested models, nor the specific implementation of models returned.
 */
@FunctionalInterface
public interface SubModelLoader {
	/**
	 * Attempts to load the requested model.  Will throw an exception and cause loading of the
	 * current model to fail if a circular dependency is detected.
	 *
	 * @param path identifies the sub model to load.
	 * @return The loaded UnbakedModel or the missing model instance if the requested path is not found.
	 */
	@Nullable
	UnbakedModel loadSubModel(ResourceLocation path);
}
