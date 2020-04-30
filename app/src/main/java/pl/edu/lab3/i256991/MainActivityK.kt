package pl.edu.lab3.i256991

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

import android.view.View
import android.widget.*

import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText


import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import kotlinx.android.synthetic.main.content_main.*

class MainActivityK : AppCompatActivity() {
    private lateinit var picture_uri: Uri


    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 2
        private const val MY_PERMISSIONS_REQUEST_READ_STORAGE = 10
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 20
        private const val RESULT_LOAD_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //GET IMAGE FROM GALLERY
        uploadButton.setOnClickListener {
            //check sys OS version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // system OS > marshmallow
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                    //Denied, needs to request for permissions
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                    requestPermissions(permissions, MY_PERMISSIONS_REQUEST_READ_STORAGE);
                } else {
                    //Granted
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
                    //Denied, needs to request for permissions
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, MY_PERMISSIONS_REQUEST_CAMERA)
                } else {
                    //Granted
                    getImageFromCamera()
                }
            } else {
                //system OS < marshmallow
                getImageFromCamera()
            }
        }

        //GET TEXT
        textButton.setOnClickListener { extractText() }

        //FIND OBJ
        objectButton.setOnClickListener{
            if(!this::picture_uri.isInitialized){
                Toast.makeText(
                        baseContext, "Please upload a picture first.",
                        Toast.LENGTH_SHORT
                ).show()
            }
            else {
                val image = adjustImage()
                objectDetection(image)
            }
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, RESULT_LOAD_IMAGE)
    }

    private fun getImageFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New photo")
        picture_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, picture_uri)
        startActivityForResult(takePhotoIntent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_STORAGE -> {
                // READ GALLERY
                if (grantResults.isNotEmpty() && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT).show()
                }
                return
            }
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                // CAMERA
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission from popup granted
                    getImageFromCamera()
                } else {
                    Toast.makeText(this, "Please allow the permission...", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    private fun extractText() {
        if (imgView.drawable != null && (this::picture_uri.isInitialized)) {
            textView.setText("")
            val bitmap = (imgView.drawable as BitmapDrawable).bitmap
            val image = FirebaseVisionImage.fromBitmap(bitmap)
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer

            detector.processImage(image)
                    .addOnSuccessListener { firebaseVisionText ->
                        extractTextResult(firebaseVisionText)
                    }
                    .addOnFailureListener {
                        textView.setText("Process failed")
                    }
        } else {
            Toast.makeText(this, "Select an Image First", Toast.LENGTH_LONG).show()
        }
    }


    private fun extractTextResult(texts: FirebaseVisionText) {
        if (texts.textBlocks.size == 0) {
            textView.text = "No Text detected"
            return
        }
        for (block in texts.textBlocks) {
            val blockText = block.text
            textView.append(blockText + "\n")
        }
    }


    private fun objectDetection(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val options = FirebaseVisionObjectDetectorOptions.Builder()
                .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        val detector = FirebaseVision.getInstance().getOnDeviceObjectDetector(options)

        detector.processImage(image)
                .addOnSuccessListener {
                    // Task was successful, drawing result
                    val drawingView = DrawingView(applicationContext, it)
                    drawingView.draw(Canvas(bitmap))
                    runOnUiThread {
                        imgView.setImageBitmap(bitmap)
                    }
                }
                .addOnFailureListener {
                    // Task failed
                    Toast.makeText(
                            baseContext, "Oops, something went wrong!",
                            Toast.LENGTH_SHORT
                    ).show()
                }

    }


    /**
     * getCapturedImage():
     *     Decodes and center crops the captured image from camera.
     */
    // Decoding and centering theselected picture is required
    // in order to make the ML kit work smoothly.
    private fun adjustImage(): Bitmap {

        val srcImage = FirebaseVisionImage
                .fromFilePath(baseContext, picture_uri).bitmap

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
            picture_uri= data?.data!!
            imgView.setImageURI(data?.data)
            //clear the textView in case Extract Text was previously used
            textView.text = ""

        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
           val image = adjustImage()
            imgView.setImageBitmap(image)
            //clear the textView in case Extract Text was previously used
            textView.text = ""

        }
    }

}

//required for the object detector's "draw on image" requirement
@SuppressLint("ViewConstructor")
class DrawingView(context: Context, var visionObjects: List<FirebaseVisionObject>) : View(context) {

    companion object {

        val categoryNames: Map<Int, String> = mapOf(
                FirebaseVisionObject.CATEGORY_UNKNOWN to "Unknown",
                FirebaseVisionObject.CATEGORY_HOME_GOOD to "Home Goods",
                FirebaseVisionObject.CATEGORY_FASHION_GOOD to "Fashion Goods",
                FirebaseVisionObject.CATEGORY_FOOD to "Food",
                FirebaseVisionObject.CATEGORY_PLACE to "Place",
                FirebaseVisionObject.CATEGORY_PLANT to "Plant"
        )
    }

    private val MAX_FONT_SIZE = 96F

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pen = Paint()
        pen.textAlign = Paint.Align.LEFT

        for (item in visionObjects) {
            // draw rectangle
            pen.color = Color.RED
            pen.strokeWidth = 8F
            pen.style = Paint.Style.STROKE
            val box = item.getBoundingBox()
            canvas.drawRect(box, pen)

            // Draws the result category and confidence
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

            // calculates the appropriate font size
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.color = Color.YELLOW
            pen.strokeWidth = 2F

            pen.textSize = MAX_FONT_SIZE
            pen.getTextBounds(tags[index], 0, tags[index].length, tagSize)
            val fontSize: Float = pen.textSize * box.width() / tagSize.width()

            // adjust the font size to fit the texts in the rectangle
            if (fontSize < pen.textSize) pen.textSize = fontSize

            var margin = (box.width() - tagSize.width()) / 2.0F
            if (margin < 0F) margin = 0F

            // draw tags onto bitmap
            for ((idx, txt) in tags.withIndex()) {
                canvas.drawText(
                        txt, box.left + margin,
                        box.top + tagSize.height().times(idx + 1.0F), pen
                )
            }
        }
    }
}
