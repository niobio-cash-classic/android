package nbr.core;

import java.io.*;
import java.math.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;
import java.util.*;

import org.bouncycastle.jce.*;
import org.bouncycastle.jce.spec.*;

// https://bitcoin.stackexchange.com/questions/44024/get-uncompressed-public-key-from-compressed-form
// https://stackoverflow.com/questions/28172710/java-compact-representation-of-ecc-publickey
public class Curvy {

	static final BigInteger MODULUS = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F",
			16);
	static final BigInteger CURVE_A = new BigInteger("0");
	static final BigInteger CURVE_B = new BigInteger("7");

	// Given a 33-byte compressed public key, this returns a 65-byte uncompressed key.
	private static final byte UNCOMPRESSED_POINT_INDICATOR = 0x04;

	static ECNamedCurveParameterSpec SPEC = ECNamedCurveTable.getParameterSpec("secp256r1");

	public static byte[] compressedToUncompressed(final byte[] compKey) throws IOException {
		final org.bouncycastle.math.ec.ECPoint point = SPEC.getCurve().decodePoint(compKey);
		final byte[] x = point.getXCoord().getEncoded();
		final byte[] y = point.getYCoord().getEncoded();
		// concat 0x04, x, and y, make sure x and y has 32-bytes:

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(new byte[] { 0x04 });
		outputStream.write(x);
		outputStream.write(y);
		final byte c[] = outputStream.toByteArray();
		return c;
	}

	public static String compressPubKey(final BigInteger pubKey) {
		final String pubKeyYPrefix = pubKey.testBit(0) ? "03" : "02";
		final String pubKeyHex = pubKey.toString(16);
		final String pubKeyX = pubKeyHex.substring(0, 64);
		return pubKeyYPrefix + pubKeyX;
	}

	public static byte[] decompressPubkey(final byte[] compKey) {
		// Check array length and type indicator byte
		if (compKey.length != 33 || compKey[0] != 2 && compKey[0] != 3) throw new IllegalArgumentException();

		final byte[] xCoordBytes = Arrays.copyOfRange(compKey, 1, compKey.length);
		final BigInteger xCoord = new BigInteger(1, xCoordBytes); // Range [0, 2^256)

		BigInteger temp = xCoord.pow(2).add(CURVE_A);
		temp = sqrtMod(temp.add(CURVE_B));
		final boolean tempIsOdd = temp.testBit(0);
		final boolean yShouldBeOdd = compKey[0] == 3;
		if (tempIsOdd != yShouldBeOdd) temp = temp.negate().mod(MODULUS);
		final BigInteger yCoord = temp;

		// Copy the x coordinate into the new
		// uncompressed key, and change the type byte
		final byte[] result = Arrays.copyOf(compKey, 65);
		result[0] = 4;

		// Carefully copy the y coordinate into uncompressed key
		final byte[] yCoordBytes = yCoord.toByteArray();
		for (int i = 0; i < 32 && i < yCoordBytes.length; i++)
			result[result.length - 1 - i] = yCoordBytes[yCoordBytes.length - 1 - i];

		return result;
	}

	// Given x, this returns a value y such that y^2 % MODULUS == x.
	public static ECPublicKey fromUncompressedPoint(final byte[] uncompressedPoint, final ECParameterSpec params)
			throws Exception {

		int offset = 0;
		if (uncompressedPoint[offset++] != UNCOMPRESSED_POINT_INDICATOR) {
			throw new IllegalArgumentException("Invalid uncompressedPoint encoding, no uncompressed point indicator");
		}

		final int keySizeBytes = (params.getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

		if (uncompressedPoint.length != 1 + 2 * keySizeBytes) {
			throw new IllegalArgumentException("Invalid uncompressedPoint encoding, not the correct size");
		}

		final BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
		offset += keySizeBytes;
		final BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressedPoint, offset, offset + keySizeBytes));
		final ECPoint w = new ECPoint(x, y);
		final ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(w, params);
		final KeyFactory keyFactory = KeyFactory.getInstance("EC");
		return (ECPublicKey) keyFactory.generatePublic(ecPublicKeySpec);
	}

	public static void main(final String[] args) throws Exception {

		// just for testing

		final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
		kpg.initialize(163);

		for (int i = 0; i < 1_000; i++) {
			final KeyPair ecKeyPair = kpg.generateKeyPair();

			final ECPublicKey ecPublicKey = (ECPublicKey) ecKeyPair.getPublic();
			final ECPublicKey retrievedEcPublicKey = fromUncompressedPoint(toUncompressedPoint(ecPublicKey),
					ecPublicKey.getParams());
			if (!Arrays.equals(retrievedEcPublicKey.getEncoded(), ecPublicKey.getEncoded())) {
				throw new IllegalArgumentException("Whoops");
			}
		}
	}

	public static byte[] toUncompressedPoint(final ECPublicKey publicKey) {

		final int keySizeBytes = (publicKey.getParams().getOrder().bitLength() + Byte.SIZE - 1) / Byte.SIZE;

		final byte[] uncompressedPoint = new byte[1 + 2 * keySizeBytes];
		int offset = 0;
		uncompressedPoint[offset++] = 0x04;

		final byte[] x = publicKey.getW().getAffineX().toByteArray();
		if (x.length <= keySizeBytes) {
			System.arraycopy(x, 0, uncompressedPoint, offset + keySizeBytes - x.length, x.length);
		} else if (x.length == keySizeBytes + 1 && x[0] == 0) {
			System.arraycopy(x, 1, uncompressedPoint, offset, keySizeBytes);
		} else {
			throw new IllegalStateException("x value is too large");
		}
		offset += keySizeBytes;

		final byte[] y = publicKey.getW().getAffineY().toByteArray();
		if (y.length <= keySizeBytes) {
			System.arraycopy(y, 0, uncompressedPoint, offset + keySizeBytes - y.length, y.length);
		} else if (y.length == keySizeBytes + 1 && y[0] == 0) {
			System.arraycopy(y, 1, uncompressedPoint, offset, keySizeBytes);
		} else {
			throw new IllegalStateException("y value is too large");
		}

		return uncompressedPoint;
	}

	static BigInteger sqrtMod(final BigInteger value) {
		assert (MODULUS.intValue() & 3) == 3;
		final BigInteger pow = MODULUS.add(BigInteger.ONE).shiftRight(2);
		final BigInteger result = value.modPow(pow, MODULUS);
		assert result.pow(2).mod(MODULUS).equals(value);
		return result;
	}
}