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

/*
 * This particular set of hooks was adapted from Fabric, and was originally
 * written by Asie Kierka if memory serves. The Fabric copyright and
 * license notice is reproduced below:
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vram.frex.impl.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.include.com.google.common.base.Preconditions;
import io.vram.frex.api.config.FrexConfig;
import io.vram.frex.api.model.provider.ModelLocationProvider;
import io.vram.frex.api.model.provider.ModelProvider;
import io.vram.frex.api.model.provider.SubModelLoader;
import io.vram.frex.impl.FrexLog;
import io.vram.frex.mixinterface.ModelBakeryExt;

public class ModelProviderRegistryImpl {
	public static class LoaderInstance implements SubModelLoader {
		private final List<ModelProvider<ResourceLocation>> modelVariantProviders;
		private final List<ModelProvider<ResourceLocation>> modelResourceProviders;
		private final Object2ObjectOpenHashMap<ResourceLocation, ModelProvider<ResourceLocation>> blockItemProviders = new Object2ObjectOpenHashMap<>();

		private ModelBakery loader;

		private LoaderInstance(ModelBakery loader, Map<ResourceLocation, BlockModel> models, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStates) {
			this.loader = loader;
			this.modelVariantProviders = variantProviderFunctions.stream().map((s) -> s.apply(models, blockStates)).collect(Collectors.toList());
			this.modelResourceProviders = resourceProviderFunctions.stream().map((s) -> s.apply(models, blockStates)).collect(Collectors.toList());

			for (final var pair : blockItemProviderFunctions) {
				final var func = pair.getLeft().apply(models, blockStates);

				for (final var path : pair.getRight()) {
					blockItemProviders.putIfAbsent(path, func);
				}
			}
		}

		@Override
		public UnbakedModel loadSubModel(ResourceLocation id) {
			if (loader == null) {
				throw new RuntimeException("A model provider attempted to access ModelBakery after model baking was complete.");
			}

			return ((ModelBakeryExt) loader).frx_loadModel(id);
		}

		@Nullable
		public UnbakedModel loadModelFromResource(ResourceLocation resourceId) {
			return loadCustomModel((r) -> r.loadModel(resourceId, this), modelResourceProviders, "resource provider");
		}

		@Nullable
		public UnbakedModel loadModelFromVariant(ResourceLocation path) {
			if (!(path instanceof final ModelResourceLocation modelId)) {
				return loadModelFromResource(path);
			} else {
				final var variantId = (ResourceLocation) path;
				final ModelProvider<ResourceLocation> pathProvider = blockItemProviders.get(ResourceLocation.fromNamespaceAndPath(path.getNamespace(), path.getPath()));
				UnbakedModel model = null;

				if (pathProvider != null) {
					try {
						model = pathProvider.loadModel(variantId, this);
					} catch (final Exception e) {
						FrexLog.error(e);
					}
				}

				if (model != null) {
					return model;
				}

				model = loadCustomModel((r) -> r.loadModel(variantId, this), modelVariantProviders, "resource provider");

				if (model != null) {
					return model;
				}

				// Replicating the special-case from ModelBakery as loadModelFromJson is insufficiently patchable
				if (Objects.equals(modelId.getVariant(), "inventory")) {
					final ResourceLocation resourceId = ResourceLocation.fromNamespaceAndPath(modelId.getNamespace(), "item/" + modelId.getPath());
					model = loadModelFromResource(resourceId);

					if (model != null) {
						return model;
					}
				}

				return null;
			}
		}

		public void finish() {
			loader = null;
		}
	}

	private static <T> UnbakedModel loadCustomModel(Function<T, UnbakedModel> function, Collection<T> loaders, String debugName) {
		if (!FrexConfig.debugModelLoading) {
			for (final T provider : loaders) {
				try {
					final UnbakedModel model = function.apply(provider);

					if (model != null) {
						return model;
					}
				} catch (final Exception e) {
					FrexLog.error(e);
					return null;
				}
			}

			return null;
		}

		UnbakedModel modelLoaded = null;
		T providerUsed = null;
		List<T> providersApplied = null;

		for (final T provider : loaders) {
			try {
				final UnbakedModel model = function.apply(provider);

				if (model != null) {
					if (providersApplied != null) {
						providersApplied.add(provider);
					} else if (providerUsed != null) {
						providersApplied = Lists.newArrayList(providerUsed, provider);
					} else {
						modelLoaded = model;
						providerUsed = provider;
					}
				}
			} catch (final Exception e) {
				FrexLog.error(e);
				return null;
			}
		}

		if (providersApplied != null) {
			final StringBuilder builder = new StringBuilder("Conflict - multiple " + debugName + "s claimed the same unbaked model:");

			for (final T loader : providersApplied) {
				builder.append("\n\t - ").append(loader.getClass().getName());
			}

			FrexLog.error(builder.toString());
			return null;
		} else {
			return modelLoaded;
		}
	}

	private static final ObjectArrayList<BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>>> variantProviderFunctions = new ObjectArrayList<>();
	private static final ObjectArrayList<BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>>> resourceProviderFunctions = new ObjectArrayList<>();
	private static final ObjectArrayList<Pair<BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>>, ResourceLocation[]>> blockItemProviderFunctions = new ObjectArrayList<>();
	private static final ObjectArrayList<ModelLocationProvider> locationProviders = new ObjectArrayList<>();

	public static void registerLocationProvider(ModelLocationProvider provider) {
		locationProviders.add(provider);
	}

	public static void registerResourceProvider(BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>> providerFunction) {
		resourceProviderFunctions.add(providerFunction);
	}

	public static void registerVariantProvider(BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>> providerFunction) {
		variantProviderFunctions.add(providerFunction);
	}

	public static LoaderInstance begin(ModelBakery loader, Map<ResourceLocation, BlockModel> models, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStates) {
		return new LoaderInstance(loader, models, blockStates);
	}

	public static void onModelPopulation(Map<ResourceLocation, BlockModel> models, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>> blockStates, Consumer<ResourceLocation> target) {
		for (final ModelLocationProvider appender : locationProviders) {
			appender.provideLocations(models, blockStates, target);
		}
	}

	public static void registerBlockItemProvider(BiFunction<Map<ResourceLocation, BlockModel>, Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>, ModelProvider<ResourceLocation>> providerFunction, ResourceLocation... paths) {
		blockItemProviderFunctions.add(Pair.of(providerFunction, paths));
	}

	public static ResourceLocation[] stringsToLocations(String... paths) {
		Preconditions.checkNotNull(paths);
		return Stream.of(paths).map(p -> ResourceLocation.parse(p)).collect(Collectors.toList()).toArray(new ResourceLocation[paths.length]);
	}
}
