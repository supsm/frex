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

package io.vram.frex.base.renderer.material;

import io.vram.frex.api.material.RenderMaterial;

public abstract class BaseRenderMaterial extends BaseMaterialView implements RenderMaterial {
	protected final boolean blur;
	protected final boolean castShadows;
	protected final int conditionIndex;
	protected final boolean cull;
	protected final int cutout;
	protected final int decal;
	protected final int depthTest;
	protected final MaterialTriState disableAo;
	protected final boolean disableColorIndex;
	protected final MaterialTriState disableDiffuse;
	protected final boolean discardsTexture;
	protected final MaterialTriState emissive;
	protected final MaterialTriState flashOverlay;
	protected final boolean fog;
	protected final MaterialTriState foilOverlay;
	protected final MaterialTriState hurtOverlay;
	protected final int index;
	protected final boolean lines;
	protected final int preset;
	protected final int shaderIndex;
	protected final boolean sorted;
	protected final int target;
	protected final int textureIndex;
	protected final int transparency;
	protected final boolean unmipped;
	protected final int writeMask;
	protected final BaseMaterialManager<? extends BaseRenderMaterial> manager;
	protected final int hashCode;

	public BaseRenderMaterial(BaseMaterialManager<? extends BaseRenderMaterial> manager, int index, BaseMaterialView template) {
		super(template.bits0, template.bits1, template.label);
		this.hashCode = super.hashCode();
		this.manager = manager;
		this.index = index;
		blur = super.blur();
		castShadows = super.castShadows();
		conditionIndex = super.conditionIndex();
		cull = super.cull();
		cutout = super.cutout();
		decal = super.decal();
		depthTest = super.depthTest();
		disableAo = MaterialTriState.of(super.disableAo(), super.disableAoIsDefault());
		disableColorIndex = super.disableColorIndex();
		disableDiffuse = MaterialTriState.of(super.disableDiffuse(), super.disableDiffuseIsDefault());
		discardsTexture = super.discardsTexture();
		emissive = MaterialTriState.of(super.emissive(), super.emissiveIsDefault());
		flashOverlay = MaterialTriState.of(super.flashOverlay(), super.flashOverlayIsDefault());
		fog = super.fog();
		foilOverlay = MaterialTriState.of(super.foilOverlay(), super.foilOverlayIsDefault());
		hurtOverlay = MaterialTriState.of(super.hurtOverlay(), super.hurtOverlayIsDefault());
		lines = super.lines();
		preset = super.preset();
		shaderIndex = super.shaderIndex();
		sorted = super.sorted();
		target = super.target();
		textureIndex = super.textureIndex();
		transparency = super.transparency();
		unmipped = super.unmipped();
		writeMask = super.writeMask();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean blur() {
		return blur;
	}

	@Override
	public boolean castShadows() {
		return castShadows;
	}

	@Override
	public int conditionIndex() {
		return conditionIndex;
	}

	@Override
	public boolean cull() {
		return cull;
	}

	@Override
	public int cutout() {
		return cutout;
	}

	@Override
	public int decal() {
		return decal;
	}

	@Override
	public int depthTest() {
		return depthTest;
	}

	@Override
	public boolean disableAo() {
		return disableAo.value;
	}

	@Override
	public boolean disableAoIsDefault() {
		return disableAo.isDefault;
	}

	@Override
	public boolean disableColorIndex() {
		return disableColorIndex;
	}

	@Override
	public boolean disableDiffuse() {
		return disableDiffuse.value;
	}

	@Override
	public boolean disableDiffuseIsDefault() {
		return disableDiffuse.isDefault;
	}

	@Override
	public boolean discardsTexture() {
		return discardsTexture;
	}

	@Override
	public boolean emissive() {
		return emissive.value;
	}

	@Override
	public boolean emissiveIsDefault() {
		return emissive.isDefault;
	}

	@Override
	public boolean flashOverlay() {
		return flashOverlay.value;
	}

	@Override
	public boolean flashOverlayIsDefault() {
		return flashOverlay.isDefault;
	}

	@Override
	public boolean fog() {
		return fog;
	}

	@Override
	public boolean foilOverlay() {
		return foilOverlay.value;
	}

	@Override
	public boolean foilOverlayIsDefault() {
		return foilOverlay.isDefault;
	}

	@Override
	public boolean hurtOverlay() {
		return hurtOverlay.value;
	}

	@Override
	public boolean hurtOverlayIsDefault() {
		return hurtOverlay.isDefault;
	}

	@Override
	public int index() {
		return index;
	}

	@Override
	public boolean lines() {
		return lines;
	}

	@Override
	public int preset() {
		return preset;
	}

	@Override
	public int shaderIndex() {
		return shaderIndex;
	}

	@Override
	public boolean sorted() {
		return sorted;
	}

	@Override
	public int target() {
		return target;
	}

	@Override
	public int textureIndex() {
		return textureIndex;
	}

	@Override
	public int transparency() {
		return transparency;
	}

	@Override
	public boolean unmipped() {
		return unmipped;
	}

	@Override
	public int writeMask() {
		return writeMask;
	}
}
