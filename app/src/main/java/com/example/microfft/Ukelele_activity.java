package com.example.microfft;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class Ukelele_activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ukelele);
    }
    //metodo para volver a la guitarra
    public void guitarra(View view){
        Intent guitarra = new Intent(this,MainActivity.class);
        startActivity(guitarra);
    }
}