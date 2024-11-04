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

package io.vram.frex.api.math;

import org.joml.Vector3f;
import io.vram.frex.api.mesh.QuadView;
import io.vram.frex.api.model.util.GeometryUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;

/**
 * Static routines of general utility for renderer implementations.
 * Renderers are not required to use these helpers, but they were
 * designed to be usable without the default renderer.
 */
public abstract class PackedVector3f {
	private PackedVector3f() { }

	// Translates normalized value from 0 to 2 and adds half a unit step for fast rounding via (int)
	private static final float HALF_UNIT_PLUS_ONE = 1f + 1f / 254f;

	// Avoid division
	private static final float DIVIDE_BY_127 = 1f / 127f;

	public static int pack(float x, float y, float z) {
		final int i = (int) ((x + HALF_UNIT_PLUS_ONE) * 0x7F);
		final int j = (int) ((y + HALF_UNIT_PLUS_ONE) * 0x7F00);
		final int k = (int) ((z + HALF_UNIT_PLUS_ONE) * 0x7F0000);

		// NB: 0x81 is -127 as byte
		return (i < 0 ? 0x81 : i > 0xFE ? 0x7F : ((i - 0x7F) & 0xFF))
				| (j < 0 ? 0x8100 : j > 0xFE00 ? 0x7F00 : ((j - 0x7F00) & 0xFF00))
				| (k < 0 ? 0x810000 : k > 0xFE0000 ? 0x7F0000 : ((k - 0x7F0000) & 0xFF0000));
	}

	public static int pack(float x, float y, float z, boolean inverted) {
		return pack(x, y, z) | (inverted ? 0x1000000 : 0);
	}

	public static float unpackX(int packedVector) {
		return ((byte) (packedVector & 0xFF)) * DIVIDE_BY_127;
	}

	public static float unpackY(int packedVector) {
		return ((byte) ((packedVector >>> 8) & 0xFF)) * DIVIDE_BY_127;
	}

	public static float unpackZ(int packedVector) {
		return ((byte) ((packedVector >>> 16) & 0xFF)) * DIVIDE_BY_127;
	}

	public static boolean unpackInverted(int packedVector) {
		return (packedVector & 0x1000000) != 0;
	}

	/** Returns value -127 to +127. */
	public static int unpackByteX(int packedVector) {
		return (byte) (packedVector & 0xFF);
	}

	/** Returns value -127 to +127. */
	public static int unpackByteY(int packedVector) {
		return (byte) ((packedVector >>> 8) & 0xFF);
	}

	/** Returns value -127 to +127. */
	public static int unpackByteZ(int packedVector) {
		return (byte) ((packedVector >>> 16) & 0xFF);
	}

	public static Vector3f unpackTo(int packedVector, Vector3f target) {
		target.set(
				unpackX(packedVector),
				unpackY(packedVector),
				unpackZ(packedVector));

		return target;
	}

	/**
	 * Computes the face normal of the given quad and saves it in the provided non-null vector.
	 * If {@link QuadView#nominalFace()} is set will optimize by confirming quad is parallel to that
	 * face and, if so, use the standard normal for that face direction.
	 *
	 * <p>Will work with triangles also. Assumes counter-clockwise winding order, which is the norm.
	 * Expects convex quads with all points co-planar.
	 */
	public static int computePackedFaceNormal(QuadView q) {
		final Direction nominalFace = q.nominalFace();

		if (GeometryUtil.isQuadParallelToFace(nominalFace, q)) {
			final Vec3i vec = nominalFace.getNormal();
			return pack(vec.getX(), vec.getY(), vec.getZ());
		}

		final float x0 = q.x(0);
		final float y0 = q.y(0);
		final float z0 = q.z(0);
		final float x1 = q.x(1);
		final float y1 = q.y(1);
		final float z1 = q.z(1);
		final float x2 = q.x(2);
		final float y2 = q.y(2);
		final float z2 = q.z(2);
		final float x3 = q.x(3);
		final float y3 = q.y(3);
		final float z3 = q.z(3);

		final float dx0 = x2 - x0;
		final float dy0 = y2 - y0;
		final float dz0 = z2 - z0;
		final float dx1 = x3 - x1;
		final float dy1 = y3 - y1;
		final float dz1 = z3 - z1;

		float normX = dy0 * dz1 - dz0 * dy1;
		float normY = dz0 * dx1 - dx0 * dz1;
		float normZ = dx0 * dy1 - dy0 * dx1;

		final float l = (float) Math.sqrt(normX * normX + normY * normY + normZ * normZ);

		if (l != 0) {
			final float inv = 1f / l;
			normX *= inv;
			normY *= inv;
			normZ *= inv;
		}

		return pack(normX, normY, normZ);
	}

	public static final int POSITIVE_X = pack(1, 0, 0);
	public static final int NEGATIVE_X = pack(-1, 0, 0);
	public static final int POSITIVE_Y = pack(0, 1, 0);
	public static final int NEGATIVE_Y = pack(0, -1, 0);
	public static final int POSITIVE_Z = pack(0, 0, 1);
	public static final int NEGATIVE_Z = pack(0, 0, -1);
}
