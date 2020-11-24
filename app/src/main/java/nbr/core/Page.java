package nbr.core;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;

import java.io.IOException;

class Page extends LinearLayout {
    MainActivity activity;

    public Page(Context context) {
        super(context);
        this.activity = (MainActivity)context;
        this.setOrientation(LinearLayout.VERTICAL);
    }

    private Button createButton(String name, int textColor, int backgroundColor) {
        return createButton(0, name, textColor, backgroundColor);
    }

    private Button createButton(int id, String name, int textColor, int backgroundColor) {
        Button button = new Button(this.getContext());
        if (id != 0) button.setId(id);
        button.setText(name);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Command.onClickButton(activity, v);
                } catch (JSONException | IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        button.setTextColor(textColor);
        button.setBackgroundColor(backgroundColor);
        return button;
    }

    public void addButton(String name) {
        addView(createButton(name, Color.WHITE, Color.BLUE));
    }

    public void addButton(int id, String name, int textColor, int backgroundColor) {
        addView(createButton(id, name, textColor, backgroundColor));
    }

    public void addButton(String name, int textColor, int backgroundColor) {
        addView(createButton(name, textColor, backgroundColor));
    }

    public void show() {
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(this);
        activity.setContentView(scrollView);
    }

    public void addEditText(int id) {
        addEditText(id, false);
    }

    public void addEditText(int id, boolean noKeyboard) {
        final EditText editText = new EditText(this.getContext());
        editText.setId(id);
        if (noKeyboard) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // API 21
                editText.setShowSoftInputOnFocus(false);
            } else { // API 11-20
                editText.setTextIsSelectable(true);
            }
        }
        addView(editText);
    }

    public void addText(String text, int textColor, int backgroudColor, int typeface) {
        addText(0, text, textColor, backgroudColor, typeface);
    }

    public void addText(int id, String text, int textColor, int backgroudColor, int typeface) {
        TextView textView = new TextView(this.getContext());
        if (id != 0) textView.setId(id);
        textView.setText(text);
        textView.setTextSize(22);
        textView.setTypeface(textView.getTypeface(), typeface);
        textView.setTextColor(textColor);
        textView.setBackgroundColor(backgroudColor);
        addView(textView);
    }

    public void addButtons(String[] strings) {
        addButtons(strings, Color.WHITE, Color.BLUE);
    }

    public void addButtons(String[] strings, int textColor, int backgroudColor) {
        LinearLayout linearLayout = new LinearLayout(this.getContext());
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        linearLayout.setBackgroundColor(MainActivity.WHITE);

        for(String s: strings) {
            Button b = createButton(s, textColor, backgroudColor);
            linearLayout.addView(b);
        }

        linearLayout.setPadding(10, 10, 10, 10);

        addView(linearLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }
}