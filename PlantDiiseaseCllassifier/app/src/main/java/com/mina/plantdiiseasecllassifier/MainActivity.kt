package com.mina.plantdiiseasecllassifier

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var mClassifier: Classifier
    private lateinit var mBitmap: Bitmap

    private val mCameraRequestCode = 0
    private val mGalleryRequestCode = 2

    //    private val mInputSize = 224
    private val mInputSize = 299
    private val mModelPath = "plant_efficientnetbo_model.tflite"
    private val mLabelPath = "class.txt"
    private val mSamplePath = "Applehealthy.JPG"

    private val CAMERA_PERMISSION_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        mClassifier = Classifier(assets, mModelPath, mLabelPath, mInputSize)

        resources.assets.open(mSamplePath).use {
            mBitmap = BitmapFactory.decodeStream(it)
            mBitmap = Bitmap.createScaledBitmap(mBitmap, mInputSize, mInputSize, true)
            mPhotoImageView.setImageBitmap(mBitmap)
        }

        mCameraButton.setOnClickListener {
            checkPermission(
                Manifest.permission.CAMERA,
                CAMERA_PERMISSION_CODE
            )
//            openCam()

        }

        mGalleryButton.setOnClickListener {
            checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                STORAGE_PERMISSION_CODE
            )
//            openGallery()

        }
        mDetectButton.setOnClickListener {
            val results = mClassifier.recognizeImage(mBitmap).firstOrNull()
            mResultTextView.text = results?.title + "\n Confidence:" + results?.confidence

        }
        mCall.setOnClickListener {
            openDial()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == mCameraRequestCode) {
            //Considérons le cas de la caméra annulée
            if (resultCode == Activity.RESULT_OK && data != null) {
                mBitmap = data.extras!!.get("data") as Bitmap
                mBitmap = scaleImage(mBitmap)
                val toast = Toast.makeText(
                    this,
                    ("Image crop to: w= ${mBitmap.width} h= ${mBitmap.height}"),
                    Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.BOTTOM, 0, 20)
                toast.show()
                mPhotoImageView.setImageBitmap(mBitmap)
                mResultTextView.text = "Your photo image set now."
            } else {
                Toast.makeText(this, "Camera cancel..", Toast.LENGTH_LONG).show()
            }
        } else if (requestCode == mGalleryRequestCode) {
            if (data != null) {
                val uri = data.data

                try {
                    mBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                println("Success!!!")
                mBitmap = scaleImage(mBitmap)
                mPhotoImageView.setImageBitmap(mBitmap)

            }
        } else {
            Toast.makeText(this, "Unrecognized request code", Toast.LENGTH_LONG).show()

        }
    }

    //  resize image to git same size that model working on
    fun scaleImage(bitmap: Bitmap?): Bitmap {
        val orignalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / orignalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, orignalWidth, originalHeight, matrix, true)
    }
// Permission to access mobile hardware
    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode)
        } else {
            if (requestCode == 100)
                openCam()
            else if (requestCode == 101)
                openGallery()
//            Toast.makeText(this@MainActivity, "Permission already granted", Toast.LENGTH_SHORT)
//                .show()

        }
    }

// This function is called when the user accepts or decline the permission.
// Request Code is used to check which permission called this function.
// This request code is provided when the user is prompt for permission.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCam()
                Toast.makeText(this@MainActivity, "Camera Permission Granted", Toast.LENGTH_SHORT)
                    .show()

            } else {
                Toast.makeText(this@MainActivity, "Camera Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery()
                Toast.makeText(this@MainActivity, "Storage Permission Granted", Toast.LENGTH_SHORT)
                    .show()

            } else {
                Toast.makeText(this@MainActivity, "Storage Permission Denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

//    private fun acceptPermission() {
//        checkPermission(
//            Manifest.permission.CAMERA,
//            CAMERA_PERMISSION_CODE
//        )
//
//        checkPermission(
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            STORAGE_PERMISSION_CODE
//        )
//
//
//    }

// open  mobile openCam
    private fun openCam() {
        val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(callCameraIntent, mCameraRequestCode)

    }
// open  mobile gallery
    private fun openGallery() {
        val callGalleryIntent = Intent(Intent.ACTION_PICK)
        callGalleryIntent.type = "image/*"
        startActivityForResult(callGalleryIntent, mGalleryRequestCode)

    }
    // open  mobile openDial
    private fun openDial() {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:<+18557880638>")
        startActivity(intent)

    }

}

