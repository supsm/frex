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

package io.vram.frex.base.renderer.mesh;

import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.EMPTY;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_BITS;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_COLOR_INDEX;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_FIRST_VERTEX_TANGENT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_MATERIAL;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_SPRITE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.HEADER_TAG;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_QUAD_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.MESH_VERTEX_STRIDE_SHIFT;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.UV_PRECISE_UNIT_VALUE;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR1;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR2;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_COLOR3;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP1;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP2;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_LIGHTMAP3;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_NORMAL0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_U0;
import static io.vram.frex.base.renderer.mesh.MeshEncodingHelper.VERTEX_X0;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.buffer.VertexEmitter;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.math.PackedVector3f;
import io.vram.frex.api.model.BakedInputContext;
import io.vram.frex.api.model.util.ColorUtil;
import io.vram.frex.api.model.util.FaceUtil;
import io.vram.frex.api.texture.SpriteIndex;
import io.vram.frex.impl.texture.IndexedSprite;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

/**
 * Almost-concrete implementation of a mutable quad. The only missing part is {@link #emit()},
 * because that depends on where/how it is used. (Mesh encoding vs. render-time transformation).
 */
public abstract class BaseQuadEmitter extends BaseQuadView implements QuadEmitter, VertexEmitter {
	// PERF: could pack values into a single int here
	public final int[] ao = new int[]{255, 255, 255, 255};
	protected RenderMaterial defaultMaterial = RenderMaterial.defaultMaterial();

	private int vertexIndex = 0;

	public final void begin(int[] data, int baseIndex) {
		this.data = data;
		this.baseIndex = baseIndex;
		clear();
	}

	@Override
	public BaseQuadEmitter defaultMaterial(RenderMaterial defaultMaterial) {
		this.defaultMaterial = defaultMaterial;
		return this;
	}

	/**
	 * Call before emit or mesh incorporation.
	 */
	public final void complete() {
		computeGeometry();
		packedFaceTanget();
		normalizeSpritesIfNeeded();
		vertexIndex = 0;
	}

	public void clear() {
		System.arraycopy(EMPTY, 0, data, baseIndex, MeshEncodingHelper.TOTAL_MESH_QUAD_STRIDE);
		isGeometryInvalid = true;
		isTangentInvalid = true;
		nominalFaceId = FaceUtil.UNASSIGNED_INDEX;
		material(defaultMaterial);
		isSpriteInterpolated = false;
		vertexIndex = 0;
	}

	@Override
	public final BaseQuadEmitter material(RenderMaterial material) {
		if (material == null) {
			material = defaultMaterial;
		}

		this.material = material;
		data[baseIndex + HEADER_MATERIAL] = material.index();
		return this;
	}

	public final BaseQuadEmitter cullFace(int faceId) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(data[baseIndex + HEADER_BITS], faceId);
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final BaseQuadEmitter cullFace(Direction face) {
		return cullFace(FaceUtil.toFaceIndex(face));
	}

	public final BaseQuadEmitter nominalFace(int faceId) {
		nominalFaceId = faceId;
		return this;
	}

	@Override
	@Deprecated
	public final BaseQuadEmitter nominalFace(Direction face) {
		return nominalFace(FaceUtil.toFaceIndex(face));
	}

	@Override
	public final BaseQuadEmitter colorIndex(int colorIndex) {
		data[baseIndex + HEADER_COLOR_INDEX] = colorIndex;
		return this;
	}

	@Override
	public final BaseQuadEmitter tag(int tag) {
		data[baseIndex + HEADER_TAG] = tag;
		return this;
	}

	private void convertVanillaUvPrecision() {
		// Convert sprite data from float to fixed precision
		int index = baseIndex + 0 * MESH_VERTEX_STRIDE + VERTEX_COLOR0 + 1;

		for (int i = 0; i < 4; ++i) {
			data[index] = (int) (Float.intBitsToFloat(data[index]) * UV_PRECISE_UNIT_VALUE);
			data[index + 1] = (int) (Float.intBitsToFloat(data[index + 1]) * UV_PRECISE_UNIT_VALUE);
			index += MESH_VERTEX_STRIDE;
		}
	}

	@Override
	public QuadEmitter fromVanilla(int[] quadData, int startIndex) {
		System.arraycopy(quadData, startIndex, data, baseIndex + HEADER_STRIDE, MESH_QUAD_STRIDE);
		convertVanillaUvPrecision();
		normalizeSprite();
		material(defaultMaterial);
		isSpriteInterpolated = false;
		isGeometryInvalid = true;
		isTangentInvalid = true;
		return this;
	}

	@Override
	public final BaseQuadEmitter fromVanilla(BakedQuad quad, RenderMaterial material, Direction cullFace) {
		return fromVanilla(quad, material, FaceUtil.toFaceIndex(cullFace));
	}

