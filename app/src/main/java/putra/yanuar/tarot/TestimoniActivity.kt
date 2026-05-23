package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityTestimoniBinding

class TestimoniActivity : AppCompatActivity() {

    lateinit var b: ActivityTestimoniBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTestimoniBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).readableDatabase
        loadTestimoni()
    }

    fun loadTestimoni() {
        val listData = ArrayList<HashMap<String, String>>()

        try {
            val cursor = db.rawQuery(
                "SELECT u.name, t.message FROM testimonials t JOIN users u ON t.user_id = u.id ORDER BY t.id DESC",
                null
            )
            while (cursor.moveToNext()) {
                val map = HashMap<String, String>()
                map["name"]    = cursor.getString(0) ?: "Anonim"
                map["message"] = cursor.getString(1) ?: "-"
                listData.add(map)
            }
            cursor.close()

            if (listData.isEmpty()) {
                Toast.makeText(this, "Belum ada testimoni", Toast.LENGTH_SHORT).show()
            }

            val adapter = SimpleAdapter(
                this,
                listData,
                R.layout.item_testimoni,
                arrayOf("name", "message"),
                intArrayOf(R.id.tvTestimoniName, R.id.tvTestimoniMessage)
            )
            b.lvTestimoni.adapter = adapter

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}