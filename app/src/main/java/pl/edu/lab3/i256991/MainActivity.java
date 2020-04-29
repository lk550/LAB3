package pl.edu.lab3.i256991;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.provider.MediaStore;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int MY_PERMISSIONS_REQUEST_READ_STORAGE = 10;
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 20;
    private static int RESULT_LOAD_IMAGE = 1;

    private ImageView mImageView;
    private Uri img_uri;
    private Button mTextButton;
    private TextView mTextView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mImageView = findViewById(R.id.imgView);
        mTextButton = findViewById(R.id.textButton);
        mTextView = findViewById(R.id.textView);

        //OLD SCHOOL GET IMAGE FROM GALLERY
        Button buttonLoadImage = (Button) findViewById(R.id.uploadButton);
        buttonLoadImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    // No explanation needed; request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_STORAGE);
                } else {
                getImageFromGallery();
                }
            }
        });

        //OLD SCHOOL GET IMAGE FROM CAMERA
        Button buttonTakePicture = (Button) findViewById(R.id.cameraButton);
        buttonTakePicture.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
             //check sys OS version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    // system OS > marshmallow
                    if (checkSelfPermission(Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED){
                        //requesting permission
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        //show popup to request permissions
                        requestPermissions(permission, MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                    else {
                        //permission already granted
                        openCameraApp();
                    }
                }
                else {
                    //system OS < marshmallow
                    openCameraApp();
                }
            }
        });

        //GET TEXT
        Button buttonFindText = (Button) findViewById(R.id.textButton);
        buttonFindText.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                    runTextRecognition();
                }

        });
    }

    private void getImageFromGallery() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    private void openCameraApp() {
        ContentValues val = new ContentValues();
        val.put(MediaStore.Images.Media.TITLE, "New Picture");
        val.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");

        img_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, val);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, img_uri);
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_STORAGE: {
                // READ GALLERY
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImageFromGallery();
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // CAMERA
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                   openCameraApp();
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void runTextRecognition() {
        FirebaseVisionImage image =
                FirebaseVisionImage.fromBitmap(((BitmapDrawable) mImageView.getDrawable()).getBitmap() );
        FirebaseVisionTextRecognizer recognizer = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        recognizer.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            public void onSuccess(FirebaseVisionText texts) {
                                processTextRecognitionResult(texts);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                e.printStackTrace();
                            }
                        });
    }

    private void processTextRecognitionResult(FirebaseVisionText texts) {
        mTextView.setText(null);
        if (texts.getTextBlocks().size() == 0) {
            mTextView.setText("");
            return;
        }
        for (FirebaseVisionText.TextBlock block : texts.getTextBlocks()
        ) {
            mTextView.append(block.getText());

        }

    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            mImageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }
        else
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && null != data) {
            mImageView.setImageURI(img_uri);


        }
    }


}
