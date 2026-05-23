package putra.yanuar.tarot

import android.app.AlertDialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.EditText
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
        val readerName: String,
        val userId: Int,
        val hasTestimoni: Boolean
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

        // Ambil user_id dulu
        var userId = 0
        val cUser = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(userEmail))
        if (cUser.moveToFirst()) userId = cUser.getInt(0)
        cUser.close()

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
                val bookingId = cursor.getInt(0)
                val status = (cursor.getString(3) ?: "PENDING").uppercase()

                // Cek apakah booking ini sudah ada testimoninya
                var hasTestimoni = false
                val cTesti = db.rawQuery(
                    "SELECT COUNT(*) FROM testimonials WHERE booking_id = ?",
                    arrayOf(bookingId.toString())
                )
                if (cTesti.moveToFirst()) hasTestimoni = cTesti.getInt(0) > 0
                cTesti.close()

                val item = HistoryItem(
                    id          = bookingId,
                    pkg         = cursor.getString(1) ?: "Paket Tidak Diketahui",
                    date        = cursor.getString(2) ?: "-",
                    status      = status,
                    question    = "Q: " + (cursor.getString(4) ?: "Belum ada pertanyaan"),
                    answer      = "A: " + (cursor.getString(5) ?: "Menunggu jawaban Reader..."),
                    readerName  = cursor.getString(6) ?: "-",
                    userId      = userId,
                    hasTestimoni = hasTestimoni
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

    /**
     * Dialog tulis ulasan — muncul dari item history yang sudah selesai.
     * Menyimpan booking_id dan package_name ke tabel testimonials.
     */
    fun showTestimoniDialog(bookingId: Int, packageName: String, userId: Int) {
        val input = EditText(this)
        input.hint = "Tulis pengalaman reading-mu..."
        input.minLines = 3
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        input.gravity = android.view.Gravity.TOP
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, 0)

        AlertDialog.Builder(this)
            .setTitle("✨ Tulis Ulasan")
            .setMessage("Bagikan pengalamanmu untuk paket:\n$packageName")
            .setView(input)
            .setPositiveButton("Kirim Ulasan") { _, _ ->
                val pesan = input.text.toString().trim()
                if (pesan.isEmpty()) {
                    Toast.makeText(this, "Ulasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    db.execSQL(
                        """INSERT INTO testimonials (user_id, booking_id, package_name, message, created_at) 
                           VALUES (?, ?, ?, ?, datetime('now','localtime'))""",
                        arrayOf(userId.toString(), bookingId.toString(), packageName, pesan)
                    )
                    Toast.makeText(this, "Ulasan berhasil dikirim! 💕", Toast.LENGTH_SHORT).show()
                    loadHistory() // refresh supaya tombol ulasan hilang
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal kirim ulasan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
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
                binding = ItemHistoryBinding.inflate(
                    LayoutInflater.from(this@HistoryActivity), parent, false
                )
                view = binding.root
                view.tag = binding
            } else {
                binding = convertView.tag as ItemHistoryBinding
                view = convertView
            }

            binding.tvItemPackage.text  = item.pkg
            binding.tvItemDate.text     = item.date
            binding.tvItemStatus.text   = item.status
            binding.tvItemQuestion.text = item.question
            binding.tvItemAnswer.text   = item.answer
            binding.tvItemReader.text   = "Reader: ${item.readerName}"

            // Tombol batalkan — hanya untuk PENDING atau PAID
            val canCancel = item.status == "PENDING" || item.status == "PAID"
            binding.btnCancelOrder.visibility = if (canCancel) View.VISIBLE else View.GONE
            binding.btnCancelOrder.setOnClickListener { cancelOrder(item.id) }

            // Tombol ulasan — hanya untuk status selesai dan belum ada testimoninya
            val isDone = item.status == "DONE" || item.status == "COMPLETED"
            binding.btnTulisUlasan.visibility = when {
                isDone && !item.hasTestimoni -> View.VISIBLE
                else -> View.GONE
            }
            binding.btnTulisUlasan.setOnClickListener {
                showTestimoniDialog(item.id, item.pkg, item.userId)
            }

            // Jika sudah ada testimoni, tampilkan label kecil
            binding.tvSudahUlasan.visibility = if (isDone && item.hasTestimoni) View.VISIBLE else View.GONE

            return view
        }
    }
}