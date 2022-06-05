package com.kisera.drawingappkotlin

import android.Manifest.*
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Gallery
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_layout_brushsize.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.jar.Manifest


class MainActivity : AppCompatActivity() {

    private var mImageCurrentPaint : ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        drawing_view.setSizeForBrush(20.toFloat())

        mImageCurrentPaint = ll_paint_colours[1] as ImageButton
        mImageCurrentPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
        )

        ib_brush.setOnClickListener {
            showBrushChooserDialog()
        }

        ib_gallery.setOnClickListener {
            if (isReadStorageAllowed()){

                val pickPhotoPicker = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)

                startActivityForResult(pickPhotoPicker, GALLERY)

            }else{
                requestStoragePermission()
            }
        }






        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }
        ib_saveImage.setOnClickListener {
            if (isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(fl_drawing_view_container)).execute()
            }else{
                requestStoragePermission()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if (requestCode== GALLERY){
                try {
                    if (data!! != null){
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }else{
                        Toast.makeText(this@MainActivity, "Error in parsing the image of the corrupted file",
                            Toast.LENGTH_SHORT).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushChooserDialog (){

        val brushDialog = Dialog(this, )
        brushDialog.setContentView(R.layout.dialog_layout_brushsize)
        brushDialog.setTitle("Brush size: ")

        val smallBtn = brushDialog.ib_small_brush
        smallBtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val mediumBtn = brushDialog.ib_medium_brush
        mediumBtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.ib_large_brush
        largeBtn.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }

    fun painClicked(view: View){
        if (view !== mImageCurrentPaint){
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this@MainActivity, R.drawable.pallet_pressed)
            )
            mImageCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this@MainActivity, R.drawable.pallet_normal)
            )
            mImageCurrentPaint =view
        }
    }

    private fun requestStoragePermission(){
       if (ActivityCompat.shouldShowRequestPermissionRationale(this,
           arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
               permission.WRITE_EXTERNAL_STORAGE).toString())){
           Toast.makeText(this, "need permission to add to background ",
               Toast.LENGTH_LONG).show()
       }
        ActivityCompat.requestPermissions(this,
            arrayOf(android.Manifest.
                permission.READ_EXTERNAL_STORAGE,
            permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode== STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[1] ==
                PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this@MainActivity,
                    "permission granted now yu may read the storage",
                    Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this@MainActivity,
                    "permission denied",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap{

        val returnedBitmap = Bitmap.createBitmap(
            view.width, view.height, Bitmap.Config.ARGB_8888)

            val canvas =Canvas(returnedBitmap)
        //saving both the image background and the image drawn as one image
            val bgDrawable = view.background
            if (bgDrawable != null){
                bgDrawable.draw(canvas)
            }else{
                canvas.drawColor(Color.WHITE)
            }

            view.draw(canvas)
            return returnedBitmap
    }


    //Running in background by creating an async task
    private inner class BitmapAsyncTask(val mBitmap: Bitmap) :
        AsyncTask<Any, Void, String>(){

        //code for initializing the progress bar
        private lateinit var mProgressDialog : Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {

           var result = ""

            if (mBitmap != null){
                try {
            /// General structure if you want to store something in your device
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90, bytes)
                    val f =File(externalCacheDir!!.absoluteFile.toString() +
                            File.separator + "KidsDrawing_" +
                            System.currentTimeMillis() /1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result= f.absolutePath

                }catch (e: Exception){
                    result =""
                    e.printStackTrace()
                }
            }

            return  result

        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressbar()
            if (!result!!.isEmpty()){
                Toast.makeText(this@MainActivity,
                    "File saved Successfully :$result",
                    Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this@MainActivity,
                    "something went wrong",
                    Toast.LENGTH_SHORT).show()
            }

            //sharing the file to other people
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type= "images/png"
                startActivity(
                    Intent.createChooser(
                        shareIntent, "share"
                    )
                )
            }

        }
        //showing the progress dialog
        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity,)
            mProgressDialog.setContentView(R.layout.dialog_cudtom_progress_bar)
            mProgressDialog.show()
        }
        //function for cancelling the progress bar
        private fun cancelProgressbar(){
            mProgressDialog.dismiss()
        }

    }

    companion object {
        private const val STORAGE_PERMISSION_CODE =1
        private const val GALLERY =2
    }

}