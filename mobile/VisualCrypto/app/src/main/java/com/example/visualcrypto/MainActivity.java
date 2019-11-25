package com.example.visualcrypto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void getImageFromFile(View view){
        //ContextCompat.checkSelfPermission(MainActivity.Manifest.)
        File imgFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "qrcode.png");
        if (imgFile.exists()){
            Bitmap bMap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

            ImageView iView = (ImageView) findViewById(R.id.iView);
            iView.setImageBitmap(bMap);
        }
    }
}
