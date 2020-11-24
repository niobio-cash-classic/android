/*
 * Copyright by the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nbr.core;

import java.math.*;
import java.util.*;

import org.bouncycastle.math.ec.*;

/**
 * A wrapper around ECPoint that delays decoding of the point for as long as possible. This is useful because point
 * encode/decode in Bouncy Castle is quite slow especially on Dalvik, as it often involves decompression/recompression.
 */
public class LazyECPoint {
	// If curve is set, bits is also set. If curve is unset, point is set and bits is unset. Point can be set along
	// with curve and bits when the cached form has been accessed and thus must have been converted.

	private final ECCurve curve;
	private final byte[] bits;
	private final boolean compressed;

	// This field is effectively final - once set it won't change again. However it can be set after
	// construction.
	private ECPoint point;

	public LazyECPoint(final byte[] bits) {
		this.curve = ECKey.CURVE_PARAMS.getCurve();
		this.bits = bits;
		this.compressed = ECKey.isPubKeyCompressed(bits);
	}

	public LazyECPoint(final ECCurve curve, final byte[] bits) {
		this.curve = curve;
		this.bits = bits;
		this.compressed = ECKey.isPubKeyCompressed(bits);
	}

	public LazyECPoint(final ECPoint point, final boolean compressed) {
		this.point = point.normalize();
		this.compressed = compressed;
		this.curve = null;
		this.bits = null;
	}

	public ECPoint add(final ECPoint b) {
		return get().add(b);
	}

	public boolean equals(final ECPoint other) {
		return get().equals(other);
	}

	// Delegated methods.

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		return Arrays.equals(getCanonicalEncoding(), ((LazyECPoint) o).getCanonicalEncoding());
	}

	public ECPoint get() {
		if (point == null) point = curve.decodePoint(bits);
		return point;
	}

	public ECFieldElement getAffineXCoord() {
		return get().getAffineXCoord();
	}

	public ECFieldElement getAffineYCoord() {
		return get().getAffineYCoord();
	}

	public ECCurve getCurve() {
		return get().getCurve();
	}

	public ECPoint getDetachedPoint() {
		return get().getDetachedPoint();
	}

	public byte[] getEncoded() {
		if (bits != null) return Arrays.copyOf(bits, bits.length);
		else return get().getEncoded(compressed);
	}

	public byte[] getEncoded(final boolean compressed) {
		if (compressed == isCompressed() && bits != null) return Arrays.copyOf(bits, bits.length);
		else return get().getEncoded(compressed);
	}

	public ECFieldElement getX() {
		return this.normalize().getXCoord();
	}

	public ECFieldElement getXCoord() {
		return get().getXCoord();
	}

	public ECFieldElement getY() {
		return this.normalize().getYCoord();
	}

	public ECFieldElement getYCoord() {
		return get().getYCoord();
	}

	public ECFieldElement getZCoord(final int index) {
		return get().getZCoord(index);
	}

	public ECFieldElement[] getZCoords() {
		return get().getZCoords();
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(getCanonicalEncoding());
	}

	public boolean isCompressed() {
		return compressed;
	}

	public boolean isInfinity() {
		return get().isInfinity();
	}

	public boolean isNormalized() {
		return get().isNormalized();
	}

	public boolean isValid() {
		return get().isValid();
	}

	public ECPoint multiply(final BigInteger k) {
		return get().multiply(k);
	}

	public ECPoint negate() {
		return get().negate();
	}

	public ECPoint normalize() {
		return get().normalize();
	}

	public ECPoint scaleX(final ECFieldElement scale) {
		return get().scaleX(scale);
	}

	public ECPoint scaleY(final ECFieldElement scale) {
		return get().scaleY(scale);
	}

	public ECPoint subtract(final ECPoint b) {
		return get().subtract(b);
	}

	public ECPoint threeTimes() {
		return get().threeTimes();
	}

	public ECPoint timesPow2(final int e) {
		return get().timesPow2(e);
	}

	public ECPoint twice() {
		return get().twice();
	}

	public ECPoint twicePlus(final ECPoint b) {
		return get().twicePlus(b);
	}

	private byte[] getCanonicalEncoding() {
		return getEncoded(true);
	}
}
