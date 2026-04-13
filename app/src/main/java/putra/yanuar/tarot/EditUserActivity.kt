package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityEditUserBinding

class EditUserActivity : AppCompatActivity() {

    lateinit var b: ActivityEditUserBinding
    lateinit var db: SQLiteDatabase
    var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        userId = intent.getStringExtra("USER_ID") ?: ""

        // Load data lama ke form
        if (userId.isNotEmpty()) {
            val cursor = db.rawQuery("SELECT name, email, password, role FROM users WHERE id = ?", arrayOf(userId))
            if (cursor.moveToFirst()) {
                b.etName.setText(cursor.getString(0))
                b.etEmail.setText(cursor.getString(1))
                b.etPassword.setText(cursor.getString(2))
                when (cursor.getString(3).lowercase()) {
                    "admin" -> b.rbAdmin.isChecked = true
                    "reader" -> b.rbReader.isChecked = true
                    "customer" -> b.rbCustomer.isChecked = true
                }
            }
            cursor.close()
        }

        b.btnSave.setOnClickListener {
            val name = b.etName.text.toString().trim()
            val email = b.etEmail.text.toString().trim()
            val pass = b.etPassword.text.toString().trim()
            val role = when {
                b.rbAdmin.isChecked -> "admin"
                b.rbReader.isChecked -> "reader"
                else -> "customer"
            }

            if (name.isNotEmpty() && email.isNotEmpty()) {
                try {
                    db.execSQL(
                        "UPDATE users SET name = ?, email = ?, password = ?, role = ? WHERE id = ?",
                        arrayOf(name, email, pass, role, userId)
                    )
                    Toast.makeText(this, "Data $name diperbarui ✨", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}