	@Override
	public final BaseQuadEmitter fromVanilla(BakedQuad quad, RenderMaterial material, int cullFaceId) {
		System.arraycopy(quad.getVertices(), 0, data, baseIndex + HEADER_STRIDE, MESH_QUAD_STRIDE);
		material(material);
		convertVanillaUvPrecision();
		normalizeSprite();
		isSpriteInterpolated = false;
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.cullFace(0, cullFaceId);
		nominalFaceId = FaceUtil.toFaceIndex(quad.getDirection());
		data[baseIndex + HEADER_COLOR_INDEX] = quad.getTintIndex();
		data[baseIndex + HEADER_TAG] = 0;
		isGeometryInvalid = true;
		isTangentInvalid = true;
		return this;
	}

	@Override
	public BaseQuadEmitter pos(int vertexIndex, float x, float y, float z) {
		final int index = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_X0;
		data[index] = Float.floatToRawIntBits(x);
		data[index + 1] = Float.floatToRawIntBits(y);
		data[index + 2] = Float.floatToRawIntBits(z);
		isGeometryInvalid = true;
		return this;
	}

	public void normalFlags(int flags) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.normalFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public BaseQuadEmitter normal(int vertexIndex, float x, float y, float z) {
		normalFlags(normalFlags() | (1 << vertexIndex));
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_NORMAL0] = PackedVector3f.pack(x, y, z);
		return this;
	}

	public void tangentFlags(int flags) {
		data[baseIndex + HEADER_BITS] = MeshEncodingHelper.tangentFlags(data[baseIndex + HEADER_BITS], flags);
	}

	@Override
	public BaseQuadEmitter tangent(int vertexIndex, float x, float y, float z) {
		tangentFlags(tangentFlags() | (1 << vertexIndex));
		data[baseIndex + vertexIndex + HEADER_FIRST_VERTEX_TANGENT] = PackedVector3f.pack(x, y, z);
		return this;
	}

	@Override
	public BaseQuadEmitter lightmap(int vertexIndex, int lightmap) {
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_LIGHTMAP0] = lightmap;
		return this;
	}

	public void applyFlatLighting(final int lightmap) {
		final int baseIndex = this.baseIndex;
		final int[] data = this.data;
		final int i0 = baseIndex + VERTEX_LIGHTMAP0;
		data[i0] = ColorUtil.maxBrightness(data[i0], lightmap);
		final int i1 = baseIndex + VERTEX_LIGHTMAP1;
		data[i1] = ColorUtil.maxBrightness(data[i1], lightmap);
		final int i2 = baseIndex + VERTEX_LIGHTMAP2;
		data[i2] = ColorUtil.maxBrightness(data[i2], lightmap);
		final int i3 = baseIndex + VERTEX_LIGHTMAP3;
		data[i3] = ColorUtil.maxBrightness(data[i3], lightmap);
	}

	@Override
	public BaseQuadEmitter vertexColor(int vertexIndex, int color) {
		data[baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_COLOR0] = color;
		return this;
	}

	/**
	 * Handles block color and red-blue swizzle, common to all renders.
	 */
	public void colorize(BakedInputContext context) {
		final int colorIndex = data[baseIndex + HEADER_COLOR_INDEX];
		final int color = colorIndex == -1 || material().disableColorIndex() ? ColorUtil.WHITE : context.indexedColor(colorIndex);

		if (color == ColorUtil.WHITE) {
			// NB: static final input, so assuming JVM optimization will eliminate if not needed
			if (ColorUtil.SWAP_RED_BLUE) {
				swapRedBlue();
			}
		} else {
			// NB: static final input, so assuming JVM optimization will mitigate branching
			if (ColorUtil.SWAP_RED_BLUE) {
				colorizeSwapRedBlue(color);
			} else {
				colorize(color);
			}
		}
	}

	private void swapRedBlue() {
		final int[] data = this.data;
		final int baseIndex = this.baseIndex;
		final int i0 = baseIndex + VERTEX_COLOR0;
		data[i0] = ColorUtil.swapRedBlue(data[i0]);
		final int i1 = baseIndex + VERTEX_COLOR1;
		data[i1] = ColorUtil.swapRedBlue(data[i1]);
		final int i2 = baseIndex + VERTEX_COLOR2;
		data[i2] = ColorUtil.swapRedBlue(data[i2]);
		final int i3 = baseIndex + VERTEX_COLOR3;
		data[i3] = ColorUtil.swapRedBlue(data[i3]);
	}

	private void colorizeSwapRedBlue(final int color) {
		final int[] data = this.data;
		final int baseIndex = this.baseIndex;
		final int i0 = baseIndex + VERTEX_COLOR0;
		data[i0] = ColorUtil.multiplyColorSwapRedBlue(color, data[i0]);
		final int i1 = baseIndex + VERTEX_COLOR1;
		data[i1] = ColorUtil.multiplyColorSwapRedBlue(color, data[i1]);
		final int i2 = baseIndex + VERTEX_COLOR2;
		data[i2] = ColorUtil.multiplyColorSwapRedBlue(color, data[i2]);
		final int i3 = baseIndex + VERTEX_COLOR3;
		data[i3] = ColorUtil.multiplyColorSwapRedBlue(color, data[i3]);
	}

	private void colorize(int color) {
		final int[] data = this.data;
		final int baseIndex = this.baseIndex;
		final int i0 = baseIndex + VERTEX_COLOR0;
		data[i0] = ColorUtil.multiplyColor(color, data[i0]);
		final int i1 = baseIndex + VERTEX_COLOR1;
		data[i1] = ColorUtil.multiplyColor(color, data[i1]);
		final int i2 = baseIndex + VERTEX_COLOR2;
		data[i2] = ColorUtil.multiplyColor(color, data[i2]);
		final int i3 = baseIndex + VERTEX_COLOR3;
		data[i3] = ColorUtil.multiplyColor(color, data[i3]);
	}

	public final void setSpriteNormalized() {
		isSpriteInterpolated = false;
	}

	public BaseQuadEmitter spriteFloat(int vertexIndex, float u, float v) {
		final int i = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0;
		data[i] = (int) (u * UV_PRECISE_UNIT_VALUE + 0.5f);
		data[i + 1] = (int) (v * UV_PRECISE_UNIT_VALUE + 0.5f);
		isTangentInvalid = true;
		return this;
	}

	/**
	 * Must call {@link #spriteId(int)} separately.
	 */
	public BaseQuadEmitter spritePrecise(int vertexIndex, int u, int v) {
		final int i = baseIndex + (vertexIndex << MESH_VERTEX_STRIDE_SHIFT) + VERTEX_U0;
		data[i] = u;
		data[i + 1] = v;
		isTangentInvalid = true;
		assert !isSpriteInterpolated;
		return this;
	}

	public void normalizeSpritesIfNeeded() {
		if (isSpriteInterpolated) {
			normalizeSprite();
			isSpriteInterpolated = false;
		}
	}

	protected void normalizeSprite() {
		if (material().texture().isAtlas()) {
			final TextureAtlasSprite sprite = findSprite();
			final int spriteId = ((IndexedSprite) sprite).frex_index();
			final float u0 = sprite.getU0();
			final float v0 = sprite.getV0();
			final float uSpanInv = 1f / (sprite.getU1() - u0);
			final float vSpanInv = 1f / (sprite.getV1() - v0);

			spriteFloat(0, (spriteFloatU(0) - u0) * uSpanInv, (spriteFloatV(0) - v0) * vSpanInv);
			spriteFloat(1, (spriteFloatU(1) - u0) * uSpanInv, (spriteFloatV(1) - v0) * vSpanInv);
			spriteFloat(2, (spriteFloatU(2) - u0) * uSpanInv, (spriteFloatV(2) - v0) * vSpanInv);
			spriteFloat(3, (spriteFloatU(3) - u0) * uSpanInv, (spriteFloatV(3) - v0) * vSpanInv);
			spriteId(spriteId);
		} else {
			assert false : "Attempt to normalize non-atlas sprite coordinates.";
		}
	}

	/**
	 * Same as logic in SpriteFinder but can assume sprites are mapped - avoids checks.
	 */
	protected TextureAtlasSprite findSprite() {
		float u = 0;
		float v = 0;

		for (int i = 0; i < 4; i++) {
			u += spriteFloatU(i);
			v += spriteFloatV(i);
		}

		return material().texture().spriteFinder().find(u * 0.25f, v * 0.25f);
	}

	@Override
	public BaseQuadEmitter uvSprite(@Nullable TextureAtlasSprite sprite, float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
		isSpriteInterpolated = false;
		spriteFloat(0, u0, v0);
		spriteFloat(1, u1, v1);
		spriteFloat(2, u2, v2);
		spriteFloat(3, u3, v3);

		if (sprite != null) {
			// TODO migrate 1.19.3: assertion?
			// assert material().texture().isAtlas() && material().texture().textureAsAtlas() == sprite.atlas();
			spriteId(((IndexedSprite) sprite).frex_index());
		}

		return this;
	}

	@Override
	public BaseQuadEmitter uv(int vertexIndex, float u, float v) {
		// This legacy method accepts interpolated coordinates
		// and so any usage forces us to de-normalize if we are not already.
		// Otherwise any subsequent reads or transformations could be inconsistent.

		if (!isSpriteInterpolated) {
			final var mat = material();

			if (mat.texture().isAtlas()) {
				final var atlasInfo = material().texture().spriteIndex();
				final var spriteId = spriteId();

				for (int i = 0; i < 4; ++i) {
					spriteFloat(i, atlasInfo.mapU(spriteId, spriteFloatU(i)), atlasInfo.mapV(spriteId, spriteFloatV(i)));
				}

				isSpriteInterpolated = true;
			}
		}

		spriteFloat(vertexIndex, u, v);
		return this;
	}

	@Override
	public BaseQuadEmitter spriteBake(TextureAtlasSprite sprite, int bakeFlags) {
		TextureHelper.bakeSprite(this, sprite, bakeFlags);
		return this;
	}

	public BaseQuadEmitter spriteId(int spriteId) {
		data[baseIndex + HEADER_SPRITE] = spriteId;
		return this;
	}

	@Override
	public void endVertex() {
		// NB: We don't worry about triangles here because we only
		// use this for API calls (which accept quads) or to transcode
		// render layers that have quads as the primitive type.

		// Auto-emit when we finish a quad.
		// NB: emit will call complete which will set vertex index to zero
		if (vertexIndex == 3) {
			emit();
		} else {
			++vertexIndex;
		}
	}

	@Override
	public void defaultColor(int i, int j, int k, int l) {
		// Mojang currently only uses this for outline rendering and
		// also it would be needlessly complicated to implement here.
		// We only render quads so should never see it.
		assert false : "fixedColor call encountered in quad rendering";
	}

	@Override
	public void unsetDefaultColor() {
		// Mojang currently only uses this for outline rendering and
		// also it would be needlessly complicated to implement here.
		// We only render quads so should never see it.
		assert false : "unfixColor call encountered in quad rendering";
	}

	@Override
	public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
		addVertex(x, y, z);
		setColor(MeshEncodingHelper.packColor(red, green, blue, alpha));
		setUv(u, v);
		setOverlayValue(overlay);
		setLight(light);
		setNormal(normalX, normalY, normalZ);
		endVertex();
	}

	@Override
	public VertexEmitter addVertex(float x, float y, float z) {
		pos(vertexIndex, x, y, z);
		return this;
	}

	@Override
	public VertexEmitter setColor(int color) {
		vertexColor(vertexIndex, color);
		return this;
	}

	@Override
	public VertexEmitter setUv(float u, float v) {
		uv(vertexIndex, u, v);
		return this;
	}

	@Override
	public VertexEmitter setUv1(int u, int v) {
		setOverlayValue(u, v);
		return this;
	}

	@Override
	public VertexEmitter setOverlay(int uv) {
		setOverlayValue(uv);
		return this;
	}

	protected void setOverlayValue(int uv) {
		final var mat = material();
		final var oMat = mat.withOverlay(uv);

		if (oMat != mat) {
			material(oMat);
		}
	}

	protected void setOverlayValue(int u, int v) {
		final var mat = material();
		final var oMat = mat.withOverlay(u, v);

		if (oMat != mat) {
			material(oMat);
		}
	}

	@Override
	public VertexEmitter setUv2(int block, int sky) {
		this.lightmap(vertexIndex, (block & 0xFF) | ((sky & 0xFF) << 8));
		return this;
	}

	@Override
	public VertexEmitter setLight(int lightmap) {
		this.lightmap(vertexIndex, lightmap);
		return this;
	}

	@Override
	public VertexEmitter setNormal(float x, float y, float z) {
		this.normal(vertexIndex, x, y, z);
		return this;
	}

	@Override
	public VertexEmitter setColor(int red, int green, int blue, int alpha) {
		return setColor(MeshEncodingHelper.packColor(red, green, blue, alpha));
	}

	@Override
	public VertexEmitter addVertex(Matrix4f mat, float x, float y, float z) {
		final float tx = mat.m00() * x + mat.m10() * y + mat.m20() * z + mat.m30();
		final float ty = mat.m01() * x + mat.m11() * y + mat.m21() * z + mat.m31();
		final float tz = mat.m02() * x + mat.m12() * y + mat.m22() * z + mat.m32();

		return this.addVertex(tx, ty, tz);
	}

	@Override
	public VertexEmitter setNormal(PoseStack.Pose pose, float x, float y, float z) {
		final var mat = pose.normal();
		final float tx = mat.m00() * x + mat.m10() * y + mat.m20() * z;
		final float ty = mat.m01() * x + mat.m11() * y + mat.m21() * z;
		final float tz = mat.m02() * x + mat.m12() * y + mat.m22() * z;

		return this.setNormal(tx, ty, tz);
	}
}
