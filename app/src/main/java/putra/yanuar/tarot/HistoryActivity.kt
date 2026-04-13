package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
    lateinit var b: ActivityHistoryBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).readableDatabase
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        loadHistoryWithListView(userEmail)
    }

    fun loadHistoryWithListView(email: String) {
        val listData = ArrayList<HashMap<String, String>>()

        // QUERY DIPERBAIKI: Mengambil package_name langsung dari tabel bookings
        // dan melakukan LEFT JOIN ke tabel questions untuk mengambil jawaban
        val sql = """
            SELECT b.package_name, b.booking_date, b.status, q.question, q.answer 
            FROM bookings b 
            LEFT JOIN questions q ON b.id = q.booking_id 
            WHERE b.email = ? 
            ORDER BY b.id DESC
        """.trimIndent()

        try {
            val cursor = db.rawQuery(sql, arrayOf(email))
            while (cursor.moveToNext()) {
                val map = HashMap<String, String>()
                map["pkg"] = cursor.getString(0) ?: "Paket Tidak Diketahui"
                map["date"] = cursor.getString(1) ?: "-"
                map["status"] = (cursor.getString(2) ?: "PENDING").uppercase()
                map["q"] = "Q: " + (cursor.getString(3) ?: "Belum ada pertanyaan")
                map["a"] = "A: " + (cursor.getString(4) ?: "Menunggu jawaban Reader...")
                listData.add(map)
            }
            cursor.close()

            val adapter = SimpleAdapter(
                this,
                listData,
                R.layout.item_history,
                arrayOf("pkg", "date", "status", "q", "a"),
                intArrayOf(
                    R.id.tvItemPackage,
                    R.id.tvItemDate,
                    R.id.tvItemStatus,
                    R.id.tvItemQuestion,
                    R.id.tvItemAnswer
                )
            )

            b.lvHistory.adapter = adapter

            if (listData.isEmpty()) {
                Toast.makeText(this, "Belum ada riwayat ramalan", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}