package putra.yanuar.tarot

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.permissionx.guolindev.PermissionX
import mumayank.com.airlocationlibrary.AirLocation
import putra.yanuar.tarot.databinding.ActivityReaderEditProfileBinding
import java.io.File

class ReaderEditProfileActivity : AppCompatActivity() {

    lateinit var b: ActivityReaderEditProfileBinding
    lateinit var db: SQLiteDatabase
    lateinit var mediaHelper: MediaHelper

    var userEmail: String = ""
    var fotoStr: String = ""
    var lat: Double = 0.0
    var lng: Double = 0.0

    // Deklarasi AirLocation — sama persis pola dari mobile12a
    private val airLocation = AirLocation(this, object : AirLocation.Callback {
        override fun onFailure(locationFailedEnum: AirLocation.LocationFailedEnum) {
            Toast.makeText(this@ReaderEditProfileActivity, "Gagal mengambil lokasi GPS", Toast.LENGTH_SHORT).show()
        }

        override fun onSuccess(locations: ArrayList<Location>) {
            lat = locations[0].latitude
            lng = locations[0].longitude
            b.tvLocationStatus.text = "📍 Lokasi tersimpan: %.6f, %.6f".format(lat, lng)
            Toast.makeText(this@ReaderEditProfileActivity, "Lokasi berhasil diambil!", Toast.LENGTH_SHORT).show()
        }
    }, false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderEditProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        mediaHelper = MediaHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        loadCurrentData()

        b.btnReaderChangePhoto.setOnClickListener { view ->
            val popUp = PopupMenu(this, view)
            popUp.menu.add(0, 0, 0, "Kamera")
            popUp.menu.add(0, 1, 1, "Galeri")
            popUp.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> {
                        PermissionX.init(this)
                            .permissions(Manifest.permission.CAMERA)
                            .request { allGranted, _, _ ->
                                if (allGranted) {
                                    val mediaPath = File(
                                        Environment.getExternalStoragePublicDirectory(
                                            Environment.DIRECTORY_PICTURES
                                        ), "TarotMeow"
                                    )
                                    if (!mediaPath.exists()) mediaPath.mkdirs()
                                    ImagePicker.with(this)
                                        .cameraOnly()
                                        .saveDir(mediaPath)
                                        .start()
                                } else {
                                    Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
                                }
                            }
                        true
                    }
                    1 -> {
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.action = Intent.ACTION_GET_CONTENT
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, mediaHelper.getRcGallery())
                        true
                    }
                    else -> false
                }
            }
            popUp.show()
        }

        b.btnAmbilLokasi.setOnClickListener {
            airLocation.start()
        }

        b.btnSaveReaderProfile.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadCurrentData() {
        val cursor = db.rawQuery(
            "SELECT name, email, foto, lat, lng FROM users WHERE email = ?",
            arrayOf(userEmail)
        )
        if (cursor.moveToFirst()) {
            b.etReaderEditName.setText(cursor.getString(0))
            b.tvReaderStaticEmail.text = cursor.getString(1)

            val fotoBase64 = if (!cursor.isNull(2)) cursor.getString(2) else null
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    b.imgReaderProfileEdit.setImageBitmap(bmp)
                } catch (e: Exception) {
                    b.imgReaderProfileEdit.setImageResource(R.drawable.meow)
                }
            } else {
                b.imgReaderProfileEdit.setImageResource(R.drawable.meow)
            }

            lat = if (!cursor.isNull(3)) cursor.getDouble(3) else 0.0
            lng = if (!cursor.isNull(4)) cursor.getDouble(4) else 0.0
            b.tvLocationStatus.text = if (lat != 0.0 || lng != 0.0) {
                "📍 Lokasi tersimpan: %.6f, %.6f".format(lat, lng)
            } else {
                "Lokasi belum diatur"
            }
        }
        cursor.close()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        airLocation.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                fotoStr = mediaHelper.getBitmapToString(uri, b.imgReaderProfileEdit)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        airLocation.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun saveChanges() {
        val newName     = b.etReaderEditName.text.toString().trim()
        val newPass     = b.etReaderEditPassword.text.toString().trim()
        val confirmPass = b.etReaderEditPasswordConfirm.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPass.isNotEmpty()) {
            if (newPass.length < 6) {
                Toast.makeText(this, "Password minimal 6 karakter", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPass != confirmPass) {
                Toast.makeText(this, "Konfirmasi password tidak cocok!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        try {
            when {
                fotoStr.isNotEmpty() && newPass.isNotEmpty() ->
                    db.execSQL("UPDATE users SET name=?, foto=?, password=?, lat=?, lng=? WHERE email=?",
                        arrayOf(newName, fotoStr, newPass, lat.toString(), lng.toString(), userEmail))
                fotoStr.isNotEmpty() ->
                    db.execSQL("UPDATE users SET name=?, foto=?, lat=?, lng=? WHERE email=?",
                        arrayOf(newName, fotoStr, lat.toString(), lng.toString(), userEmail))
                newPass.isNotEmpty() ->
                    db.execSQL("UPDATE users SET name=?, password=?, lat=?, lng=? WHERE email=?",
                        arrayOf(newName, newPass, lat.toString(), lng.toString(), userEmail))
                else ->
                    db.execSQL("UPDATE users SET name=?, lat=?, lng=? WHERE email=?",
                        arrayOf(newName, lat.toString(), lng.toString(), userEmail))
            }

            val msg = if (newPass.isNotEmpty())
                "Profil & password berhasil diperbarui!"
            else
                "Profil berhasil diperbarui!"

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()

        } catch (e: Exception) {
            try { db.execSQL("ALTER TABLE users ADD COLUMN foto TEXT DEFAULT ''") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN lat REAL DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN lng REAL DEFAULT 0") } catch (_: Exception) {}
            try {
                when {
                    fotoStr.isNotEmpty() && newPass.isNotEmpty() ->
                        db.execSQL("UPDATE users SET name=?, foto=?, password=?, lat=?, lng=? WHERE email=?",
                            arrayOf(newName, fotoStr, newPass, lat.toString(), lng.toString(), userEmail))
                    fotoStr.isNotEmpty() ->
                        db.execSQL("UPDATE users SET name=?, foto=?, lat=?, lng=? WHERE email=?",
                            arrayOf(newName, fotoStr, lat.toString(), lng.toString(), userEmail))
                    newPass.isNotEmpty() ->
                        db.execSQL("UPDATE users SET name=?, password=?, lat=?, lng=? WHERE email=?",
                            arrayOf(newName, newPass, lat.toString(), lng.toString(), userEmail))
                    else ->
                        db.execSQL("UPDATE users SET name=?, lat=?, lng=? WHERE email=?",
                            arrayOf(newName, lat.toString(), lng.toString(), userEmail))
                }
                Toast.makeText(this, "Profil berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e2: Exception) {
                Toast.makeText(this, "Gagal: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}