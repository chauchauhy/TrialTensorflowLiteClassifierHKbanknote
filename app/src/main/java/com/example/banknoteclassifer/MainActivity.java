package com.example.banknoteclassifer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;
    Button selecter;
    Button label;
    Context context;
    Bitmap bitmap;
    Uri url;
    private int pickCode = 1;
    private String labelPath = "labels.txt";
    private String modelPath = "model_unquant.tflite";
    private String modelPath1 = "model.tflite";
    private int inputSize= 224;
    private Classifier classifier;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            initialUIView();

        }catch (Exception e){

        }



    }





    public void initialUIView(){
        imageView = findViewById(R.id.imageView);
        selecter = findViewById(R.id.imageSelecter);
        label = findViewById(R.id.imageLabel);
        label.setClickable(true);
        context = this;

        selecter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent intent = new Intent();
               intent.setType("image/*");
               intent.setAction(Intent.ACTION_GET_CONTENT);
               startActivityForResult(Intent.createChooser(intent, "please select"), pickCode);
            }
        });
        label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Clicked", Toast.LENGTH_LONG).show();
                try {
                    classifier = new Classifier(getAssets(), modelPath, labelPath, inputSize);
                    List<Classifier.Recognition> result = classifier.recognizeImage(bitmap);
                    for (Classifier.Recognition recognition : result){
                        Log.i("Clicked", recognition.title + recognition.confidence );
                    }

                    Log.i("Clicked", "clicked leng: " + result.size());

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("Clicked", "exed");

                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == pickCode && resultCode == RESULT_OK && data != null && data.getData() != null){
            url = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), url);
                imageView.setImageBitmap(bitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}