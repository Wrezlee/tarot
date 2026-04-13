package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var b: ActivityReaderBinding
    lateinit var db: SQLiteDatabase
    lateinit var userEmail: String

    fun getDbObject(): SQLiteDatabase {
        return db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Ambil email dari intent login
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        db = DBOpenHelper(this).writableDatabase

        // Listeners
        b.btnLogoutReader.setOnClickListener(this)
        b.btnAccept1.setOnClickListener(this)

        loadReaderProfile()
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnAccept1 -> {
                Toast.makeText(this, "Memulai sesi ramalan...", Toast.LENGTH_SHORT).show()
            }
            R.id.btnLogoutReader -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun loadReaderProfile() {
        val sql = "SELECT name FROM users WHERE email = '$userEmail'"
        val c = db.rawQuery(sql, null)
        if (c.moveToFirst()) {
            b.tvReaderGreeting.text = "Selamat Datang,\n${c.getString(0)}"
        }
        c.close()
    }
}