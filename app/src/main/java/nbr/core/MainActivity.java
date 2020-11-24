package nbr.core;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.bouncycastle.util.encoders.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.PrivateKey;

public class MainActivity extends AppCompatActivity {

    static final int idPublicKeyField = 1;
    static final int idMineButton = 2;
    static final int idBalance = 3;

    static final int ORANGE = Color.rgb(255, 165, 0);
    static final int GREEN = Color.rgb(0, 100, 0);
    static final int WHITE = Color.rgb(242, 243, 244);

    static String pubkeyStr;
    static PrivateKey privateKey;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            genOrLoadKeys();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        Page page = new Page(this);

        page.addText(idBalance, "$0.00", GREEN, WHITE, Typeface.BOLD_ITALIC);
        page.addText("New Transaction", Color.DKGRAY, WHITE, Typeface.NORMAL);
        page.addButtons(new String[] {"+0.01", "+0.05", "+0.10", "+0.50"}, WHITE, Color.DKGRAY);
        page.addButtons(new String[] {"+1.00", "+5.00", "+10.00", "+50.00"}, WHITE, Color.DKGRAY); // Command.ALL
        page.addText("To: (only copy-paste)", Color.BLACK, WHITE, Typeface.ITALIC);
        page.addEditText(idPublicKeyField, true);
        page.addButton(Command.SEND_TX, WHITE, GREEN);

        page.addText("Options", Color.DKGRAY, WHITE, Typeface.NORMAL);
        page.addButton(Command.CLEAN, WHITE, GREEN);
        page.addButton(idMineButton, Command.START_MINING, WHITE, ORANGE);
        page.addButton(Command.SHOW_STATUS, WHITE, GREEN);
        page.addButton(Command.CLIPBOARD, WHITE, ORANGE);

        page.show();
        getBalance();
    }

    protected void getBalance() {
        JSONObject balance = null;
        final TextView textView = this.findViewById(MainActivity.idBalance);
        try {
            balance = new JSONObject("{\"method\":\"getBalance\", \"pubkey\":\"" + pubkeyStr + "\"}");
        } catch (JSONException e) {
            throw new RuntimeException(e.getMessage());
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, Command.url, balance, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(final JSONObject response) {
                        Log.d("wallet", response.toString());
                        try {
                            double v = response.getLong("balance") / 100.0;
                            textView.setText("$" + (double)(Math.round(v * 100.0) / 100.0));
                            if (response.has("mempool")) {
                                String msg = response.getString("mempool");
                                Command.killAllToast();
                                Toast t = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG);
                                t.show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

        MySingleton.getInstance(MainActivity.this).addToRequestQueue(jsonObjectRequest);
    }

    private void genOrLoadKeys() throws Exception {
        SharedPreferences pref = this.getSharedPreferences(
                "key", Context.MODE_PRIVATE);
        pubkeyStr = pref.getString("pubkey", "Nope");

        if (pubkeyStr.equals("Nope")) {
            ECKey keypair = null;
            String publicKeyString = null;
            do {
                keypair = new ECKey();
                publicKeyString = Base64.toBase64String(keypair.getPubKey());
            } while (publicKeyString.contains("/") || publicKeyString.contains("+"));

            String privateKeyString = Base64.toBase64String(Util.bigIntegerToBytes(keypair.getPrivKey(),32));
            SharedPreferences.Editor editor = getSharedPreferences("key", MODE_PRIVATE).edit();
            editor.putString("pubkey", publicKeyString);
            editor.putString("pivkey", privateKeyString);
            editor.apply();
        }

        String pivkeyStr = pref.getString("pivkey", "Nope");
        pubkeyStr = pref.getString("pubkey", "Nope");
        privateKey = Crypto.getPrivateKey(pivkeyStr);
    }

}
