package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.ArrayAdapter
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

        // Setup AutoCompleteTextView untuk kategori
        val daftarKategori = arrayOf("Tarot", "Chat", "Call", "Palm Reading", "Ritual")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, daftarKategori)
        b.actvCategory.setAdapter(adapter)
        b.actvCategory.setOnClickListener { b.actvCategory.showDropDown() }

        // Load data lama termasuk kategori
        if (tarotId.isNotEmpty()) {
            val cursor = db.rawQuery(
                "SELECT name, category, price FROM tarot_packages WHERE id = ?",
                arrayOf(tarotId)
            )
            if (cursor.moveToFirst()) {
                b.etTarotName.setText(cursor.getString(0))
                b.actvCategory.setText(cursor.getString(1), false)
                b.etTarotPrice.setText(cursor.getInt(2).toString())
            }
            cursor.close()
        }

        b.btnSaveTarot.setOnClickListener {
            val name     = b.etTarotName.text.toString().trim()
            val category = b.actvCategory.text.toString().trim()
            val price    = b.etTarotPrice.text.toString().trim()

            if (name.isNotEmpty() && category.isNotEmpty() && price.isNotEmpty()) {
                try {
                    db.execSQL(
                        "UPDATE tarot_packages SET name = ?, category = ?, price = ? WHERE id = ?",
                        arrayOf(name, category, price.toInt(), tarotId)
                    )
                    Toast.makeText(this, "Paket berhasil diperbarui ✨", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Harap lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}