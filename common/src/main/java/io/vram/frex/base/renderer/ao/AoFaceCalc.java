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

package io.vram.frex.base.renderer.ao;

import java.util.function.IntBinaryOperator;
import io.vram.frex.api.math.FixedMath255;

/**
 * Handles vanilla-style calculations for ao and light blending.
 */
public class AoFaceCalc {
	int aoBottomRight;
	int aoBottomLeft;
	int aoTopLeft;
	int aoTopRight;

	int blockBottomRight;
	int blockBottomLeft;
	int blockTopLeft;
	int blockTopRight;

	int skyBottomRight;
	int skyBottomLeft;
	int skyTopLeft;
	int skyTopRight;

	/**
	 * Independent minimum of packed components.
	 */
	public static int min(int x, int y) {
		final int s = Math.min(x & 0x00FF0000, y & 0x00FF0000);
		final int b = Math.min(x & 0xFF, y & 0xFF);
		return s | b;
	}

	/**
	 * Independent maximum of packed components.
	 */
	public static int max(int x, int y) {
		final int s = Math.max(x & 0x00FF0000, y & 0x00FF0000);
		final int b = Math.max(x & 0xFF, y & 0xFF);
		return s | b;
	}

	/**
	 * Vanilla code excluded missing light values from mean but was not isotropic.
	 * Still need to substitute or edges are too dark but consistently use the min
	 * value from all four samples.
	 */
	private static int meanBrightness(int a, int b, int c, int d) {
		int missingVal = 0x0FFFFFFF;
		final IntBinaryOperator func = AoFaceCalc::min;
		int missingCount = 0;
		int total = 0;

		if (a == AoFaceData.OPAQUE) {
			missingCount++;
		} else {
			total += a;

			missingVal = func.applyAsInt(missingVal, a);
		}

		if (b == AoFaceData.OPAQUE) {
			missingCount++;
		} else {
			total += b;
			missingVal = func.applyAsInt(missingVal, b);
		}

		if (c == AoFaceData.OPAQUE) {
			missingCount++;
		} else {
			total += c;
			missingVal = func.applyAsInt(missingVal, c);
		}

		if (d == AoFaceData.OPAQUE) {
			missingCount++;
		} else {
			total += d;
			missingVal = func.applyAsInt(missingVal, d);
		}

		assert missingCount < 4 : "Computing light for four occluding neighbors?";

		// bitwise divide by 4, clamp to expected (positive) range, round up
		return (total + missingVal * missingCount + 2) >> 2 & 16711935;
	}

	public void compute(AoFaceData input) {
		aoTopLeft = input.aoTopLeft;
		aoTopRight = input.aoTopRight;
		aoBottomLeft = input.aoBottomLeft;
		aoBottomRight = input.aoBottomRight;

		int l = meanBrightness(input.right, input.bottom, input.bottomRight, input.center);
		blockBottomRight = l & 0xFFFF;
		skyBottomRight = (l >>> 16) & 0xFFFF;

		l = meanBrightness(input.left, input.bottom, input.bottomLeft, input.center);
		blockBottomLeft = l & 0xFFFF;
		skyBottomLeft = (l >>> 16) & 0xFFFF;

		l = meanBrightness(input.left, input.top, input.topLeft, input.center);
		blockTopLeft = l & 0xFFFF;
		skyTopLeft = (l >>> 16) & 0xFFFF;

		l = meanBrightness(input.right, input.top, input.topRight, input.center);
		blockTopRight = l & 0xFFFF;
		skyTopRight = (l >>> 16) & 0xFFFF;
	}

	public int weightedBlockLight(int packedWeights) {
		final var sum = (
				blockBottomRight * (packedWeights & 0xFF)
				+ blockBottomLeft * ((packedWeights >> 8) & 0xFF)
				+ blockTopLeft * ((packedWeights >> 16) & 0xFF)
				+ blockTopRight * ((packedWeights >> 24) & 0xFF)
		) + FixedMath255.UNIT_VALUE;

		// In rare cases we exceed the shifted unit value of 0xFFFF
		// and if that happen the shifted result will typically be zero
		// or greatly reduced.  So to catch these we clamp at 0xFFFF.
		return ((sum > 0xFFFF ? 0xFFFF : sum) >> FixedMath255.UNIT_SHIFT) & 0xFF;
	}

	public int maxBlockLight(int oldMax) {
		final int i = blockBottomRight > blockBottomLeft ? blockBottomRight : blockBottomLeft;
		final int j = blockTopLeft > blockTopRight ? blockTopLeft : blockTopRight;
		return Math.max(oldMax, i > j ? i : j);
	}

	public int weightedSkyLight(int packedWeights) {
		return (
					(
						skyBottomRight * (packedWeights & 0xFF)
						+ skyBottomLeft * ((packedWeights >> 8) & 0xFF)
						+ skyTopLeft * ((packedWeights >> 16) & 0xFF)
						+ skyTopRight * ((packedWeights >> 24) & 0xFF)
					) + FixedMath255.UNIT_VALUE
				) >> FixedMath255.UNIT_SHIFT & 0xFF;
	}

	public int maxSkyLight(int oldMax) {
		final int i = skyBottomRight > skyBottomLeft ? skyBottomRight : skyBottomLeft;
		final int j = skyTopLeft > skyTopRight ? skyTopLeft : skyTopRight;
		return Math.max(oldMax, i > j ? i : j);
	}

	public int weightedCombinedLight(int packedWeights) {
		return (weightedSkyLight(packedWeights) << 16) | weightedBlockLight(packedWeights);
	}

	public int weigtedAo(int packedWeights) {
		return Math.min(0xFFFF, (
					(
						aoBottomRight * (packedWeights & 0xFF)
						+ aoBottomLeft * ((packedWeights >> 8) & 0xFF)
						+ aoTopLeft * ((packedWeights >> 16) & 0xFF)
						+ aoTopRight * ((packedWeights >> 24) & 0xFF)
					) + FixedMath255.HALF_VALUE
				)) >> FixedMath255.UNIT_SHIFT & 0xFF;
	}

	// PERF: use integer weights
	public void weightedMean(AoFaceCalc in0, float w0, AoFaceCalc in1, float w1) {
		aoBottomRight = Math.round(in0.aoBottomRight * w0 + in1.aoBottomRight * w1);
		aoBottomLeft = Math.round(in0.aoBottomLeft * w0 + in1.aoBottomLeft * w1);
		aoTopLeft = Math.round(in0.aoTopLeft * w0 + in1.aoTopLeft * w1);
		aoTopRight = Math.round(in0.aoTopRight * w0 + in1.aoTopRight * w1);

		blockBottomRight = Math.round(in0.blockBottomRight * w0 + in1.blockBottomRight * w1);
		blockBottomLeft = Math.round(in0.blockBottomLeft * w0 + in1.blockBottomLeft * w1);
		blockTopLeft = Math.round(in0.blockTopLeft * w0 + in1.blockTopLeft * w1);
		blockTopRight = Math.round(in0.blockTopRight * w0 + in1.blockTopRight * w1);

		skyBottomRight = Math.round(in0.skyBottomRight * w0 + in1.skyBottomRight * w1);
		skyBottomLeft = Math.round(in0.skyBottomLeft * w0 + in1.skyBottomLeft * w1);
		skyTopLeft = Math.round(in0.skyTopLeft * w0 + in1.skyTopLeft * w1);
		skyTopRight = Math.round(in0.skyTopRight * w0 + in1.skyTopRight * w1);
	}
}
