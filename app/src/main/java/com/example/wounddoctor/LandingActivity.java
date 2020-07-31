package com.example.wounddoctor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Objects;

public class LandingActivity extends AppCompatActivity {

    private Button getStartedButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        getStartedButton = findViewById(R.id.landingStartButton);
        getStartedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Go to login activity
                startActivity(new Intent(LandingActivity.this,
                        LoginActivity.class));
            }
        });
    }
}
