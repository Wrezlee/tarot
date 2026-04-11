package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import putra.yanuar.tarot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    lateinit var b: ActivityMainBinding
    lateinit var db: SQLiteDatabase
    lateinit var helper: DBOpenHelper

    public fun getDbObject(): SQLiteDatabase {
        return db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        helper = DBOpenHelper(this)
        db = helper.writableDatabase

        b.btLogin.setOnClickListener {
            performLogin()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    public fun performLogin() {
        val email = b.edEmail.text.toString().trim()
        val password = b.edPass.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Harap isi email dan password", Toast.LENGTH_SHORT).show()
            return
        }

        val query = "SELECT role FROM users WHERE email = ? AND password = ?"
        val cursor = db.rawQuery(query, arrayOf(email, password))

        if (cursor.moveToFirst()) {
            val role = cursor.getString(cursor.getColumnIndexOrThrow("role"))

            Toast.makeText(this, "Login Berhasil sebagai $role", Toast.LENGTH_SHORT).show()

            val intent = when (role) {
                "admin" -> Intent(this, AdminActivity::class.java)
                "reader" -> Intent(this, ReaderActivity::class.java)
                "customer" -> Intent(this, CustomerActivity::class.java)
                else -> null
            }

            if (intent != null) {
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Role tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Email atau Password Salah!", Toast.LENGTH_SHORT).show()
        }

        cursor.close()
    }
}