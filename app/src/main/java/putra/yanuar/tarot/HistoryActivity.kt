package putra.yanuar.tarot

import android.app.AlertDialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityHistoryBinding
import putra.yanuar.tarot.databinding.ItemHistoryBinding

class HistoryActivity : AppCompatActivity() {
    lateinit var b: ActivityHistoryBinding
    lateinit var db: SQLiteDatabase
    lateinit var userEmail: String
    val listData = ArrayList<HistoryItem>()

    data class HistoryItem(
        val id: Int,
        val pkg: String,
        val date: String,
        val status: String,
        val question: String,
        val answer: String,
        val readerName: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        loadHistory()
    }

    fun loadHistory() {
        listData.clear()

        val sql = """
            SELECT b.id, b.package_name, b.booking_date, b.status, q.question, q.answer, b.reader_name 
            FROM bookings b 
            LEFT JOIN questions q ON b.id = q.booking_id 
            WHERE b.email = ? 
            ORDER BY b.id DESC
        """.trimIndent()

        try {
            val cursor = db.rawQuery(sql, arrayOf(userEmail))
            while (cursor.moveToNext()) {
                val item = HistoryItem(
                    id = cursor.getInt(0),
                    pkg = cursor.getString(1) ?: "Paket Tidak Diketahui",
                    date = cursor.getString(2) ?: "-",
                    status = (cursor.getString(3) ?: "PENDING").uppercase(),
                    question = "Q: " + (cursor.getString(4) ?: "Belum ada pertanyaan"),
                    answer = "A: " + (cursor.getString(5) ?: "Menunggu jawaban Reader..."),
                    readerName = cursor.getString(6) ?: "-"
                )
                listData.add(item)
            }
            cursor.close()

            b.lvHistory.adapter = HistoryAdapter()

            if (listData.isEmpty()) {
                Toast.makeText(this, "Belum ada riwayat ramalan", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun cancelOrder(bookingId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Pesanan")
            .setMessage("Apakah kamu yakin ingin membatalkan pesanan ini?")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                try {
                    db.execSQL(
                        "UPDATE bookings SET status = 'cancelled' WHERE id = ?",
                        arrayOf(bookingId.toString())
                    )
                    Toast.makeText(this, "Pesanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                    loadHistory()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membatalkan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    inner class HistoryAdapter : BaseAdapter() {
        override fun getCount(): Int = listData.size
        override fun getItem(position: Int): HistoryItem = listData[position]
        override fun getItemId(position: Int): Long = listData[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val binding: ItemHistoryBinding
            val view: View

            if (convertView == null) {
                binding = ItemHistoryBinding.inflate(LayoutInflater.from(this@HistoryActivity), parent, false)
                view = binding.root
                view.tag = binding
            } else {
                binding = convertView.tag as ItemHistoryBinding
                view = convertView
            }

            binding.tvItemPackage.text = item.pkg
            binding.tvItemDate.text = item.date
            binding.tvItemStatus.text = item.status
            binding.tvItemQuestion.text = item.question
            binding.tvItemAnswer.text = item.answer
            binding.tvItemReader.text = "Reader: ${item.readerName}"

            // Tampilkan tombol batalkan hanya untuk status PENDING atau PAID
            val canCancel = item.status == "PENDING" || item.status == "PAID"
            binding.btnCancelOrder.visibility = if (canCancel) View.VISIBLE else View.GONE

            binding.btnCancelOrder.setOnClickListener {
                cancelOrder(item.id)
            }

            return view
        }
    }
}