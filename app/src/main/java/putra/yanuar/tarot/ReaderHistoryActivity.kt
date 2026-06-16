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
import putra.yanuar.tarot.databinding.ActivityReaderHistoryBinding
import putra.yanuar.tarot.databinding.ItemCustomerHistoryBinding

class ReaderHistoryActivity : AppCompatActivity() {

    lateinit var b: ActivityReaderHistoryBinding
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
        val time: String,
        val status: String,
        val notes: String,
        val readerName: String,
        val userId: Int,
        val hasTestimoni: Boolean,
        val isiUlasan: String,
        val rating: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderHistoryBinding.inflate(layoutInflater)
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

        try {
            val cursor = db.rawQuery(
                """SELECT b.id, b.package_name, b.booking_date, b.status, b.notes, b.reader_name, b.booking_time
                   FROM bookings b
                   WHERE b.email = ?
                   ORDER BY b.id DESC""",
                arrayOf(userEmail)
            )

            while (cursor.moveToNext()) {
                val bookingId = cursor.getInt(0)
                val status    = (cursor.getString(3) ?: "PENDING").uppercase()

                var hasTestimoni = false; var isiUlasan = ""; var rating = 0
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

                listData.add(HistoryItem(
                    id           = bookingId,
                    pkg          = cursor.getString(1) ?: "Paket Tidak Diketahui",
                    date         = cursor.getString(2) ?: "-",
                    time         = cursor.getString(6) ?: "--:--",
                    status       = status,
                    notes        = cursor.getString(4) ?: "",
                    readerName   = cursor.getString(5) ?: "-",
                    userId       = userId,
                    hasTestimoni = hasTestimoni,
                    isiUlasan    = isiUlasan,
                    rating       = rating
                ))
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
        val statusList = arrayOf("Semua", "PENDING", "PAID", "PROCESSING", "DONE", "COMPLETED", "CANCELLED")
        val spinnerAdapter = ArrayAdapter(this, R.layout.item_spinner, statusList)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        b.spinnerStatusFilter.adapter = spinnerAdapter

        b.spinnerStatusFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentStatus = statusList[pos]; applyFilter()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        b.searchViewHistory.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim() ?: ""; applyFilter(); return true
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

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (16 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, 0)
        }
        val p = (16 * resources.displayMetrics.density).toInt()

        val tvLabel = TextView(this).apply {
            text = "Beri rating untuk:\n$packageName"
            textSize = 13f
            setTextColor(0xFF7469B6.toInt())
            setPadding(0, 0, 0, p / 2)
        }
        layout.addView(tvLabel)

        val starRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, p / 2)
        }

        val stars = Array(5) { _ ->
            TextView(this).apply {
                text = "☆"; textSize = 36f
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
                    stars[j].setTextColor(if (j < selectedRating) 0xFFFFB300.toInt() else 0xFFAD88C6.toInt())
                }
            }
        }
        layout.addView(starRow)

        val etPesan = EditText(this).apply {
            hint = "Tulis pengalaman reading-mu... (opsional)"
            minLines = 3
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = android.view.Gravity.TOP
        }
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
            if (item.notes.isNotEmpty()) appendLine("Q: ${item.notes}")
            appendLine()
            appendLine("✨ Temukan ramalanmu di Tarot Meow!")
            appendLine("📞 +62 856-4947-1086 | TikTok: @tarotmeow111")
        }

        AlertDialog.Builder(this)
            .setTitle("Bagikan Ramalan")
            .setItems(arrayOf("WhatsApp", "Bagikan ke Aplikasi Lain")) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; setPackage("com.whatsapp")
                                putExtra(Intent.EXTRA_TEXT, teks)
                            }
                            startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, teks)
                                putExtra(Intent.EXTRA_SUBJECT, "Hasil Ramalan Tarot Meow")
                            }
                            startActivity(Intent.createChooser(intent, "Bagikan via..."))
                        }
                    }
                    1 -> {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, teks)
                            putExtra(Intent.EXTRA_SUBJECT, "Hasil Ramalan Tarot Meow")
                        }
                        startActivity(Intent.createChooser(intent, "Bagikan via..."))
                    }
                }
            }
            .show()
    }

    inner class HistoryAdapter : android.widget.BaseAdapter() {
        override fun getCount(): Int = listData.size
        override fun getItem(pos: Int): HistoryItem = listData[pos]
        override fun getItemId(pos: Int): Long = listData[pos].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val binding: ItemCustomerHistoryBinding
            val view: View

            if (convertView == null) {
                binding = ItemCustomerHistoryBinding.inflate(
                    LayoutInflater.from(this@ReaderHistoryActivity), parent, false
                )
                view = binding.root
                view.tag = binding
            } else {
                binding = convertView.tag as ItemCustomerHistoryBinding
                view = convertView
            }

            binding.tvItemPackage.text  = item.pkg
            binding.tvItemDate.text     = "📅 ${item.date}  ⏰ ${item.time}"
            binding.tvItemStatus.text   = item.status
            binding.tvItemQuestion.text = if (item.notes.isNotEmpty()) "Q: ${item.notes}" else "Q: -"
            binding.tvItemAnswer.text   = "A: Lihat di riwayat lengkap"
            binding.tvItemReader.text   = "Reader: ${item.readerName}"

            val canCancel = item.status in listOf("PENDING", "PAID")
            binding.btnCancelOrder.visibility = if (canCancel) View.VISIBLE else View.GONE
            if (canCancel) binding.btnCancelOrder.setOnClickListener { cancelOrder(item.id) }

            val isDone = item.status in listOf("DONE", "COMPLETED")

            binding.btnTulisUlasan.visibility = when {
                isDone && !item.hasTestimoni -> View.VISIBLE
                else -> View.GONE
            }
            if (isDone && !item.hasTestimoni) {
                binding.btnTulisUlasan.setOnClickListener {
                    showTestimoniDialog(item.id, item.pkg, item.userId)
                }
            }

            binding.btnShareRamalan.visibility = if (isDone) View.VISIBLE else View.GONE
            if (isDone) binding.btnShareRamalan.setOnClickListener { shareRamalan(item) }

            if (isDone && item.hasTestimoni) {
                binding.layoutUlasanTerkirim.visibility = View.VISIBLE
                binding.tvIsiUlasan.text = item.isiUlasan
                binding.tvIsiRating.text = "★".repeat(item.rating) + "☆".repeat(5 - item.rating)
            } else {
                binding.layoutUlasanTerkirim.visibility = View.GONE
            }

            // QR Ticket
            binding.layoutQrTicket.visibility = View.GONE
            if (item.status !in listOf("CANCELLED")) {
                try {
                    val c = db.rawQuery("SELECT qr_content FROM bookings WHERE id = ?", arrayOf(item.id.toString()))
                    var qrContent = if (c.moveToFirst()) c.getString(0) ?: "" else ""
                    c.close()

                    if (qrContent.isEmpty()) {
                        qrContent = QrHelper.buildQrContent(
                            bookingId    = item.id.toString(),
                            customerId   = item.userId.toString(),
                            customerName = "",
                            packageName  = item.pkg,
                            date         = item.date,
                            time         = item.time
                        )
                        try { db.execSQL("ALTER TABLE bookings ADD COLUMN qr_content TEXT DEFAULT ''") } catch (_: Exception) {}
                        db.execSQL("UPDATE bookings SET qr_content = ? WHERE id = ?", arrayOf(qrContent, item.id.toString()))
                    }

                    binding.layoutQrTicket.visibility = View.VISIBLE
                    val bmp = QrHelper.generateQr(qrContent, sizePx = 300)
                    binding.imgQrCode.setImageBitmap(bmp)
                    binding.tvQrBookingId.text = "Booking #${item.id}"
                    binding.btnSaveQr.setOnClickListener {
                        // simpan QR — delegasi ke activity
                        Toast.makeText(this@ReaderHistoryActivity, "Fungsi simpan QR tersedia di riwayat booking", Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {}
            }

            return view
        }
    }
}