package com.example.intra;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatTextView;

public class TypeWriter extends AppCompatTextView {

    private CharSequence text;
    private int index;
    private long delay = 100; // in ms

    private final Handler handler = new Handler();

    public TypeWriter(Context context) {
        super(context);
    }

    public TypeWriter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private final Runnable characterAdder = new Runnable() {
        @Override
        public void run() {
            setText(text.subSequence(0, index++));
            if (index <= text.length()) {
                handler.postDelayed(characterAdder, delay);
            }
        }
    };

    public void animateText(CharSequence txt) {
        text = txt;
        index = 0;
        setText("");
        handler.removeCallbacks(characterAdder);
        handler.postDelayed(characterAdder, delay);
    }

    public void setCharacterDelay(long m) {
        delay = m;
    }
}
