package putra.yanuar.tarot

import android.app.AlertDialog
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import putra.yanuar.tarot.databinding.ActivityHistoryBinding
import putra.yanuar.tarot.databinding.ItemHistoryBinding

class HistoryActivity : AppCompatActivity() {

    lateinit var b: ActivityHistoryBinding
    lateinit var db: SQLiteDatabase
    lateinit var userEmail: String

    val listData     = ArrayList<HistoryItem>()
    val listDataFull = ArrayList<HistoryItem>()

    var currentQuery  = ""
    var currentStatus = "Semua"

    data class HistoryItem(
        val id: Int,
        val pkg: String,
        val date: String,
        val status: String,
        val question: String,
        val answer: String,
        val readerName: String,
        val userId: Int,
        val hasTestimoni: Boolean,
        val isiUlasan: String,
        val rating: Int
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
        listDataFull.clear()

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

                var hasTestimoni = false
                var isiUlasan    = ""
                var rating       = 0
                val cTesti = db.rawQuery(
                    "SELECT message, rating FROM testimonials WHERE booking_id = ? LIMIT 1",
                    arrayOf(bookingId.toString())
                )
                if (cTesti.moveToFirst()) {
                    hasTestimoni = true
                    isiUlasan    = cTesti.getString(0) ?: ""
                    rating       = cTesti.getInt(1)
                }
                cTesti.close()

                listData.add(
                    HistoryItem(
                        id           = bookingId,
                        pkg          = cursor.getString(1) ?: "Paket Tidak Diketahui",
                        date         = cursor.getString(2) ?: "-",
                        status       = status,
                        question     = "Q: " + (cursor.getString(4) ?: "Belum ada pertanyaan"),
                        answer       = "A: " + (cursor.getString(5) ?: "Menunggu jawaban Reader..."),
                        readerName   = cursor.getString(6) ?: "-",
                        userId       = userId,
                        hasTestimoni = hasTestimoni,
                        isiUlasan    = isiUlasan,
                        rating       = rating
                    )
                )
            }
            cursor.close()

            listDataFull.addAll(listData)
            b.lvHistory.adapter = HistoryAdapter()
            setupFilter()

