package se.ltu.emapal.androidcompute;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

@SuppressWarnings("JniMissingFunction")
public class ActivityMain extends AppCompatActivity {
    static {
        System.loadLibrary("jni_compute");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textHello = (TextView)findViewById(R.id.text_hello);
        if (textHello != null) {
            textHello.setText(stringFromJNI());
        }
    }

    private native String stringFromJNI();
}
