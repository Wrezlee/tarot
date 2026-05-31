package putra.yanuar.tarot

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.os.Environment
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.dhaval2404.imagepicker.ImagePicker
import com.permissionx.guolindev.PermissionX
import putra.yanuar.tarot.databinding.ActivityEditProfileBinding
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    lateinit var b: ActivityEditProfileBinding
    lateinit var db: SQLiteDatabase
    lateinit var mediaHelper: MediaHelper
    var userEmail: String = ""
    var fotoStr: String = ""   // Base64 foto baru (kosong = tidak ganti)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        mediaHelper = MediaHelper(this)
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        loadCurrentData()

        // Klik foto → pilih Galeri atau Kamera (persis seperti Mobile 11a)
        b.imgProfileEdit.setOnClickListener { view ->
            val popUp = PopupMenu(this, view)
            popUp.menu.add(0, 0, 0, "Galeri")
            popUp.menu.add(0, 1, 1, "Kamera")
            popUp.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    0 -> { // Galeri
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.action = Intent.ACTION_GET_CONTENT
                        @Suppress("DEPRECATION")
                        startActivityForResult(intent, mediaHelper.getRcGallery())
                        true
                    }
                    1 -> { // Kamera dengan PermissionX
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
                    else -> false
                }
            }
            popUp.show()
        }

        b.btnSaveProfile.setOnClickListener {
            saveChanges()
        }
    }

    fun loadCurrentData() {
        val cursor = db.rawQuery(
            "SELECT name, email, foto FROM users WHERE email = ?",
            arrayOf(userEmail)
        )
        if (cursor.moveToFirst()) {
            b.etEditName.setText(cursor.getString(0))
            b.tvStaticEmail.text = cursor.getString(1)

            // Load foto dari kolom foto (Base64) jika ada
            val fotoBase64 = if (!cursor.isNull(2)) cursor.getString(2) else null
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = android.util.Base64.decode(fotoBase64, android.util.Base64.DEFAULT)
                    val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    b.imgProfileEdit.setImageBitmap(bmp)
                } catch (e: Exception) {
                    b.imgProfileEdit.setImageResource(R.drawable.meow)
                }
            } else {
                b.imgProfileEdit.setImageResource(R.drawable.meow)
            }
        }
        cursor.close()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                fotoStr = mediaHelper.getBitmapToString(uri, b.imgProfileEdit)
            }
        }
    }

    fun saveChanges() {
        val newName = b.etEditName.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            if (fotoStr.isNotEmpty()) {
                // Simpan nama + foto
                db.execSQL(
                    "UPDATE users SET name = ?, foto = ? WHERE email = ?",
                    arrayOf(newName, fotoStr, userEmail)
                )
            } else {
                // Hanya simpan nama
                db.execSQL(
                    "UPDATE users SET name = ? WHERE email = ?",
                    arrayOf(newName, userEmail)
                )
            }
            Toast.makeText(this, "Profil berhasil diperbarui! ✨", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            // Kolom foto belum ada → coba tambahkan dulu
            try {
                db.execSQL("ALTER TABLE users ADD COLUMN foto TEXT DEFAULT ''")
                if (fotoStr.isNotEmpty()) {
                    db.execSQL(
                        "UPDATE users SET name = ?, foto = ? WHERE email = ?",
                        arrayOf(newName, fotoStr, userEmail)
                    )
                } else {
                    db.execSQL(
                        "UPDATE users SET name = ? WHERE email = ?",
                        arrayOf(newName, userEmail)
                    )
                }
                Toast.makeText(this, "Profil berhasil diperbarui! ✨", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e2: Exception) {
                Toast.makeText(this, "Gagal: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}