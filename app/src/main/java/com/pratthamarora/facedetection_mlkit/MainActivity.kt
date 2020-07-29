package com.pratthamarora.facedetection_mlkit

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val IMAGE_PICKER = 100
    }

    private val options by lazy {
        FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()
    }
    private lateinit var firebaseVisionImage: FirebaseVisionImage
    private lateinit var firebaseVisionFaceDetector: FirebaseVisionFaceDetector
    private lateinit var paint: Paint
    private lateinit var canvas: Canvas


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseVisionFaceDetector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options)

        selectImageBtn.setOnClickListener {
            getImageFromLocal()
        }

    }

    private fun getImageFromLocal() {
        Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(this, IMAGE_PICKER)
        }
    }

    private fun detectFace(bmp: Bitmap) {
        val scaledBitmap = Bitmap.createScaledBitmap(
                bmp,
                480,
                480,
                true
        )
        val mBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)

        canvas = Canvas(mBitmap)
        paint = Paint()
        paint.apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        firebaseVisionImage = FirebaseVisionImage.fromBitmap(mBitmap)
        firebaseVisionFaceDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { faces ->
                    for (face in faces) {
                        val bounds = face.boundingBox
                        canvas.drawRect(bounds, paint)

                        //landmark ears
                        val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                        leftEar?.let {
                            val pos = it.position
                            val rect = Rect(
                                    pos.x.toInt() - 20,
                                    pos.y.toInt() - 30,
                                    pos.x.toInt() + 5,
                                    pos.y.toInt() + 30
                            )
                            canvas.drawRect(rect, paint)
                        }
                        val rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR)
                        rightEar?.let {
                            val pos = it.position
                            val rect = Rect(
                                    pos.x.toInt() + 20,
                                    pos.y.toInt() - 30,
                                    pos.x.toInt() + 5,
                                    pos.y.toInt() + 30
                            )
                            canvas.drawRect(rect, paint)
                        }

                        //landmark mouth
                        val mouthLeft = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT)
                        val mouthRight = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT)
                        val mouthBottom = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM)
                        mouthLeft?.let {
                            val leftPos = it.position
                            mouthRight?.let { it1 ->
                                val rightPos = it1.position
                                mouthBottom?.let { it2 ->
                                    val bottomPos = it2.position
                                    val path = Path().apply {
                                        moveTo(leftPos.x - 5, rightPos.y - 5)
                                        lineTo(rightPos.x + 5, rightPos.y - 5)
                                        lineTo(bottomPos.x, bottomPos.y + 5)
                                        close()
                                    }
                                    canvas.drawPath(path, paint)
                                }
                            }
                        }

                        //landmark eye
                        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
                        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
                        leftEye?.let {
                            val pos = it.position
                            val rect = Rect(
                                    pos.x.toInt() - 30,
                                    pos.y.toInt() - 10,
                                    pos.x.toInt() + 20,
                                    pos.y.toInt() + 10
                            )
                            canvas.drawRect(rect, paint)
                        }
                        rightEye?.let {
                            val pos = it.position
                            val rect = Rect(
                                    pos.x.toInt() - 20,
                                    pos.y.toInt() - 10,
                                    pos.x.toInt() + 20,
                                    pos.y.toInt() + 10
                            )
                            canvas.drawRect(rect, paint)
                        }

                        //landmark nose
                        val noseBase = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)
                        noseBase?.let {
                            val pos = it.position
                            val rect = Rect(
                                    pos.x.toInt() - 30,
                                    pos.y.toInt() - 20,
                                    pos.x.toInt() + 30,
                                    pos.y.toInt() + 10
                            )
                            canvas.drawRect(rect, paint)
                        }
                    }
                    imageViewFace.setImageBitmap(mBitmap)
                }
                .addOnFailureListener {
                    Log.d(TAG, "detectFace: $it")
                }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.data
                val bitmap = uri?.let { getBitmap(it) }
//                imageViewFace.setImageBitmap(bitmap)
                bitmap?.let { detectFace(it) }
            }
        }
    }

    private fun getBitmap(uri: Uri): Bitmap {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }
    }
}