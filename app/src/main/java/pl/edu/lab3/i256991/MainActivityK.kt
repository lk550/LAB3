package pl.edu.lab3.i256991

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

import android.view.Menu
import android.view.MenuItem
import android.widget.*

import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText


import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.content_main.*

class MainActivityK : AppCompatActivity() {
    private var mImageView: ImageView? = null
    private var img_uri: Uri? = null
    private var mUploadButton: Button? = null
    private var mTextButton: Button? = null
    private var mTextView: TextView? = null

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;

        //Permission code
        private val PERMISSION_CODE = 1001;
        const val REQUEST_IMAGE_CAPTURE = 2
        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 10
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 20
        private const val RESULT_LOAD_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageView = findViewById(R.id.imgView)
        mTextButton = findViewById(R.id.textButton)
        mUploadButton = findViewById(R.id.uploadButton)
        mTextView = findViewById(R.id.textView)

        //GET IMAGE FROM GALLERY
        // val buttonLoadImage = findViewById(R.id.uploadButton) as Button
        uploadButton.setOnClickListener {
            //check runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_DENIED) {
                    //permission denied
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    //show popup to request runtime permission
                    requestPermissions(permissions, MY_PERMISSIONS_REQUEST_READ_STORAGE);
                } else {
                    //permission already granted
                    pickImageFromGallery();
                }
            } else {
                //system OS is < Marshmallow
                pickImageFromGallery();
            }

        }

        //GET IMAGE FROM CAMERA
        cameraButton.setOnClickListener {
            //check sys OS version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // system OS > marshmallow
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    //requesting permission
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    //show popup to request permissions
                    requestPermissions(permission, MY_PERMISSIONS_REQUEST_CAMERA)
                } else {
                    //permission already granted
                    openCameraApp()
                }
            } else {
                //system OS < marshmallow
                openCameraApp()
            }
        }

        //GET TEXT
        textButton.setOnClickListener { runTextRecognition() }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_LOAD_IMAGE)
    }

    private fun openCameraApp() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        img_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, img_uri)
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_STORAGE -> {
                // READ GALLERY
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT)
                            .show()
                }
                return
            }
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // CAMERA
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCameraApp()
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    private fun runTextRecognition() {
        /*
        val image =
            FirebaseVisionImage.fromBitmap((mImageView!!.drawable as BitmapDrawable).bitmap)
        val recognizer = FirebaseVision.getInstance()
            .onDeviceTextRecognizer
        recognizer.processImage(image)
            .addOnSuccessListener { texts -> processTextRecognitionResult(texts) }
            .addOnFailureListener { e -> e.printStackTrace() }*/
        if (imgView.drawable != null) {
            textView.setText("")
            val bitmap = (imgView.drawable as BitmapDrawable).bitmap
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

            detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        processTextRecognitionResult(firebaseVisionText)
                    }
                    .addOnFailureListener {
                        textView.setText("Process failed")
                    }
        } else {
            Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
        }
    }


    private fun processTextRecognitionResult(texts: FirebaseVisionText) {
        if (texts.textBlocks.size == 0) {
            textView.text = "No Text detected"
            return
        }
        for (block in texts.textBlocks) {
            val blockText = block.text
            textView.append(blockText + "\n")
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK) {
            mImageView!!.setImageURI(data?.data)
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            mImageView!!.setImageURI(img_uri)
        }
    }


}