package nbr.core;

import java.io.*;
import java.math.*;
import java.net.*;
import java.nio.charset.*;
import java.security.*;
import java.text.*;
import java.util.*;

import org.json.*;

public class Util {

	public static boolean DEBUG = true;

	public static final SecureRandom random = new SecureRandom();
	public static SimpleDateFormat simpleDateFormat;
	public static JSONObject conf;

	static {
		simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	public static byte[] bigIntegerToBytes(final BigInteger b, final int numBytes) {
		final byte[] src = b.toByteArray();
		final byte[] dest = new byte[numBytes];
		final boolean isFirstByteOnlyForSign = src[0] == 0;
		final int length = isFirstByteOnlyForSign ? src.length - 1 : src.length;
		final int srcPos = isFirstByteOnlyForSign ? 1 : 0;
		final int destPos = numBytes - length;
		System.arraycopy(src, srcPos, dest, destPos, length);
		return dest;
	}

	public static void cleanFolder(final String dir) {
		final File index = new File(dir);
		if (!index.exists()) {
			index.mkdir();
		} else {
			final String[] entries = index.list();
			for (final String s : entries) {
				final File currentFile = new File(index.getPath(), s);
				currentFile.delete();
			}
		}
	}

//	public static byte[] decodePubKey(final String pubKeyStr) {
//		final byte[] x = Base64.getDecoder().decode(pubKeyStr);
//		final var newJSONArrayay = new byte[33];
//
//		final var startAt = newJSONArrayay.length - x.length;
//		System.arraycopy(x, 0, newJSONArrayay, startAt, x.length);
//		return newJSONArrayay;
//	}

	public static void p(final Object o) {
		System.out.println(
				simpleDateFormat.format(new Date()) + ": " + Thread.currentThread().getName() + ": " + o.toString());
	}

	public static byte[] serialize(final JSONArray array) throws IOException {
		return array.toString().getBytes(StandardCharsets.UTF_8);
	}

	public static byte[] serialize(final JSONObject obj) throws IOException {
		return obj.toString().getBytes(StandardCharsets.UTF_8);
	}

	public static String targetToString(final BigInteger target) {
		return String.format("%64s", target.toString(16)).replace(' ', '0');
	}

	public static String[] toStringArray(final JSONArray array) throws JSONException {
		if (array == null) return null;

		final String[] arr = new String[array.length()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = array.get(i).toString();
		}
		return arr;
	}

	public static void writeToFile(final String fileName, final JSONArray array) {
		writeToFile(fileName, array.toString());
	}

	public static void writeToFile(final String fileName, final JSONObject jobj) {
		writeToFile(fileName, jobj.toString());
	}

	public static void writeToFile(final String fileName, final String text) {

		if (new File(fileName).exists()) {
			Util.p("ERROR: File already exists.. not saving");
			return;
		}

		try (PrintWriter out = new PrintWriter(fileName)) {
			out.print(text);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	static JSONObject postRPC(final String _url, JSONObject json) throws JSONException {
		try {
			final URL url = new URL(_url);
			final HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "application/json; utf-8");
			con.setRequestProperty("Accept", "application/json");
			con.setConnectTimeout(2500); // set timeout to 2.5 seconds
			con.setDoOutput(true);
			try (OutputStream os = con.getOutputStream()) {
				final byte[] input = json.toString().getBytes("utf-8");
				os.write(input, 0, input.length);
				os.flush();
			}
			try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"))) {
				final StringBuilder response = new StringBuilder();
				String responseLine = null;
				while ((responseLine = br.readLine()) != null) {
					response.append(responseLine.trim());
				}
				json = new JSONObject(response.toString());
			}
		} catch (final Exception e) {
			json = new JSONObject();
			json.put("error", e.getMessage());
		}
		return json;
	}

}
