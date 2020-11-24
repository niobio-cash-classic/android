package nbr.core;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigInteger;

class Candidate {

    static boolean MINE = false;

    private final JSONObject block;

    private final BigInteger target;

    private final JSONArray txs;

    private final long height;

    private final long time;

    // transform blockTemplate into a block to mine
    Candidate(final String pubkey, final JSONObject blockTemplate) throws IOException, JSONException {
        block = blockTemplate;

        // get and remove target
        target = new BigInteger(block.getString("target"), 16);
        block.remove("target");

        height = block.getLong("height");
        block.remove("height");

        //final long shouldBeAt = Util.conf.getLong("startTime") + ((height + 1) * Util.conf.getLong("blockTime"));
        final long shouldBeAt = 1230940800000L + ((height + 1) * 600000);

        final long now = System.currentTimeMillis();

        time = now > shouldBeAt ? shouldBeAt : now;

        // get and remove reward, txs and create coinbase
        final long reward = block.getLong("reward");
        txs = block.getJSONArray("txs");
        if (reward > 0) {
            final JSONObject tx = new JSONObject();
            final JSONArray outputs = new JSONArray();
            final JSONObject out = new JSONObject("{\"pubkey\":\"" + pubkey + "\", \"amount\":" + block.getLong("reward") + "}");
            outputs.put(out);
            tx.put("outputs", outputs);
            tx.put("time", time);
            txs.put(tx);
        }
        block.remove("reward");
        block.remove("txs");

        // add txsHash
        block.put("txsHash", Crypto.sha(txs));
        block.put("time", time);
    }

    // start with a random number (nonce) and follow in sequence
    void mine(AppCompatActivity activity) throws IOException, InterruptedException, JSONException {
        long i = Util.random.nextLong();

        for (;MINE; i++) {
            block.put("nonce", i);
            final BigInteger sha = Crypto.shaMine(block);
            if (target.compareTo(sha) > 0) {
                block.put("txs", txs);
                Log.d("wallet", block.toString());
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.POST, Command.url, block, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(final JSONObject response) {
                                Log.d("wallet", response.toString());
                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {

                            }
                        });

                MySingleton.getInstance(activity).addToRequestQueue(jsonObjectRequest);
                return;
            }

            if (i % 10000 == 0) {
                Log.d("wallet","mining");
            }
        }
        Log.d("wallet","mine out");
    }
}
