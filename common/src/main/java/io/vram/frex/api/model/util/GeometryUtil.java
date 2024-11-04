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

package io.vram.frex.api.model.util;

import static net.minecraft.util.Mth.equal;

import org.joml.Vector3f;
import io.vram.frex.api.math.PackedVector3f;
import io.vram.frex.api.mesh.QuadView;
import net.minecraft.client.renderer.FaceInfo.Constants;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class GeometryUtil {
	private GeometryUtil() { }

	/**
	 * set when a quad touches all four corners of a unit cube.
	 */
	public static final int CUBIC_FLAG = 1;
	/**
	 * set when a quad is parallel to (but not necessarily on) a its light face.
	 */
	public static final int AXIS_ALIGNED_FLAG = CUBIC_FLAG << 1;
	/**
	 * set when a quad is coplanar with its light face. Implies {@link #AXIS_ALIGNED_FLAG}
	 */
	public static final int LIGHT_FACE_FLAG = AXIS_ALIGNED_FLAG << 1;
	/**
	 * how many bits quad header encoding should reserve for encoding geometry flags.
	 */
	public static final int FLAG_BIT_COUNT = 3;

	private static final float EPS_MIN = 0.0001f;
	private static final float EPS_MAX = 1.0f - EPS_MIN;

	/**
	 * Analyzes the quad and returns a value with some combination
	 * of {@link #AXIS_ALIGNED_FLAG}, {@link #LIGHT_FACE_FLAG} and {@link #CUBIC_FLAG}.
	 * Intended use is to optimize lighting when the geometry is regular.
	 * Expects convex quads with all points co-planar.
	 */
	public static int computeShapeFlags(QuadView quad) {
		final Direction lightFace = quad.lightFace();
		int bits = 0;

		if (isQuadParallelToFace(lightFace, quad)) {
			bits |= AXIS_ALIGNED_FLAG;

			if (isParallelQuadOnFace(lightFace, quad)) {
				bits |= LIGHT_FACE_FLAG;
			}
		}

		if (isQuadCubic(lightFace, quad)) {
			bits |= CUBIC_FLAG;
		}

		return bits;
	}

	/**
	 * Returns true if quad is parallel to the given face.
	 * Does not validate quad winding order.
	 * Expects convex quads with all points co-planar.
	 */
	public static boolean isQuadParallelToFace(Direction face, QuadView quad) {
		if (face == null) {
			return false;
		}

		final int i = face.getAxis().ordinal();
		final float val = quad.posByIndex(0, i);
		return equal(val, quad.posByIndex(1, i)) && equal(val, quad.posByIndex(2, i)) && equal(val, quad.posByIndex(3, i));
	}

	/**
	 * True if quad - already known to be parallel to a face - is actually coplanar with it.
	 * For compatibility with vanilla resource packs, also true if quad is outside the face.
	 *
	 * <p>Test will be unreliable if not already parallel, use {@link #isQuadParallelToFace(Direction, QuadView)}
	 * for that purpose. Expects convex quads with all points co-planar.
	 */
	public static boolean isParallelQuadOnFace(Direction lightFace, QuadView quad) {
		if (lightFace == null) return false;

		final float x = quad.posByIndex(0, lightFace.getAxis().ordinal());
		return lightFace.getAxisDirection() == AxisDirection.POSITIVE ? x >= EPS_MAX : x <= EPS_MIN;
	}

	/**
	 * Returns true if quad is truly a quad (not a triangle) and fills a full block cross-section.
	 * If known to be true, allows use of a simpler/faster AO lighting algorithm.
	 *
	 * <p>Does not check if quad is actually coplanar with the light face, nor does it check that all
	 * quad vertices are coplanar with each other.
	 *
	 * <p>Expects convex quads with all points co-planar.
	 *
	 * @param lightFace MUST be non-null.
	 */
	public static boolean isQuadCubic(Direction lightFace, QuadView quad) {
		if (lightFace == null) {
			return false;
		}

		int a, b;

		switch (lightFace) {
			case EAST:
			case WEST:
				a = 1;
				b = 2;
				break;
			case UP:
			case DOWN:
				a = 0;
				b = 2;
				break;
			case SOUTH:
			case NORTH:
				a = 1;
				b = 0;
				break;
			default:
				// handle WTF case
				return false;
		}

		return confirmSquareCorners(a, b, quad);
	}

	/**
	 * Used by {@link #isQuadCubic(Direction, QuadView)}.
	 * True if quad touches all four corners of unit square.
	 *
	 * <p>For compatibility with resource packs that contain models with quads exceeding
	 * block boundaries, considers corners outside the block to be at the corners.
	 */
	private static boolean confirmSquareCorners(int aCoordinate, int bCoordinate, QuadView quad) {
		int flags = 0;

		for (int i = 0; i < 4; i++) {
			final float a = quad.posByIndex(i, aCoordinate);
			final float b = quad.posByIndex(i, bCoordinate);

			if (a <= EPS_MIN) {
				if (b <= EPS_MIN) {
					flags |= 1;
				} else if (b >= EPS_MAX) {
					flags |= 2;
				} else {
					return false;
				}
			} else if (a >= EPS_MAX) {
				if (b <= EPS_MIN) {
					flags |= 4;
				} else if (b >= EPS_MAX) {
					flags |= 8;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		return flags == 15;
	}

	/**
	 * Identifies the face to which the quad is most closely aligned.
	 * This mimics the value that {@link BakedQuad#getDirection()} returns, and is
	 * used in the vanilla renderer for all diffuse lighting.
	 *
	 * <p>Derived from the quad face normal and expects convex quads with all points co-planar.
	 */
	public static int lightFaceId(QuadView quad) {
		final int packedNormal = quad.packedFaceNormal();
		final float x = PackedVector3f.unpackX(packedNormal);
		final float y = PackedVector3f.unpackY(packedNormal);
		final float z = PackedVector3f.unpackZ(packedNormal);

		switch (GeometryUtil.longestAxis(x, y, z)) {
			case X:
				return x > 0 ? Constants.MAX_X : Constants.MIN_X;

			case Y:
				return y > 0 ? Constants.MAX_Y : Constants.MIN_Y;

			case Z:
				return z > 0 ? Constants.MAX_Z : Constants.MIN_Z;

			default:
				// handle WTF case
				return Constants.MAX_Y;
		}
	}

	/**
	 * Simple 4-way compare, doesn't handle NaN values.
	 */
	public static float min(float a, float b, float c, float d) {
		final float x = a < b ? a : b;
		final float y = c < d ? c : d;
		return x < y ? x : y;
	}

	/**
	 * Simple 4-way compare, doesn't handle NaN values.
	 */
	public static float max(float a, float b, float c, float d) {
		final float x = a > b ? a : b;
		final float y = c > d ? c : d;
		return x > y ? x : y;
	}

	/**
	 * @see #longestAxis(float, float, float)
	 */
	public static Axis longestAxis(Vector3f vec) {
		return longestAxis(vec.x(), vec.y(), vec.z());
	}

	/**
	 * Identifies the largest (max absolute magnitude) component (X, Y, Z) in the given vector.
	 */
	public static Axis longestAxis(float normalX, float normalY, float normalZ) {
		Axis result = Axis.Y;
		float longest = Math.abs(normalY);
		final float a = Math.abs(normalX);

		if (a > longest) {
			result = Axis.X;
			longest = a;
		}

		return Math.abs(normalZ) > longest ? Axis.Z : result;
	}
}
