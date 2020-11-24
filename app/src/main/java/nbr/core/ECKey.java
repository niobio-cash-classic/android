package nbr.core;

import java.math.*;
import java.security.*;

import org.bouncycastle.asn1.x9.*;
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.ec.*;
import org.bouncycastle.crypto.generators.*;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.math.ec.*;

public class ECKey {

	public static final ECDomainParameters CURVE;

	private static final SecureRandom secureRandom;

	static final X9ECParameters CURVE_PARAMS = CustomNamedCurves.getByName("secp256r1");

	static {
		FixedPointUtil.precompute(CURVE_PARAMS.getG(), 12);
		CURVE = new ECDomainParameters(CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(),
				CURVE_PARAMS.getH());
		secureRandom = new SecureRandom();
	}

	public static LazyECPoint compressPoint(final LazyECPoint point) {
		return point.isCompressed() ? point : getPointWithCompression(point.get(), true);
	}

	public static LazyECPoint decompressPoint(final LazyECPoint point) {
		return !point.isCompressed() ? point : getPointWithCompression(point.get(), false);
	}

	/**
	 * Returns true if the given pubkey is in its compressed form.
	 */
	public static boolean isPubKeyCompressed(final byte[] encoded) {
		if (encoded.length == 33 && (encoded[0] == 0x02 || encoded[0] == 0x03)) return true;
		else if (encoded.length == 65 && encoded[0] == 0x04) return false;
		else throw new RuntimeException("isPubKeyCompressed");
	}

	private static LazyECPoint getPointWithCompression(final ECPoint point, final boolean compressed) {
		return new LazyECPoint(point, compressed);
	}

	protected final BigInteger priv; // A field element.

	protected final LazyECPoint pub;

	/**
	 * Generates an entirely new keypair. Point compression is used so the resulting public key will be 33 bytes (32 for
	 * the co-ordinate and 1 byte to represent the y bit).
	 */
	public ECKey() {
		this(secureRandom);
	}

	/**
	 * Generates an entirely new keypair with the given {@link SecureRandom} object. Point compression is used so the
	 * resulting public key will be 33 bytes (32 for the co-ordinate and 1 byte to represent the y bit).
	 */
	public ECKey(final SecureRandom secureRandom) {
		final ECKeyPairGenerator generator = new ECKeyPairGenerator();
		final ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(CURVE, secureRandom);
		generator.init(keygenParams);
		final AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
		final ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
		final ECPublicKeyParameters pubParams = (ECPublicKeyParameters) keypair.getPublic();
		priv = privParams.getD();
		pub = getPointWithCompression(pubParams.getQ(), true);
	}

	ECKey(final BigInteger priv, final LazyECPoint pub) {
		this.priv = priv;
		this.pub = pub;
	}

	/**
	 * Returns a copy of this key, but with the public point represented in uncompressed form. Normally you would never
	 * need this: it's for specialised scenarios or when backwards compatibility in encoded form is necessary.
	 */
	public ECKey decompress() {
		if (!pub.isCompressed()) return this;
		else return new ECKey(priv, getPointWithCompression(pub.get(), false));
	}

	public BigInteger getPrivKey() {
		return priv;
	}

	public byte[] getPubKey() {
		return pub.getEncoded();
	}
}
