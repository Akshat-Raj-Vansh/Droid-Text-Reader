package com.example.dr5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static java.lang.System.out;


public class MainActivity extends AppCompatActivity {

    TextView textTargetUri;
    ImageView targetImage;
    Button btnProcess;
    Button buttonLoadImage;
    Button camera;
    TextView txtView;
    Bitmap bitmap;
    final int CAMERA_REQUEST = 9999;
    final int LOAD_REQUEST = 8888;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        buttonLoadImage = findViewById(R.id.loadimage);
        btnProcess = findViewById(R.id.btnProcess);
        camera = findViewById(R.id.camera);
        txtView = findViewById(R.id.txtView);
        textTargetUri = findViewById(R.id.targeturi);
        targetImage = findViewById(R.id.image_view);

        btnProcess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               processImage();
            }
        });

        buttonLoadImage.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadImage();
            }
        });

        camera.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                    startCamera();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == LOAD_REQUEST) {
            Uri targetUri = data.getData();
            textTargetUri.setText(targetUri.toString());
            try {
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(targetUri));
                targetImage.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST && data != null && data.getExtras() != null)
            try {
                bitmap = (Bitmap) data.getExtras().get("data");
                targetImage.setImageBitmap(bitmap);
                textTargetUri.setText("Picture Clicked");
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public void startCamera(){
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST);
    }

    public void loadImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, LOAD_REQUEST);
    }

    public void processImage(){
        TextRecognizer txtRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        if (!txtRecognizer.isOperational()) {
            txtView.setText(R.string.error_prompt);
        } else {
            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray items = txtRecognizer.detect(frame);
            StringBuilder strBuilder = new StringBuilder();
            for (int i = 0; i < items.size(); i++) {
                TextBlock item = (TextBlock) items.valueAt(i);
                strBuilder.append(item.getValue());
                strBuilder.append("\n");
                for (Text line : item.getComponents()) {
                    Log.v("lines", line.getValue());
                    for (Text element : line.getComponents()) {
                        Log.v("element", element.getValue());

                    }
                }
            }
            if (strBuilder.toString().length() != 0)
                txtView.setText(strBuilder.toString().substring(0, strBuilder.toString().length() - 1));
            else
                txtView.setText("Found nothing!");
        }
    }
}
