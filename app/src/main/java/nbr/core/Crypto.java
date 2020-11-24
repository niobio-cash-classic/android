package nbr.core;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.*;
import java.math.*;
import java.nio.charset.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;

import org.bouncycastle.util.encoders.Base64;
import org.json.*;

public class Crypto {

	private static final Signature ecdsa;
	private static final MessageDigest sha256;
	private static final ECGenParameterSpec ecSpec;
	private static final ECParameterSpec ecParameterSpec;

	static {
		try {
			//Security.addProvider(new BouncyCastleProvider());
			Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
			sha256 = MessageDigest.getInstance("SHA-256");
			ecdsa = Signature.getInstance("SHA256withECDSA");
			ecSpec = new ECGenParameterSpec("secp256r1");

			final AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
			params.init(ecSpec);
			ecParameterSpec = params.getParameterSpec(ECParameterSpec.class);

		} catch (final NoSuchAlgorithmException | InvalidParameterSpecException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public static PublicKey getPublicKeyFromString(final String pubKeyStr) throws Exception {
//		final AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC", "BC");
//		parameters.init(ecSpec);
//		final ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
//		return Curvy.fromUncompressedPoint(Curvy.decompressPubkey(new BigInteger(publicKey, 16).toByteJSONArrayay()),
//				ecParameters);
//		final byte[] keyBytes = new BigInteger(publicKey, 16).toByteJSONArrayay();
//		return getPublicKeyFromBytes(keyBytes);
		// Util.bigIntegerToBytes(new BigInteger(pubKeyStr, 16), 33);
		// Util.decodePubKey(pubKeyStr);
		final byte[] pubBytes = Base64.decode(pubKeyStr);
		final LazyECPoint pub = new LazyECPoint(pubBytes);
		ECKey ecKeypair = new ECKey(null, pub);
		ecKeypair = ecKeypair.decompress();
		final ECPublicKey ecPubKey = Curvy.fromUncompressedPoint(ecKeypair.getPubKey(), ecParameterSpec);
		return ecPubKey;
	}

	public static synchronized BigInteger shaMine(final JSONObject o) throws IOException {
		return new BigInteger(1, sha256.digest(sha256.digest(Util.serialize(o))));
	}

	public static byte[] sign(final PrivateKey privateKey, final JSONObject tx)
			throws InvalidKeyException, SignatureException {
		synchronized (ecdsa) {
			ecdsa.initSign(privateKey);
			ecdsa.update(tx.toString().getBytes(StandardCharsets.UTF_8));
			return ecdsa.sign();
		}
	}

	public static boolean verify(final String publicKeyString, final JSONObject tx, final String signatureString)
			throws Exception {
		final PublicKey publicKey = getPublicKeyFromString(publicKeyString);
		final byte[] signature = Base64.decode(signatureString);
		synchronized (ecdsa) {
			ecdsa.initVerify(publicKey);
			ecdsa.update(Util.serialize(tx));
			return ecdsa.verify(signature);
		}
	}

	private static PrivateKey getPrivateKeyFromBytes(final byte[] keyBytes) {
		final ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(1, keyBytes), ecParameterSpec);
		try {
			final KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
			return keyFactory.generatePrivate(privateKeySpec);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static PrivateKey getPrivateKey(final String privKeyStr) throws Exception {
		final byte[] privBytes = Base64.decode(privKeyStr);

		final BigInteger priv = new BigInteger(privBytes);
		final PrivateKey privKey = getPrivateKeyFromBytes(privBytes);

		return privKey;
	}

	public static synchronized String sha(final JSONArray a) throws IOException {
		return Util.targetToString(new BigInteger(1, sha256.digest(sha256.digest(Util.serialize(a)))));
	}

	// blocks (with txs) has a special treatment in hash
	public static synchronized String sha(final JSONObject o) throws IOException, JSONException {
		JSONArray txs = null;
		if (o.has("txs")) {
			txs = o.getJSONArray("txs");
			o.remove("txs");
			o.put("txsHash", Crypto.sha(txs));
		}

		final String sha = Util.targetToString(new BigInteger(1, sha256.digest(sha256.digest(Util.serialize(o)))));

		if (txs != null) {
			o.put("txs", txs);
		}

		return sha;
	}

	private static byte hexToByte(final String hexString) {
		final int firstDigit = toDigit(hexString.charAt(0));
		final int secondDigit = toDigit(hexString.charAt(1));
		return (byte) ((firstDigit << 4) + secondDigit);
	}

	private static int toDigit(final char hexChar) {
		final int digit = Character.digit(hexChar, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Invalid Hexadecimal Character: " + hexChar);
		}
		return digit;
	}

	private static byte[] decodeHexString(final String hexString) {
		if (hexString.length() % 2 == 1) {
			throw new IllegalArgumentException("Invalid hexadecimal String supplied.");
		}

		final byte[] bytes = new byte[hexString.length() / 2];
		for (int i = 0; i < hexString.length(); i += 2) {
			bytes[i / 2] = hexToByte(hexString.substring(i, i + 2));
		}
		return bytes;
	}

	public static String encodeHexString(final byte[] byteArray) {
		final StringBuffer hexStringBuffer = new StringBuffer();
		for (int i = 0; i < byteArray.length; i++) {
			hexStringBuffer.append(byteToHex(byteArray[i]));
		}
		return hexStringBuffer.toString();
	}

	private static String byteToHex(final byte num) {
		final char[] hexDigits = new char[2];
		hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
		hexDigits[1] = Character.forDigit((num & 0xF), 16);
		return new String(hexDigits);
	}

}