            if (listData.isEmpty()) {
                Toast.makeText(this, "Belum ada riwayat ramalan", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun setupFilter() {
        val statusList = arrayOf("Semua", "PENDING", "PAID", "PROCESSING", "DONE", "CANCELLED")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerStatusFilter.adapter = spinnerAdapter

        b.spinnerStatusFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentStatus = statusList[pos]
                applyFilter()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        b.searchViewHistory.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim() ?: ""
                applyFilter()
                return true
            }
        })
    }

    fun applyFilter() {
        listData.clear()
        for (item in listDataFull) {
            val matchQuery  = currentQuery.isEmpty() || item.pkg.contains(currentQuery, ignoreCase = true)
            val matchStatus = currentStatus == "Semua" || item.status.equals(currentStatus, ignoreCase = true)
            if (matchQuery && matchStatus) listData.add(item)
        }
        (b.lvHistory.adapter as? HistoryAdapter)?.notifyDataSetChanged()
    }

    fun cancelOrder(bookingId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Pesanan")
            .setMessage("Apakah kamu yakin ingin membatalkan pesanan ini?")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                try {
                    db.execSQL("UPDATE bookings SET status = 'cancelled' WHERE id = ?", arrayOf(bookingId.toString()))
                    Toast.makeText(this, "Pesanan berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                    loadHistory()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membatalkan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    fun showTestimoniDialog(bookingId: Int, packageName: String, userId: Int) {
        var selectedRating = 0

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding, padding, 0)

        val tvLabel = TextView(this)
        tvLabel.text = "Beri rating untuk:\n$packageName"
        tvLabel.textSize = 13f
        tvLabel.setTextColor(0xFF7469B6.toInt())
        tvLabel.setPadding(0, 0, 0, padding / 2)
        layout.addView(tvLabel)

        val starRow = LinearLayout(this)
        starRow.orientation = LinearLayout.HORIZONTAL
        starRow.gravity = android.view.Gravity.CENTER
        starRow.setPadding(0, 0, 0, padding / 2)

        val stars = Array(5) { i ->
            TextView(this).apply {
                text = "☆"
                textSize = 36f
                setTextColor(0xFFAD88C6.toInt())
                setPadding(8, 0, 8, 0)
                starRow.addView(this)
            }
        }

        for (i in 0 until 5) {
            stars[i].setOnClickListener {
                selectedRating = i + 1
                for (j in 0 until 5) {
                    stars[j].text = if (j < selectedRating) "★" else "☆"
                    stars[j].setTextColor(
                        if (j < selectedRating) 0xFFFFB300.toInt() else 0xFFAD88C6.toInt()
                    )
                }
            }
        }

        layout.addView(starRow)

        val etPesan = EditText(this)
        etPesan.hint = "Tulis pengalaman reading-mu... (opsional)"
        etPesan.minLines = 3
        etPesan.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        etPesan.gravity = android.view.Gravity.TOP
        layout.addView(etPesan)

        AlertDialog.Builder(this)
            .setTitle("✨ Tulis Ulasan")
            .setView(layout)
            .setPositiveButton("Kirim Ulasan") { _, _ ->
                if (selectedRating == 0) {
                    Toast.makeText(this, "Pilih rating bintang dulu!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val pesan = etPesan.text.toString().trim()
                try {
                    db.execSQL(
                        """INSERT INTO testimonials (user_id, booking_id, package_name, rating, message, created_at)
                           VALUES (?, ?, ?, ?, ?, datetime('now','localtime'))""",
                        arrayOf(userId.toString(), bookingId.toString(), packageName, selectedRating.toString(), pesan)
                    )
                    Toast.makeText(this, "Ulasan berhasil dikirim! 💕", Toast.LENGTH_SHORT).show()
                    loadHistory()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal kirim: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    fun shareRamalan(item: HistoryItem) {
        val teks = buildString {
            appendLine("🔮 *Tarot Meow — Hasil Ramalan*")
            appendLine()
            appendLine("📦 Paket: ${item.pkg}")
            appendLine("📅 Tanggal: ${item.date}")
            appendLine("👁 Reader: ${item.readerName}")
            appendLine()
            appendLine(item.question)
            appendLine()
            appendLine(item.answer)
            appendLine()
            appendLine("✨ Temukan ramalanmu di Tarot Meow!")
            appendLine("📞 +62 856-4947-1086 | TikTok: @tarotmeow111")
        }

        val options = arrayOf("WhatsApp", "Instagram (Copy Teks)", "Lainnya")
        AlertDialog.Builder(this)
            .setTitle("Bagikan Ramalan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> shareViaWhatsApp(teks)
                    1 -> {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Ramalan", teks))
                        Toast.makeText(this, "Teks disalin! Tempel di caption Instagram.", Toast.LENGTH_LONG).show()
                    }
                    2 -> shareGeneral(teks)
                }
            }
            .show()
    }

    private fun shareViaWhatsApp(teks: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, teks)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
            shareGeneral(teks)
        }
    }

    private fun shareGeneral(teks: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, teks)
            putExtra(Intent.EXTRA_SUBJECT, "Hasil Ramalan Tarot Meow")
        }
        startActivity(Intent.createChooser(intent, "Bagikan via..."))
    }

    inner class HistoryAdapter : android.widget.BaseAdapter() {
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

            binding.tvItemPackage.text  = item.pkg
            binding.tvItemDate.text     = item.date
            binding.tvItemStatus.text   = item.status
            binding.tvItemQuestion.text = item.question
            binding.tvItemAnswer.text   = item.answer
            binding.tvItemReader.text   = "Reader: ${item.readerName}"

            val canCancel = item.status == "PENDING" || item.status == "PAID"
            binding.btnCancelOrder.visibility = if (canCancel) View.VISIBLE else View.GONE
            binding.btnCancelOrder.setOnClickListener { cancelOrder(item.id) }

            val isDone = item.status == "DONE" || item.status == "COMPLETED"

            binding.btnTulisUlasan.visibility = when {
                isDone && !item.hasTestimoni -> View.VISIBLE
                else -> View.GONE
            }
            binding.btnTulisUlasan.setOnClickListener {
                showTestimoniDialog(item.id, item.pkg, item.userId)
            }

            val hasAnswer = item.answer != "A: Menunggu jawaban Reader..." &&
                    item.answer != "A: Belum ada pertanyaan" &&
                    item.answer.length > 3
            binding.btnShareRamalan.visibility = if (isDone && hasAnswer) View.VISIBLE else View.GONE
            binding.btnShareRamalan.setOnClickListener { shareRamalan(item) }

            if (isDone && item.hasTestimoni) {
                binding.layoutUlasanTerkirim.visibility = View.VISIBLE
                binding.tvIsiUlasan.text = item.isiUlasan

                val bintangIsi   = "★".repeat(item.rating)
                val bintangKosong = "☆".repeat(5 - item.rating)
                binding.tvIsiRating.text = bintangIsi + bintangKosong
            } else {
                binding.layoutUlasanTerkirim.visibility = View.GONE
            }

            return view
        }
    }
}