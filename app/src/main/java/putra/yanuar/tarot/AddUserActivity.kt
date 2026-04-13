package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAddUserBinding

class AddUserActivity : AppCompatActivity() {

    lateinit var b: ActivityAddUserBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase

        b.btnSave.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()
            val role = when {
                b.rbAdmin.isChecked -> "admin"
                b.rbReader.isChecked -> "reader"
                else -> "customer"
            }

            if (name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty()) {
                try {
                    db.execSQL(
                        "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?)",
                        arrayOf(name, email, pass, role)
                    )
                    Toast.makeText(this, "Berhasil menambah $name ✨", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Harap lengkapi semua data!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}