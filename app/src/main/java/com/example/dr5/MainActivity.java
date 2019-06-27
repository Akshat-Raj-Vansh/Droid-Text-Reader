package com.example.dr5;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MainActivity extends AppCompatActivity {

    TextView textTargetUri;
    ImageView targetImage;
    Button btnProcess;
    Button buttonLoadImage;
    Button camera;
    FloatingActionButton allProcesses;
    FloatingActionButton openCameraX;
    FloatingActionButton btnProcessX;
    FloatingActionButton loadImageX;
    FloatingActionButton updateToCloud;
    TextView txtView;
    Bitmap bitmap;
    final int CAMERA_REQUEST = 9999;
    final int LOAD_REQUEST = 8888;
    String mCurrentPhotoPath;
    String mCurrentPath;
    boolean isFabOpen = false;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;
    String text = "";
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference ThermomterReading = database.getReference("Thermometer");

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_action_bar_layout);

        buttonLoadImage = findViewById(R.id.loadimage);
        btnProcess = findViewById(R.id.btnProcess);

        allProcesses = findViewById(R.id.allProcesses);
        openCameraX = findViewById(R.id.openCameraX);
        btnProcessX = findViewById(R.id.btnProcessX);
        loadImageX = findViewById(R.id.loadImageX);
        updateToCloud = findViewById(R.id.updateToCloud);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backward);

        camera = findViewById(R.id.camera);
        txtView = findViewById(R.id.txtView);
        textTargetUri = findViewById(R.id.targeturi);
        targetImage = findViewById(R.id.image_view);

        btnProcessX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               processImage();
            }
        });

        loadImageX.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadImage();
            }
        });

        allProcesses.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFAB();
            }
        });

        openCameraX.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    dispatchTakePictureIntent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        updateToCloud.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                postProcessing(text);
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
                mCurrentPath = targetUri.getEncodedPath();
                //bitmap = rotateImage(bitmap);
                targetImage.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            // Show the thumbnail on ImageView
            Uri imageUri = Uri.parse(mCurrentPhotoPath);
            File file = new File(imageUri.getPath());
            try {
                InputStream ims = new FileInputStream(file);
                targetImage.setImageBitmap(BitmapFactory.decodeStream(ims));
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
            } catch (FileNotFoundException e) {
                return;
            }

            // ScanFile so it will be appeared on Gallery
            MediaScannerConnection.scanFile(MainActivity.this,
                    new String[]{imageUri.getPath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });
        }
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
            if (strBuilder.toString().length() != 0) {
                text = (strBuilder.toString().substring(0, strBuilder.toString().length() - 1));
                txtView.setText(strBuilder.toString().substring(0, strBuilder.toString().length() - 1));
            }
            else
                txtView.setText("Found nothing!");
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "Camera");
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() throws IOException {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                 return;
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(MainActivity.this, "com.example.android.fileprovider",createImageFile());
                bitmap = BitmapFactory.decodeFile(photoFile.getPath());
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    public void animateFAB() {

        if (isFabOpen) {
            allProcesses.startAnimation(rotate_backward);

            loadImageX.startAnimation(fab_close);
            openCameraX.startAnimation(fab_close);
            updateToCloud.startAnimation(fab_close);
            btnProcessX.startAnimation(fab_close);

            loadImageX.setClickable(false);
            openCameraX.setClickable(false);
            updateToCloud.setClickable(false);
            btnProcessX.setClickable(false);

            isFabOpen = false;
            Log.d("FAB", "close");

        } else {

            allProcesses.startAnimation(rotate_forward);

            loadImageX.startAnimation(fab_open);
            openCameraX.startAnimation(fab_open);
            updateToCloud.startAnimation(fab_open);
            btnProcessX.startAnimation(fab_open);

            loadImageX.setClickable(true);
            openCameraX.setClickable(true);
            updateToCloud.setClickable(true);
            btnProcessX.setClickable(true);

            isFabOpen = true;
            Log.d("FAB", "open");

        }
    }

    public void postProcessing(String text) {
        StringTokenizer string = new StringTokenizer(text);
        String value = "";
        String word = "";
        while (string.hasMoreTokens()) {
            word = string.nextToken();
            if (IsNumber(word)) {
                value = value + " " + word;
            }
        }

    }

    private boolean IsNumber(String word) {
        int length = word.length();
        boolean flag = true;
        for (int i = 0; i <= length; i++) {
            if ("1234567890".indexOf(word.charAt(i)) == -1) {
                flag = false;
                break;
            }
        }
        return flag;
    }
}
