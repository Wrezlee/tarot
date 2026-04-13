package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAddTarotBinding

class AddTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityAddTarotBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase

        // 1. Siapkan Daftar Saran untuk AutoCompleteTextView
        val daftarKategori = arrayOf("Tarot", "Chat", "Call", "Palm Reading", "Ritual")

        // 2. Buat Adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, daftarKategori)

        // 3. Pasangkan ke AutoCompleteTextView
        b.actvCategory.setAdapter(adapter)

        // Opsional: Langsung munculkan saran saat kolom diklik
        b.actvCategory.setOnClickListener {
            b.actvCategory.showDropDown()
        }

        b.btnSaveTarot.setOnClickListener {
            val name = b.etTarotName.text.toString().trim()
            val category = b.actvCategory.text.toString().trim()
            val price = b.etTarotPrice.text.toString().trim()

            if (name.isNotEmpty() && price.isNotEmpty() && category.isNotEmpty()) {
                try {
                    db.execSQL(
                        "INSERT INTO tarot_packages (name, category, price) VALUES (?, ?, ?)",
                        arrayOf(name, category, price.toInt())
                    )
                    Toast.makeText(this, "Paket $name Berhasil Disimpan ✨", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Harap lengkapi semua kolom!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}