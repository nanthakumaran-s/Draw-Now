package com.nanthu.drawnow

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.azeesoft.lib.colorpicker.ColorPickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var chosenColor = "#ff000000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView.setSizeForBrush(10f)

        ib_brush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        colorPicker.setOnClickListener {
            colorPickingProcess()
        }

        ib_gallery.setOnClickListener {
            if(isReadStorageAllowed()){
                val pickPhotoIntent = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                startActivityForResult(pickPhotoIntent, GALLERY)
            }else{
                requestStoragePermission()
            }
        }

        colorIndicator.setOnClickListener {
            colorIndicatorDialog()
        }

        ib_undo.setOnClickListener {
            drawingView.onClickUndo()
        }

        ib_redo.setOnClickListener {
            drawingView.onClickRedo()
        }

        ib_save.setOnClickListener {
            if (isReadStorageAllowed()){
                BitmapAsyncTask(getBitMapFromView(fl_drawing_view_layout)).execute()
            }else{
                requestStoragePermission()
            }
        }

        ib_info.setOnClickListener {
            infoClickIndicatorDialog()
        }

        ib_restore.setOnClickListener {
            restore()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY){
                try {
                    if (data!!.data != null){
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    }else{
                        Toast.makeText(
                            this,
                            "Error in parsing the image.. Try Again",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_brush_size)
        dialog.setTitle("Select the Brush Size")
        dialog.ib_small_brush.setOnClickListener {
            drawingView.setSizeForBrush(5f)
            dialog.dismiss()
        }
        dialog.ib_medium_brush.setOnClickListener {
            drawingView.setSizeForBrush(10f)
            dialog.dismiss()
        }
        dialog.ib_large_brush.setOnClickListener {
            drawingView.setSizeForBrush(15f)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun colorPickingProcess(){
        val colorPickerDialog =
            ColorPickerDialog.createColorPickerDialog(this, ColorPickerDialog.DARK_THEME)
        colorPickerDialog.setOnColorPickedListener { color, hexVal ->
            drawingView.setColor(hexVal)
            colorIndicator.setColorFilter(color)
            chosenColor = hexVal
        }
        colorPickerDialog.show()
    }

    private fun colorIndicatorDialog(){
        MaterialAlertDialogBuilder(this)
            .setTitle("The Chosen Color is : ")
            .setMessage(chosenColor)
            .setPositiveButton("Ok"){dialog, which -> }
            .show()
    }

    private fun infoClickIndicatorDialog(){
        MaterialAlertDialogBuilder(this)
            .setTitle("File Storage Information")
            .setMessage("The file will be stored at \"Internal Storage -> Android -> data -> com.nanthu.drawnow -> cache\"")
            .setPositiveButton("Ok"){dialog, which -> }
            .show()
    }

    private fun restore(){
        drawingView.restore()
        drawingView.setSizeForBrush(10f)
        drawingView.setColor("#000000")
        colorIndicator.setColorFilter(Color.BLACK)
    }

    private fun requestStoragePermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "Need Permission to add Gallery Image", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this,
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE),
        STORAGE_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(
                    applicationContext,
                    "Permission Granted",
                    Toast.LENGTH_SHORT
                ).show()
            }else{
                Toast.makeText(
                    applicationContext,
                    "Permission Denied. You can change in settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitMapFromView(view: View): Bitmap{
        val returnBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap): AsyncTask<Any, Void, String>(){

        private lateinit var mProgressDialog: Dialog

        override fun doInBackground(vararg params: Any?): String {

            var result = ""

            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
                    val f = File(externalCacheDir!!.absoluteFile.toString() +
                                File.separator + "DrawNow_" +
                                System.currentTimeMillis() / 1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPreExecute() {
            super.onPreExecute()
            showDialog()
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelDialog()
            if (result!!.isNotEmpty()){
                Toast.makeText(
                    applicationContext,
                    "File saved at $result",
                    Toast.LENGTH_LONG
                ).show()
            }else{
                Toast.makeText(
                    applicationContext,
                    "Something went wrong when saving this file",
                    Toast.LENGTH_LONG
                ).show()
            }
            
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null){
                path, uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(
                    Intent.createChooser(
                        shareIntent, "Share Now"
                    )
                )
            }
        }

        private fun showDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun cancelDialog(){
            mProgressDialog.dismiss()
        }
    }

    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}