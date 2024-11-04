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

package io.vram.frex.api.mesh;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.vram.frex.api.buffer.QuadEmitter;
import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.api.math.PackedVector3f;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public interface QuadView {
	/** Count of integers in a conventional (un-modded) block or item vertex. */
	int VANILLA_VERTEX_STRIDE = DefaultVertexFormat.BLOCK.getVertexSize() / 4;

	/** Count of integers in a conventional (un-modded) block or item quad. */
	int VANILLA_QUAD_STRIDE = VANILLA_VERTEX_STRIDE * 4;

	/**
	 * Copies all quad properties, including material, to the given {@link QuadEmitter} instance.
	 * Must be used before calling {link QuadEmitter#emit()} on the target instance.
	 * Meant for re-texturing, analysis and static transformation use cases.
	 */
	void copyTo(QuadEmitter target);

	/**
	 * Retrieves the quad color index serialized with the quad.
	 */
	int colorIndex();

	/**
	 * Equivalent to {@link BakedQuad#getDirection()}. This is the face used for vanilla lighting
	 * calculations and will be the block face to which the quad is most closely aligned. Always
	 * the same as cull face for quads that are on a block face, but never null.
	 */
	@NotNull
	Direction lightFace();

	/**
	 * If non-null, quad should not be rendered in-world if the
	 * opposite face of a neighbor block occludes it.
	 *
	 * @see QuadEmitter#cullFace(Direction)
	 */
	@Nullable Direction cullFace();

	/**
	 * See {@link QuadEmitter#nominalFace(Direction)}.
	 */
	Direction nominalFace();

	/**
	 * Normal of the quad as implied by geometry. Will be invalid
	 * if quad vertices are not co-planar.  Typically computed lazily
	 * on demand and not encoded.
	 *
	 * <p>Not typically needed by models. Exposed to enable standard lighting
	 * utility functions for use by renderers.
	 */
	int packedFaceNormal();

	/**
	 * Retrieves the integer tag encoded with this quad via {@link QuadEmitter#tag(int)}.
	 * Will return zero if no tag was set.  For use by models.
	 */
	int tag();

	/**
	 * Pass a non-null target to avoid allocation - will be returned with values.
	 * Otherwise returns a new instance.
	 */
	Vector3f copyPos(int vertexIndex, @Nullable Vector3f target);

	/**
	 * Convenience: access x, y, z by index 0-2.
	 */
	float posByIndex(int vertexIndex, int coordinateIndex);

	/**
	 * Geometric position, x coordinate.
	 */
	float x(int vertexIndex);

	/**
	 * Geometric position, y coordinate.
	 */
	float y(int vertexIndex);

	/**
	 * Geometric position, z coordinate.
	 */
	float z(int vertexIndex);

	/**
	 * If false, no vertex normal was provided.
	 * Lighting should use face normal in that case.
	 */
	boolean hasNormal(int vertexIndex);

	int packedNormal(int vertexIndex);

	/**
	 * Pass a non-null target to avoid allocation - will be returned with values.
	 * Otherwise returns a new instance. Returns null if normal not present.
	 */
	@Nullable
	default Vector3f copyNormal(int vertexIndex, @Nullable Vector3f target) {
		if (hasNormal(vertexIndex)) {
			if (target == null) {
				target = new Vector3f();
			}

			return PackedVector3f.unpackTo(this.packedNormal(vertexIndex), target);
		} else {
			return null;
		}
	}

	/**
	 * Minimum block brightness. Zero if not set.
	 */
	int lightmap(int vertexIndex);

	/**
	 * Retrieves the material serialized with the quad.
	 */
	RenderMaterial material();

	/**
	 * Reads baked vertex data and outputs standard baked quad
	 * vertex data in the given array and location.
	 *
	 * @param spriteIndex The sprite to be used for the quad.
	 * Behavior for values &gt; 0 is currently undefined.
	 *
	 * @param target Target array for the baked quad data.
	 *
	 * @param targetIndex Starting position in target array - array must have
	 * at least 28 elements available at this index.
	 *
	 * @param isItem If true, will output vertex normals. Otherwise will output
	 * lightmaps, per Minecraft vertex formats for baked models.
	 */
	void toVanilla(int[] target, int targetIndex);

	/**
	 * Generates a new BakedQuad instance with texture
	 * coordinates and colors from the given sprite.
	 *
	 * @param spriteIndex The sprite to be used for the quad.
	 * Behavior for {@code spriteIndex > 0} is currently undefined.
	 *
	 * @param sprite  {@link QuadEmitter} does not serialize sprites
	 * so the sprite must be provided by the caller.
	 *
	 * @param isItem If true, will output vertex normals. Otherwise will output
	 * lightmaps, per Minecraft vertex formats for baked models.
	 *
	 * @return A new baked quad instance with the closest-available appearance
	 * supported by vanilla features. Will retain emissive light maps, for example,
	 * but the standard Minecraft renderer will not use them.
	 */
	default BakedQuad toBakedQuad(TextureAtlasSprite sprite) {
		final int[] vertexData = new int[VANILLA_QUAD_STRIDE];
		toVanilla(vertexData, 0);
		return new BakedQuad(vertexData, colorIndex(), lightFace(), sprite, true);
	}

	/**
	 * Retrieve vertex color.
	 */
	int vertexColor(int vertexIndex);

	/**
	 * Retrieve horizontal texture coordinates.
	 *
	 * <p>For sprite atlas textures, the coordinates will be
	 * relative to the atlas texture, not relative to the sprite.
	 */
	float u(int vertexIndex);

	/**
	 * Retrieve vertical texture coordinate.
	 *
	 * <p>For sprite atlas textures, the coordinates will be
	 * relative to the atlas texture, not relative to the sprite.
	 */
	float v(int vertexIndex);

	/**
	 * Retrieve sprite-relative horizontal texture coordinates.
	 *
	 * <p>For sprite atlas textures, the coordinates will be
	 * relative to the sprite texture, not relative to the atlas.
	 *
	 * <p>For non-atlas textures, the result will be the
	 * same as #{@link #u(int)}
	 */
	float uSprite(int vertexIndex);

	/**
	 * Retrieve sprite-relative vertical texture coordinate.
	 *
	 * <p>For sprite atlas textures, the coordinates will be
	 * relative to the sprite texture, not relative to the atlas.
	 *
	 * <p>For non-atlas textures, the result will be the
	 * same as #{@link #v(int)}
	 */
	float vSprite(int vertexIndex);

	/**
	 * If false, no vertex tangent was provided.
	 * Lighting will use automatically computed tangents.
	 */
	boolean hasTangent(int vertexIndex);

	int packedTangent(int vertexIndex);

	/**
	 * Pass a non-null target to avoid allocation - will be returned with values.
	 * Otherwise returns a new instance. Returns null if tangent not present.
	 */
	@Nullable
	Vector3f copyTangent(int vertexIndex, @Nullable Vector3f target);
}
