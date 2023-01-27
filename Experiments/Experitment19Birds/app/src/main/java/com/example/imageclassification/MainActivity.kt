package com.example.imageclassification

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.imageclassification.databinding.ActivityMainBinding
import com.example.imageclassification.ml.Birds
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.support.image.TensorImage
import java.io.File
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var imageView: ImageView
    private lateinit var button: Button
    private lateinit var tvOutput: TextView
    private val GALLERY_REQUEST_CODE = 123
    private var imsel : Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)  //setContentView(R.layout.activity_main)

        imageView = binding.imageView
        button = binding.captureImageBt
        tvOutput = binding.txtViewOutput
        val buttonLoad = binding.loadImageBt

        val bitmap: InputStream = assets.open("falcon.jpg")//val bitmap: InputStream = assets.open("owel.jpg")
        val bit = BitmapFactory.decodeStream(bitmap)
        imageView.setImageBitmap(bit)

        outputGenerator(bit)

        binding.processImage.setOnClickListener {

            if(imsel == true) {
                var bitmap: InputStream =
                    assets.open("owel.jpg")//val bitmap: InputStream = assets.open("owel.jpg")
                var bit = BitmapFactory.decodeStream(bitmap)
                imageView.setImageBitmap(bit)
                outputGenerator(bit)
                imsel = false
            }
            else{
                val bitmap: InputStream = assets.open("falcon.jpg")//val bitmap: InputStream = assets.open("owel.jpg")
                val bit = BitmapFactory.decodeStream(bitmap)
                imageView.setImageBitmap(bit)
                outputGenerator(bit)
                imsel = true
            }
        }

        binding.TEST.setOnClickListener {

            launch{tests()}

        }

    }

    // request camera permission
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        granted->if(granted){
            takePicturePreview.launch(null)
        }
        else{
            Toast.makeText(this, "Permission Denied! Please try again", Toast.LENGTH_SHORT).show()
        }
    }

    // launch camera & take picture
    private val takePicturePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){
        bitmap-> if(bitmap != null){
            imageView.setImageBitmap(bitmap)
            outputGenerator(bitmap)
        }
    }
    //get image from gallery
    private val onresult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result->
        Log.i("TAG","This is the result: ${result.data} ${result.resultCode}")
        onResultReceived(GALLERY_REQUEST_CODE,result)
    }

    private fun onResultReceived(requestCode: Int, result: ActivityResult?) {
        when(requestCode){
            GALLERY_REQUEST_CODE ->{
                if(result?.resultCode == Activity.RESULT_OK){
                    val uri = result.data?.data
                    if(uri != null){
                        Log.i("TAG", "OnResultReceived: $uri")
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        imageView.setImageBitmap(bitmap)
                        outputGenerator(bitmap)
                    }


                    /*result.data?.data?.let{uri ->
                        Log.i("TAG", "OnResultReceived: $uri")
                        val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(uri))
                        imageView.setImageBitmap(bitmap)
                        outputGenerator(bitmap)
                    }*/
                }
                else {
                    Log.e("TAG", "onActivityResult: error in selecting image")
                }
            }
        }
    }

    suspend fun tests(){
        for(i in 1..20){
            var bitmap: InputStream =
                assets.open("bird${i}.jpg")//val bitmap: InputStream = assets.open("owel.jpg")
            var bit = BitmapFactory.decodeStream(bitmap)
            imageView.setImageBitmap(bit)

            TEST(bit)
            delay(500)
        }

        for(i in 1..20){
            var bitmap: InputStream =
                assets.open("bird${i}.jpg")//val bitmap: InputStream = assets.open("owel.jpg")
            var bit = BitmapFactory.decodeStream(bitmap)
            imageView.setImageBitmap(bit)

            TEST2(bit)
            delay(500)
        }
    }

    // with currentTimeMillis
    private fun TEST2 (bitmap: Bitmap){
        //declaring tensorflow lite model variable

        val model = Birds.newInstance(this)

        // convert bitmap into tensorflow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val TFimage = TensorImage.fromBitmap(newBitmap)

        //val outputs = foodD.process(bitmap, 0)
        // process image and sort in descending order

        var outputs = model.process(TFimage).probabilityAsCategoryList
        //get current time
        var timebefore: Long = System.currentTimeMillis()
        var timeNow: Long = 0
        //val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
        // Runs model inference and gets result.
        outputs = model.process(TFimage).probabilityAsCategoryList

        timeNow = System.currentTimeMillis()
        Log.d("Diffs", "${timeNow - timebefore}");
        timebefore = timeNow

        var max = outputs.get(0);

        for (i in outputs){
            if(i.score > max.score){
                max = i
            }
        }

        // getting result having high probability
        val highProbabilityOut = max//outputs[0]


        //val result = outputs.result.get(0).labels.get(0).toString()
        // setting output text
        tvOutput.text = highProbabilityOut.label // result
        //Log.i("TAG", "outputGenerator: $highProbabilityOut")

    }

    //with elapsedRealtimeNanos
    private suspend fun TEST(bitmap: Bitmap){
        //declaring tensorflow lite model variable

        val model = Birds.newInstance(this)

        // convert bitmap into tensorflow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val TFimage = TensorImage.fromBitmap(newBitmap)

        //val outputs = foodD.process(bitmap, 0)
        // process image and sort in descending order

        var outputs = model.process(TFimage).probabilityAsCategoryList
        //get current time
        //var timebefore: Long = System.currentTimeMillis()
        //var timeNow: Long = 0
        val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
            // Runs model inference and gets result.
        outputs = model.process(TFimage).probabilityAsCategoryList

        val lastInferenceTimeNanos =
            SystemClock.elapsedRealtimeNanos() - inferenceStartTimeNanos
        Log.d("TIME", "${lastInferenceTimeNanos}")
        var max = outputs.get(0);

        for (i in outputs){
            if(i.score > max.score){
                max = i
            }
        }

        // getting result having high probability
        val highProbabilityOut = max//outputs[0]


        //val result = outputs.result.get(0).labels.get(0).toString()
        // setting output text
        tvOutput.text = highProbabilityOut.label // result
        //Log.i("TAG", "outputGenerator: $highProbabilityOut")
    }

    private fun outputGenerator(bitmap: Bitmap){
        //declaring tensorflow lite model variable
/*
        val localModel = LocalModel.Builder()
            .setAbsoluteFilePath("food.tflite")
            .build()
        // Multiple object detection in static images
        val customObjectDetectorOptions =
            CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build()

        val foodD =
            ObjectDetection.getClient(customObjectDetectorOptions)*/
        val model = Birds.newInstance(this)

        // convert bitmap into tensorflow image
        val newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val TFimage = TensorImage.fromBitmap(newBitmap)

        //val outputs = foodD.process(bitmap, 0)
        // process image and sort in descending order

        var outputs = model.process(TFimage).probabilityAsCategoryList
        //get current time
        var timebefore: Long = System.currentTimeMillis()
        var timeNow: Long = 0
        for (j in 1..100){
        // Runs model inference and gets result.
            outputs = model.process(TFimage)
            .probabilityAsCategoryList
            /*.apply {
                sortByDescending { it.score }
            }*/
            //get current time

            timeNow = System.currentTimeMillis()
            Log.d("Diffs", "${timeNow - timebefore}");
            timebefore = timeNow
        }
        var max = outputs.get(0);
        /*for (i in 1..outputs.size){
            if(outputs.get(i).score > max.score){
                max = outputs.get(i)
            }
        }*/
        for (i in outputs){
            if(i.score > max.score){
                max = i
            }
        }

        // getting result having high probability
        val highProbabilityOut = max//outputs[0]


        //val result = outputs.result.get(0).labels.get(0).toString()
        // setting output text
        tvOutput.text = highProbabilityOut.label // result
        //Log.i("TAG", "outputGenerator: $highProbabilityOut")

    }

    //to download image to device
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
        isGranted: Boolean ->
        if(isGranted){
            AlertDialog.Builder(this).setTitle("Download Image?")
                .setMessage("Do you want to download this image to your device?")
                .setPositiveButton("Yes"){_,_ ->
                    val drawable:BitmapDrawable = imageView.drawable as BitmapDrawable
                    val bitmap = drawable.bitmap
                    downloadImage(bitmap)
                }
                .setNegativeButton("No") {dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
        else {
            Toast.makeText(this, "Please allow permission to download image", Toast.LENGTH_LONG).show()
        }
    }
    // takes bitmap and store to user's device
    private fun downloadImage(mBitmap: Bitmap):Uri? {
        val contentValues = ContentValues().apply{
            put(MediaStore.Images.Media.DISPLAY_NAME, "Birds_Images"+ System.currentTimeMillis()/1000)
            put(MediaStore.Images.Media.MIME_TYPE,"image/png")
        }
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        if(uri!= null) {
            contentResolver.insert(uri, contentValues)?.also {
                contentResolver.openOutputStream(it).use{outputStream ->
                    if(!mBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)){
                        throw IOException("Couldn't save the bitmap")
                    }
                    else{
                        Toast.makeText(applicationContext, "Image Saved", Toast.LENGTH_LONG).show()
                    }
                }
                return it
            }
        }
        return null
    }
}