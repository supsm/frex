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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.resources.ResourceLocation;

@FunctionalInterface
public interface ModelLocationProvider {
	/**
	 * Adds model paths to be loaded and baked, even if that model is not used by any blocks or items.
	 *
	 * @param models Provides access to model resources.
	 * @param blockStates Provides access to block state resources.
	 * @param target Accepts paths to be loaded. Arguments that are {@link ModelIdentifier} will be
	 *            parsed as if they are a block or item variant and the load call can be intercepted by a
	 *            {@link VariantModelProvider}. Plain {@link Identifier} arguments will be loaded
	 *            directly as a JSON model unless intercepted by a {@link ResourceModelProvider}.
	 *            For example, <pre>new ResourceLocation("mymod", "foo/bar")</pre> will request loading
	 *            of the file <pre>/assets/mymod/models/foo/bar.json</pre>
	 */
	void provideLocations(Map<ResourceLocation, BlockModel> models, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStates, Consumer<ResourceLocation> target);
}
