package com.pwcat.root.tailofsuffix;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    EditText tail_et;
    Button pickPath_btn, commit_btn;
    String path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    private void init() {
        tail_et = findViewById(R.id.tail_et);
        pickPath_btn = findViewById(R.id.pickpath_btn);
        commit_btn = findViewById(R.id.commit_btn);
    }
}
