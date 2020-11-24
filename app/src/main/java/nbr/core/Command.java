package nbr.core;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

class Command {

    private static ArrayList<Toast> msjsToast = new ArrayList<Toast>();

    static void killAllToast(){
        for(Toast t:msjsToast){
            if(t!=null) {
                t.cancel();
            }
        }
        msjsToast.clear();
    }

    static final String CLIPBOARD = "Copy my addr to clipboard";
    static final String SHOW_STATUS = "Show status / Refresh balance";
    static final String ALL = "All";
    static final String CLEAN = "Clean / Reset Amount";
    static final String SEND_TX = "Send transaction";
    static final String START_MINING = "Start mining";
    static final String STOP_MINING = "Stop mining...";

    static JsonObjectRequest jsonObjectRequest = null;
    static final String url = "http://64.227.25.70:8080";
    static float total = 0;

    private static JSONObject createTx(EditText editText, JSONObject tx) throws Exception {

        long balance = tx.getLong("balance");
        tx.remove("balance");

        String toStr = editText.getText().toString();

        long value = (long) (total * 100);

        final JSONObject out = new JSONObject("{\"pubkey\":\"" + toStr + "\", \"amount\":" + value + "}");
        total = 0;

        final JSONArray outputs = new JSONArray();
        outputs.put(out);
        tx.put("outputs", outputs);

        JSONObject ret = new JSONObject();

        if (balance > total) {
            final JSONObject change = new JSONObject("{\"pubkey\":\"" + MainActivity.pubkeyStr + "\", \"amount\":" + (balance - value) + "}");
            outputs.put(change);
        } else if (balance == total) {
            // do nothing
        } else {
            ret.put("error", "amount bigger than balance");
            return ret;
        }

        tx.put("time", System.currentTimeMillis());

        final String txHash = Crypto.sha(tx);

        // signature
        tx.put("sig", Crypto.encodeHexString(Crypto.sign(MainActivity.privateKey, tx)));

        return tx;
    }

    static void onClickButton(final MainActivity activity, View v) throws JSONException, IOException, InterruptedException {
        killAllToast();
        final Button button = (Button) v;
        String command = button.getText().toString();
        final String[] toastMessage = {null};
        final EditText editText = activity.findViewById(MainActivity.idPublicKeyField);

        switch (command) {
            case SEND_TX:
                JSONObject getInputs = new JSONObject("{\"method\":\"getInputs\", \"pubkey\":\"" + MainActivity.pubkeyStr + "\"}");

                JsonObjectRequest req = new JsonObjectRequest
                        (Request.Method.POST, Command.url, getInputs, new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(final JSONObject response) {
                                Log.d("wallet", response.toString());
                                try {
                                    JSONObject toSend = createTx(editText, response);

                                JsonObjectRequest req2 = new JsonObjectRequest
                                        (Request.Method.POST, Command.url, toSend, new Response.Listener<JSONObject>() {
                                            @Override
                                            public void onResponse(final JSONObject response) {
                                                Log.d("wallet", response.toString());
                                                try {
                                                    String msg = "sorry, something went wrong";
                                                    if (response.has("status") && response.getBoolean("status")) {
                                                        msg = "transaction sent";
                                                    }
                                                    killAllToast();
                                                    Toast t = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
                                                    t.show();
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }, new Response.ErrorListener() {

                                            @Override
                                            public void onErrorResponse(VolleyError error) {
                                            }
                                        });
                                    MySingleton.getInstance(activity).addToRequestQueue(req2);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        }, new Response.ErrorListener() {

                            @Override
                            public void onErrorResponse(VolleyError error) {
                            }
                        });

                MySingleton.getInstance(activity).addToRequestQueue(req);
                //editText.setText(o.toString());

                break;

            case START_MINING:
                //final JSONObject blockTemplate = RPC.getBlockTemplate();

                Thread thread = new Thread(){
                    public void run(){
                        while(true) {
                            try {
                            Candidate.MINE = false;
                            Thread.sleep(200); // wait a little for last miner thread finish before start another

                            JSONObject blockTemplate = null;
                            try {
                                blockTemplate = new JSONObject("{\"method\":\"getBlockTemplate\"}");
                            } catch (JSONException e) {
                                throw new RuntimeException(e.getMessage());
                            }

                            jsonObjectRequest = new JsonObjectRequest
                                    (Request.Method.POST, url, blockTemplate, new Response.Listener<JSONObject>() {

                                        @Override
                                        public void onResponse(final JSONObject response) {
                                            Log.d("wallet", response.toString());
                                            Thread thread = new Thread(){
                                                public void run(){
                                                    Candidate c = null;
                                                    try {
                                                        Candidate.MINE = true;
                                                        c = new Candidate(MainActivity.pubkeyStr, response);
                                                        c.mine(activity);
                                                    } catch (IOException | JSONException | InterruptedException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            };
                                            thread.start();
                                        }
                                    }, new Response.ErrorListener() {

                                        @Override
                                        public void onErrorResponse(VolleyError error) {
                                            Candidate.MINE = false;
                                        }
                                    });

                                MySingleton.getInstance(activity).addToRequestQueue(jsonObjectRequest);
                                Thread.sleep(1000*30); // 30s mining before get another block
                                if (!Candidate.MINE) return;
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e.getMessage());
                            }
                        }
                    }
                };
                thread.start();

                button.setText(STOP_MINING);
                button.setTextColor(Color.BLACK);
                button.setBackgroundColor(Color.RED);
                // Access the RequestQueue through your singleton class.
                toastMessage[0] = "Miner started";
                break;

            case STOP_MINING:
                Candidate.MINE = false;
                Thread.sleep(350); // wait and do it again for safe stop
                Candidate.MINE = false;
                button.setText(START_MINING);
                button.setTextColor(MainActivity.WHITE);
                button.setBackgroundColor(MainActivity.ORANGE);
                toastMessage[0] = "Miner stopped";
                break;

            case CLIPBOARD:
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("pubkey", MainActivity.pubkeyStr);
                clipboard.setPrimaryClip(clip);
                toastMessage[0] = "copied";
                break;

            case SHOW_STATUS:
                Button buttonMine = activity.findViewById(MainActivity.idMineButton);
                if (!Candidate.MINE) {
                    buttonMine.setText(START_MINING);
                    buttonMine.setTextColor(MainActivity.WHITE);
                    buttonMine.setBackgroundColor(MainActivity.ORANGE);
                    toastMessage[0] = "Not Mining";
                } else {
                    buttonMine.setText(STOP_MINING);
                    buttonMine.setTextColor(Color.BLACK);
                    buttonMine.setBackgroundColor(Color.RED);
                    toastMessage[0] = "Mining..";
                }
                activity.getBalance();
                break;

            case CLEAN:
                total = 0;
                editText.setText("");
                toastMessage[0] = "Amount: " + total;
                break;

            default:
                if (command.startsWith("+")) {
                    String number = command.substring(1);
                    total += (Float.parseFloat(number) * 100.0) / 100.0;
                    total = (float) (Math.round(total * 100.0) / 100.0);
                    toastMessage[0] = "Amount: " + total;
                } else {
                    toastMessage[0] = "command not found:" + button.getText();
                }
                break;
        }

        if (toastMessage[0] != null) {
            Toast t = Toast.makeText(activity, toastMessage[0], Toast.LENGTH_LONG);
            t.show();
            msjsToast.add(t);
        }
    }
}
