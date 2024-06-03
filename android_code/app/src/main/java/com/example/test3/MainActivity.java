package com.example.test3;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editText;
    private ImageButton imageButton8;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText = findViewById(R.id.editText);
        imageButton8 = findViewById(R.id.imageButton8);

        imageButton8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String command = editText.getText().toString();
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                intent.putExtra("command", command);
                startActivity(intent);
            }
        });
    }
}
