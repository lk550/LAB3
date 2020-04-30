package pl.edu.lab3.i256991

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*

import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText


import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.android.synthetic.main.content_main.*

class MainActivityK : AppCompatActivity() {
    private var mImageView: ImageView? = null
    private var img_uri: Uri? = null
    private var img_bitmap: Bitmap? = null
    private lateinit var outputFileUri: Uri


    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 2
        private const val REQUEST_OBJ_FINDER = 2
        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 10
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 20
        private const val RESULT_LOAD_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        //FIND OBJ
        objectButton.setOnClickListener{
            val image = getCapturedImage()
            runObjectDetection(image)
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_LOAD_IMAGE)
    }

    private fun openCameraApp() {
        /*val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")
        img_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)*/
        //val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, img_uri)
        //startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New photo")
        outputFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE)
    }

    /**
     * MLKit Object Detection Function
     */
    private fun runObjectDetection(bitmap: Bitmap) {
        // Step 1: create MLKit's VisionImage object
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        // Step 2: acquire detector object
        val options = FirebaseVisionObjectDetectorOptions.Builder()
                .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)
        // Step 3: feed given image to detector and setup callback
        detector.processImage(image)
                .addOnSuccessListener {
                    // Task completed successfully
                    // Post-detection processing : draw result
                   // debugPrint(it)
                    val drawingView = DrawingView(applicationContext, it)
                    drawingView.draw(Canvas(bitmap))
                    runOnUiThread {
                        imgView.setImageBitmap(bitmap)
                    }
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    Toast.makeText(
                            baseContext, "Oops, something went wrong!",
                            Toast.LENGTH_SHORT
                    ).show()
                }

    }

    private fun debugPrint(visionObjects : List<FirebaseVisionObject>) {
        val LOG_MOD = "MLKit-ODT"
        for ((idx, obj) in visionObjects.withIndex()) {
            val box = obj.boundingBox

            Log.d(LOG_MOD, "Detected object: ${idx} ")
            Log.d(LOG_MOD, "  Category: ${obj.classificationCategory}")
            Log.d(LOG_MOD, "  trackingId: ${obj.trackingId}")
          //  Log.d(LOG_MOD, "  entityId: ${obj.entityid}")
            Log.d(LOG_MOD, "  boundingBox: (${box.left}, ${box.top}) - (${box.right},${box.bottom})")
            if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
                Log.d(LOG_MOD, "  Confidence: ${confidence}%")
            }
        }
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

    /**
     * getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     */
    private fun getCapturedImage(): Bitmap {

        val srcImage = FirebaseVisionImage
                .fromFilePath(baseContext, outputFileUri).bitmap

        // crop image to match imageView's aspect ratio
        val scaleFactor = Math.min(
                srcImage.width / imgView.width.toFloat(),
                srcImage.height / imgView.height.toFloat()
        )

        val deltaWidth = (srcImage.width - imgView.width * scaleFactor).toInt()
        val deltaHeight = (srcImage.height - imgView.height * scaleFactor).toInt()

        val scaledImage = Bitmap.createBitmap(
                srcImage, deltaWidth / 2, deltaHeight / 2,
                srcImage.width - deltaWidth, srcImage.height - deltaHeight
        )
        srcImage.recycle()
        return scaledImage

    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK) {
            outputFileUri= data?.data!!
            imgView.setImageURI(data?.data)

        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //imgView.setImageURI(img_uri)
           val image = getCapturedImage()
           // img_bitmap = image
            imgView.setImageBitmap(image)

        } else if (requestCode == REQUEST_OBJ_FINDER && resultCode == RESULT_OK) {
            //img_bitmap?.let { runObjectDetection(it) }
            val image = getCapturedImage()
            runObjectDetection(image)
        }
    }

}


/**
 * DrawingView class:
 *    onDraw() function implements drawing
 *     - boundingBox
 *     - Category
 *     - Confidence ( if Category is not CATEGORY_UNKNOWN )
 */
class DrawingView(context: Context, var visionObjects: List<FirebaseVisionObject>) : View(context) {

    companion object {
        // mapping table for category to strings: drawing strings
        val categoryNames: Map<Int, String> = mapOf(
                FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
                FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
                FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
                FirebaseVisionObject.CATEGORY_FOOD to "Food",
                FirebaseVisionObject.CATEGORY_PLACE to "Place",
                FirebaseVisionObject.CATEGORY_PLANT to "Plant"
        )
    }

    val MAX_FONT_SIZE = 96F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        for (item in visionObjects) {
            // draw bounding box
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = item.getBoundingBox()
            canvas.drawRect(box, pen)

            // Draw result category, and confidence
            val tags: MutableList<String> = mutableListOf()
            tags.add("Category: ${categoryNames[item.classificationCategory]}")
            if (item.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
                tags.add("Confidence: ${item.classificationConfidence!!.times(100).toInt()}%")
            }

            var tagSize = Rect(0, 0, 0, 0)
            var maxLen = 0
            var index: Int = -1

            for ((idx, tag) in tags.withIndex()) {
                if (maxLen < tag.length) {
                    maxLen = tag.length
                    index = idx
                }
            }

            // calculate the right font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(tags[index], 0, tags[index].length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size so texts are inside the bounding box
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F

            // draw tags onto bitmap (bmp is in upside down format)
            for ((idx, txt) in tags.withIndex()) {
                canvas.drawText(
                        txt, box.left + margin,
                        box.top + tagSize.height().times(idx + 1.0F), pen
                )
            }
        }
    }
}
