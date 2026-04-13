package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityEditTarotBinding

class EditTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityEditTarotBinding
    lateinit var db: SQLiteDatabase
    var tarotId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityEditTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        tarotId = intent.getStringExtra("TAROT_ID") ?: ""

        // Load data paket lama
        if (tarotId.isNotEmpty()) {
            val cursor = db.rawQuery("SELECT name, price FROM tarot_packages WHERE id = ?", arrayOf(tarotId))
            if (cursor.moveToFirst()) {
                b.etTarotName.setText(cursor.getString(0))
                b.etTarotPrice.setText(cursor.getInt(1).toString())
            }
            cursor.close()
        }

        b.btnSaveTarot.setOnClickListener {
            val name = b.etTarotName.text.toString().trim()
            val price = b.etTarotPrice.text.toString().trim()

            if (name.isNotEmpty() && price.isNotEmpty()) {
                try {
                    db.execSQL(
                        "UPDATE tarot_packages SET name = ?, price = ? WHERE id = ?",
                        arrayOf(name, price.toInt(), tarotId)
                    )
                    Toast.makeText(this, "Paket berhasil diperbarui ✨", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}