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

package io.vram.frex.api.rendertype;

import java.util.function.Predicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeRenderType;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import io.vram.frex.impl.material.RenderTypeExclusionImpl;

@NonExtendable
public interface RenderTypeExclusion {
	static void exclude(RenderType renderType) {
		RenderTypeExclusionImpl.exclude(renderType);
	}

	static void exclude(Predicate<RenderType> filter) {
		RenderTypeExclusionImpl.addFilter(filter);
	}

	static boolean isExcluded(RenderType renderType) {
		return !(renderType instanceof CompositeRenderType) || RenderTypeExclusionImpl.isExcluded(renderType);
	}
}
