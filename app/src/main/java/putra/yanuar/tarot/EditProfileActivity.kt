package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityEditProfileBinding

class EditProfileActivity : AppCompatActivity() {

    lateinit var b: ActivityEditProfileBinding
    lateinit var db: SQLiteDatabase
    var userEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        // Tampilkan data awal
        loadCurrentData()

        b.btnSaveProfile.setOnClickListener {
            saveChanges()
        }
    }

    fun loadCurrentData() {
        val cursor = db.rawQuery("SELECT name, email FROM users WHERE email = ?", arrayOf(userEmail))
        if (cursor.moveToFirst()) {
            b.etEditName.setText(cursor.getString(0))
            b.tvStaticEmail.text = cursor.getString(1)
        }
        cursor.close()
    }

    fun saveChanges() {
        val newName = b.etEditName.text.toString()

        if (newName.isNotEmpty()) {
            // Update Database
            db.execSQL("UPDATE users SET name = ? WHERE email = ?", arrayOf(newName, userEmail))

            Toast.makeText(this, "Profil berhasil diperbarui! ✨", Toast.LENGTH_SHORT).show()

            // Tutup activity dan kembali ke profil
            finish()
        } else {
            Toast.makeText(this, "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
        }
    }
}