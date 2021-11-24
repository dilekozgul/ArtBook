package com.dilekozgul.artbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dilekozgul.artbook.databinding.ActivityArtBinding
import com.dilekozgul.artbook.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.lang.Exception

class ArtActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArtBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    var selectedBitmap :Bitmap? = null
    private  lateinit var permissonLauncher: ActivityResultLauncher<String>
    private lateinit var database : SQLiteDatabase





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if (info.equals("new"))
        {
            binding.tvArtName.setText("")
            binding.tvArtistName.setText("")
            binding.tvArtYear.setText("")
            binding.imgArt.setImageResource(R.drawable.selecetimage)
            binding.btnSave.visibility = View.VISIBLE


        }
        else
        {
            binding.btnSave.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistnameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext())
            {
                binding.tvArtName.setText(cursor.getString(artNameIx))
                binding.tvArtistName.setText(cursor.getString(artistnameIx))
                binding.tvArtYear.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imgArt.setImageBitmap(bitmap)



            }
            cursor.close()
        }




    }
    fun save(view: View){

        val artName = binding.tvArtName.text.toString()
        val artistName = binding.tvArtistName.text.toString()
        val year = binding.tvArtYear.text.toString()


        if (selectedBitmap != null)
        {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                //val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY,artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString= "INSERT INTO arts (artname, artistname, year, image) VALUES(?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e:Exception)
            {
                e.printStackTrace()
            }

            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)


        }
    }

    private fun  makeSmallerBitmap(image : Bitmap, maximumSize: Int):Bitmap
    {
        var width = image.width
        var height= image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()

        if(bitmapRatio>1)
        {
            //landscape
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()

        }
        else
        {
            //portrait
            height = maximumSize
            val scaledWidht = height*bitmapRatio
            width = scaledWidht.toInt()

        }
        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    fun imgSelectArt(view:View){

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
        {
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE))
            {

                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission", View.OnClickListener {

                    //request permisson
                    permissonLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }).show()

            } else {


                //request permisson
                    permissonLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }



        }
        else
        {
            //intent gallery
            val intentToGallery= Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGallery)

        }



    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result->

            if (result.resultCode== RESULT_OK)
            {
                val intentFromResult = result.data
                if(intentFromResult != null){

                    val imageData = intentFromResult.data
                    //binding.imgArt.setImageURI(imageData)

                    if(imageData != null){
                        try {
                            if (Build.VERSION.SDK_INT>=28)
                            {
                                val source = ImageDecoder.createSource(this.contentResolver,imageData)
                                selectedBitmap = ImageDecoder.decodeBitmap(source)
                                binding.imgArt.setImageBitmap(selectedBitmap)


                            }else
                            {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(contentResolver,imageData)
                                binding.imgArt.setImageBitmap(selectedBitmap)
                            }


                        }catch (e:Exception){
                            e.printStackTrace()
                        }
                    }
                }


            }


        }

        permissonLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ result ->

            if (result){

                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)




            }else
            {
                //permission denied

                Toast.makeText(this, "Permission needed!", Toast.LENGTH_LONG).show()


            }

        }



    }


}