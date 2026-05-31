package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding
import putra.yanuar.tarot.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReaderActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase
    private var userEmail: String = ""
    private var readerId: Int = 0
    private var readerName: String = ""
    private var currentBookingId: Int = 0
    private var isProcessing: Boolean = false

    data class ReaderHistoryItem(
        val id: Int,
        val customerName: String,
        val packageName: String,
        val bookingDate: String,
        val status: String,
        val question: String,
        val answer: String
    )

    val historyList = ArrayList<ReaderHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(b.root)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        try {
            db = DBOpenHelper(this).writableDatabase
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal buka database: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            val cursorReader = db.rawQuery(
                "SELECT id, name, is_online FROM users WHERE email = ?",
                arrayOf(userEmail)
            )
            if (cursorReader.moveToFirst()) {
                readerId   = cursorReader.getInt(0)
                readerName = cursorReader.getString(1) ?: "Reader"
                val isOnline = cursorReader.getInt(2) == 1
                b.switchStatus.isChecked = isOnline
                b.tvStatusLabel.text = if (isOnline) "Online & Active" else "Offline / Sibuk"
            }
            cursorReader.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error baca data reader: ${e.message}", Toast.LENGTH_LONG).show()
        }

        try {
            setSupportActionBar(b.toolbarReader)
            supportActionBar?.setDisplayShowTitleEnabled(true)
            supportActionBar?.title = "Tarot Meow"
        } catch (e: Exception) {
            e.printStackTrace()
        }

        b.btnStartReading.setOnClickListener(this)

        b.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            try {
                if (isChecked) {
                    b.tvStatusLabel.text = "Online & Active"
                    db.execSQL("UPDATE users SET is_online = 1 WHERE id = ?", arrayOf(readerId.toString()))
                    Toast.makeText(this, "Status Reader: Online", Toast.LENGTH_SHORT).show()
                } else {
                    b.tvStatusLabel.text = "Offline / Sibuk"
                    db.execSQL("UPDATE users SET is_online = 0 WHERE id = ?", arrayOf(readerId.toString()))
                    Toast.makeText(this, "Status Reader: Offline", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal update status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        loadReaderProfile()
        loadStats()
        loadEarnings()
        loadNextBooking()
        loadCalendarBookings()
        loadReadingHistory()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reader_menu_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about  -> { showAboutDialog(); true }
            R.id.menu_logout -> { logout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔮 Tarot Meow")
            .setMessage(
                "Versi: 1.0\n\n" +
                        "Tarot Meow adalah aplikasi layanan pembacaan tarot profesional yang " +
                        "menghubungkan pelanggan dengan reader berpengalaman.\n\n" +
                        "Fitur Reader:\n" +
                        "• Kelola status online/offline\n" +
                        "• Lihat antrean booking pelanggan\n" +
                        "• Mulai & selesaikan sesi ramalan\n" +
                        "• Jawab pertanyaan pelanggan\n" +
                        "• Statistik pendapatan harian & mingguan\n" +
                        "• Kalender booking minggu ini\n" +
                        "• Catatan pribadi per sesi\n\n" +
                        "Dikembangkan oleh kelompok 1 PSI\n" +
                        "Fateema Az Zahra                     (243107030134)\n" +
                        "Maharani Citra Dwi Syahputri  (243107030140)\n" +
                        "Moch. Yanuar Putra Wibowo   (243107030146)\n" +
                        "Muh. Muafan Al Farisi              (243107030079)\n" +
                        "© 2026 Tarot Meow. All rights reserved."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnStartReading -> {
                if (isProcessing) showReadingOptions() else startReading()
            }
        }
    }

    // ─── STATS ───────────────────────────────────────────────────────────────

    private fun loadStats() {
        try {
            val cursorPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID') AND (reader_id = ? OR reader_id = 0)",
                arrayOf(readerId.toString())
            )
            if (cursorPending.moveToFirst()) b.tvPendingCount.text = cursorPending.getInt(0).toString()
            cursorPending.close()

            val cursorDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('completed','COMPLETED','done','DONE') AND reader_id = ?",
                arrayOf(readerId.toString())
            )
            if (cursorDone.moveToFirst()) b.tvCompletedCount.text = cursorDone.getInt(0).toString()
            cursorDone.close()
        } catch (e: Exception) {
            b.tvPendingCount.text   = "0"
            b.tvCompletedCount.text = "0"
        }
    }

    // ─── PENDAPATAN ───────────────────────────────────────────────────────────

    private fun loadEarnings() {
        try {
            val today = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY))
            val startOfWeek = cal.time

            val allBookings = db.rawQuery(
                """SELECT total_price, booking_date FROM bookings
                   WHERE reader_id = ?
                   AND status IN ('completed','COMPLETED','done','DONE')""",
                arrayOf(readerId.toString())
            )

            var totalAll   = 0
            var totalToday = 0
            var totalWeek  = 0
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

            while (allBookings.moveToNext()) {
                val price      = allBookings.getInt(0)
                val dateStr    = allBookings.getString(1) ?: ""
                totalAll += price

                try {
                    val bookingDate = sdf.parse(dateStr)
                    if (bookingDate != null) {
                        if (dateStr == today) totalToday += price
                        if (!bookingDate.before(startOfWeek)) totalWeek += price
                    }
                } catch (ex: Exception) {}
            }
            allBookings.close()

            b.tvEarningTotal.text = "Rp${formatRupiah(totalAll)}"
            b.tvEarningToday.text = "Rp${formatRupiah(totalToday)}"
            b.tvEarningWeek.text  = "Rp${formatRupiah(totalWeek)}"

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatRupiah(amount: Int): String {
        return if (amount >= 1_000_000) {
            String.format("%.1fjt", amount / 1_000_000.0)
        } else if (amount >= 1_000) {
            String.format("%dK", amount / 1_000)
        } else {
            amount.toString()
        }
    }

    // ─── KALENDER BOOKING ────────────────────────────────────────────────────

    private fun loadCalendarBookings() {
        try {
            val container = b.containerCalendar
            container.removeAllViews()

            val cal      = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val calStart = Calendar.getInstance()
            calStart.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY))

            val calEnd = Calendar.getInstance()
            calEnd.add(Calendar.DAY_OF_MONTH, (Calendar.SUNDAY - dayOfWeek + 7) % 7)

            val sdf      = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val startStr = sdf.format(calStart.time)
            val endStr   = sdf.format(calEnd.time)

            val cursor = db.rawQuery(
                """SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.booking_time, b.status
                   FROM bookings b
                   LEFT JOIN users u ON b.email = u.email
                   WHERE (b.reader_id = ? OR b.reader_id = 0)
                   AND b.status NOT IN ('cancelled','CANCELLED','completed','COMPLETED','done','DONE')
                   ORDER BY b.booking_date ASC, b.booking_time ASC""",
                arrayOf(readerId.toString())
            )

            val bookingsThisWeek = ArrayList<Array<String>>()
            while (cursor.moveToNext()) {
                val dateStr = cursor.getString(4) ?: ""
                try {
                    val bookDate  = sdf.parse(dateStr) ?: continue
                    val startDate = sdf.parse(startStr) ?: continue
                    val endDate   = sdf.parse(endStr) ?: continue
                    if (!bookDate.before(startDate) && !bookDate.after(endDate)) {
                        bookingsThisWeek.add(arrayOf(
                            cursor.getInt(0).toString(),
                            cursor.getString(1) ?: cursor.getString(2) ?: "-",
                            cursor.getString(3) ?: "-",
                            dateStr,
                            cursor.getString(5) ?: "--:--",
                            (cursor.getString(6) ?: "pending").uppercase()
                        ))
                    }
                } catch (ex: Exception) {}
            }
            cursor.close()

            if (bookingsThisWeek.isEmpty()) {
                val tvEmpty = TextView(this).apply {
                    text = "Tidak ada booking minggu ini"
                    setTextColor(0xFFAD88C6.toInt())
                    textSize = 13f
                    setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                container.addView(tvEmpty)
                return
            }

            for (booking in bookingsThisWeek) {
                val bookingId   = booking[0]
                val customerName = booking[1]
                val packageName  = booking[2]
                val dateStr      = booking[3]
                val timeStr      = booking[4]
                val status       = booking[5]

                val statusColor = when (status) {
                    "PAID"       -> 0xFF4CAF50.toInt()
                    "PROCESSING" -> 0xFFFF9800.toInt()
                    else         -> 0xFFAD88C6.toInt()
                }

                val card = com.google.android.material.card.MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 8.dpToPx() }
                    radius = 16f.dpToFloat()
                    cardElevation = 2f
                    strokeColor = 0xFFE1AFD1.toInt()
                    strokeWidth = 1
                    setCardBackgroundColor(0xFFFFFFFF.toInt())
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16.dpToPx(), 12.dpToPx(), 16.dpToPx(), 12.dpToPx())
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val colDate = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(60.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
                    setBackgroundColor(0xFFF3E5F5.toInt())
                    setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                    (layoutParams as LinearLayout.LayoutParams).setMargins(0, 0, 12.dpToPx(), 0)
                }
                val parts = dateStr.split("/")
                val tvDay = TextView(this).apply {
                    text = parts.getOrElse(0) { "-" }
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF7469B6.toInt())
                    gravity = android.view.Gravity.CENTER
                }
                val monthNames = arrayOf("","Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")
                val monthIdx = parts.getOrElse(1) { "1" }.toIntOrNull() ?: 1
                val tvMonth = TextView(this).apply {
                    text = monthNames.getOrElse(monthIdx) { "-" }
                    textSize = 10f
                    setTextColor(0xFFAD88C6.toInt())
                    gravity = android.view.Gravity.CENTER
                }
                colDate.addView(tvDay)
                colDate.addView(tvMonth)

                val colInfo = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val tvCust = TextView(this).apply {
                    text = customerName
                    textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF7469B6.toInt())
                }
                val tvPkg = TextView(this).apply {
                    text = packageName
                    textSize = 11f
                    setTextColor(0xFFAD88C6.toInt())
                }
                val tvTime = TextView(this).apply {
                    text = "⏰ $timeStr"
                    textSize = 11f
                    setTextColor(0xFF7469B6.toInt())
                    setPadding(0, 4.dpToPx(), 0, 0)
                }
                colInfo.addView(tvCust)
                colInfo.addView(tvPkg)
                colInfo.addView(tvTime)

                val tvStatus = TextView(this).apply {
                    text = status
                    textSize = 9f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFFFFFFFF.toInt())
                    setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(statusColor)
                        cornerRadius = 100f
                    }
                }

                val colNote = TextView(this).apply {
                    text = "📝"
                    textSize = 20f
                    setPadding(12.dpToPx(), 0, 0, 0)
                    setOnClickListener { showNoteDialog(bookingId.toInt(), customerName) }
                }

                row.addView(colDate)
                row.addView(colInfo)
                row.addView(tvStatus)
                row.addView(colNote)
                card.addView(row)
                container.addView(card)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ─── CATATAN READER ───────────────────────────────────────────────────────

    private fun showNoteDialog(bookingId: Int, customerName: String) {
        var existingNote = ""
        var noteId = -1

        val cNote = db.rawQuery(
            "SELECT id, note FROM reader_notes WHERE reader_id = ? AND booking_id = ? LIMIT 1",
            arrayOf(readerId.toString(), bookingId.toString())
        )
        if (cNote.moveToFirst()) {
            noteId       = cNote.getInt(0)
            existingNote = cNote.getString(1) ?: ""
        }
        cNote.close()

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding / 2, padding, 0)

        val tvInfo = TextView(this)
        tvInfo.text = "Catatan untuk: $customerName"
        tvInfo.textSize = 12f
        tvInfo.setTextColor(0xFFAD88C6.toInt())
        tvInfo.setPadding(0, 0, 0, padding / 2)
        layout.addView(tvInfo)

        val etNote = EditText(this)
        etNote.setText(existingNote)
        etNote.hint = "Tulis catatan rahasia kamu tentang sesi ini..."
        etNote.minLines = 4
        etNote.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        etNote.gravity = android.view.Gravity.TOP
        layout.addView(etNote)

        AlertDialog.Builder(this)
            .setTitle("📝 Catatan Reader")
            .setView(layout)
            .setPositiveButton("Simpan") { _, _ ->
                val teks = etNote.text.toString().trim()
                if (teks.isEmpty()) {
                    Toast.makeText(this, "Catatan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                try {
                    if (noteId == -1) {
                        db.execSQL(
                            "INSERT INTO reader_notes (reader_id, booking_id, note, created_at) VALUES (?, ?, ?, datetime('now','localtime'))",
                            arrayOf(readerId.toString(), bookingId.toString(), teks)
                        )
                    } else {
                        db.execSQL(
                            "UPDATE reader_notes SET note = ? WHERE id = ?",
                            arrayOf(teks, noteId.toString())
                        )
                    }
                    Toast.makeText(this, "Catatan tersimpan ✨", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(if (noteId != -1) "Hapus" else null) { _, _ ->
                if (noteId != -1) {
                    db.execSQL("DELETE FROM reader_notes WHERE id = ?", arrayOf(noteId.toString()))
                    Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── BOOKING AKTIF ────────────────────────────────────────────────────────

    private fun startReading() {
        if (currentBookingId <= 0) {
            Toast.makeText(this, "Tidak ada booking untuk diproses", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            db.execSQL(
                "UPDATE bookings SET status = 'processing', reader_id = ?, reader_name = ? WHERE id = ?",
                arrayOf(readerId.toString(), readerName, currentBookingId.toString())
            )
            isProcessing = true
            b.btnStartReading.text = "Sesi Sedang Berjalan ▼"
            b.btnStartReading.setBackgroundColor(0xFF4CAF50.toInt())
            Toast.makeText(this, "Sesi ramalan dimulai!", Toast.LENGTH_SHORT).show()
            loadStats()
            loadEarnings()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memulai: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReadingOptions() {
        var customerQuestion = "Belum ada pertanyaan"
        var questionId = 0
        try {
            val cQ = db.rawQuery(
                "SELECT id, question FROM questions WHERE booking_id = ? LIMIT 1",
                arrayOf(currentBookingId.toString())
            )
            if (cQ.moveToFirst()) {
                questionId       = cQ.getInt(0)
                customerQuestion = cQ.getString(1) ?: "Belum ada pertanyaan"
            }
            cQ.close()
        } catch (e: Exception) { e.printStackTrace() }

        val finalQuestionId = questionId
        val finalQuestion   = customerQuestion

        AlertDialog.Builder(this)
            .setTitle("Sesi Ramalan Aktif")
            .setMessage("Pertanyaan Customer:\n\n\"$finalQuestion\"")
            .setPositiveButton("✍️ Jawab Pertanyaan") { _, _ -> showAnswerDialog(finalQuestionId, finalQuestion) }
            .setNeutralButton("✅ Selesaikan") { _, _ -> completeReading() }
            .setNegativeButton("❌ Batalkan Sesi") { _, _ -> cancelReading() }
            .show()
    }

    private fun showAnswerDialog(questionId: Int, question: String) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (20 * resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding / 2, padding, 0)

        val tvQuestion = TextView(this)
        tvQuestion.text = "Q: $question"
        tvQuestion.setTextColor(0xFF7469B6.toInt())
        tvQuestion.textSize = 13f
        tvQuestion.setPadding(0, 0, 0, padding / 2)
        layout.addView(tvQuestion)

        val etAnswer = EditText(this)
        etAnswer.hint = "Tulis jawaban/ramalan di sini..."
        etAnswer.minLines = 4
        etAnswer.inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        etAnswer.gravity = android.view.Gravity.TOP
        layout.addView(etAnswer)

        AlertDialog.Builder(this)
            .setTitle("💬 Jawab Pertanyaan")
            .setView(layout)
            .setPositiveButton("Kirim Jawaban") { _, _ ->
                val jawaban = etAnswer.text.toString().trim()
                if (jawaban.isEmpty()) {
                    Toast.makeText(this, "Jawaban tidak boleh kosong", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveAnswer(questionId, jawaban)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun saveAnswer(questionId: Int, answer: String) {
        try {
            if (questionId > 0) {
                db.execSQL("UPDATE questions SET answer = ? WHERE id = ?", arrayOf(answer, questionId.toString()))
            } else {
                db.execSQL("INSERT INTO questions (booking_id, answer) VALUES (?, ?)", arrayOf(currentBookingId.toString(), answer))
            }
            Toast.makeText(this, "Jawaban berhasil dikirim! ✨", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal kirim jawaban: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun completeReading() {
        if (currentBookingId <= 0) return
        AlertDialog.Builder(this)
            .setTitle("Selesaikan Ramalan")
            .setMessage("Yakin ingin menyelesaikan sesi ramalan ini?")
            .setPositiveButton("Ya, Selesai") { _, _ ->
                try {
                    db.execSQL("UPDATE bookings SET status = 'completed' WHERE id = ?", arrayOf(currentBookingId.toString()))
                    isProcessing = false
                    currentBookingId = 0
                    b.btnStartReading.text = "Mulai Ramalan"
                    b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                    Toast.makeText(this, "Sesi ramalan selesai!", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadEarnings()
                    loadNextBooking()
                    loadCalendarBookings()
                    loadReadingHistory()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menyelesaikan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun cancelReading() {
        if (currentBookingId <= 0) return
        AlertDialog.Builder(this)
            .setTitle("Batalkan Sesi")
            .setMessage("Yakin ingin membatalkan sesi ini?")
            .setPositiveButton("Ya, Batalkan") { _, _ ->
                try {
                    db.execSQL(
                        "UPDATE bookings SET status = 'paid', reader_id = 0, reader_name = '' WHERE id = ?",
                        arrayOf(currentBookingId.toString())
                    )
                    isProcessing = false
                    currentBookingId = 0
                    b.btnStartReading.text = "Mulai Ramalan"
                    b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                    Toast.makeText(this, "Sesi dibatalkan.", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadNextBooking()
                    loadCalendarBookings()
                    loadReadingHistory()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membatalkan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun loadReaderProfile() {
        try {
            val c = db.rawQuery("SELECT name FROM users WHERE email = ?", arrayOf(userEmail))
            if (c.moveToFirst()) b.tvReaderGreeting.text = "Selamat Datang, ${c.getString(0)}"
            c.close()
        } catch (e: Exception) {
            b.tvReaderGreeting.text = "Selamat Datang, Reader"
        }
    }

    private fun loadNextBooking() {
        try {
            val sql = """
                SELECT b.id, u.name, b.booking_date, b.booking_time, b.package_name, b.email
                FROM bookings b
                LEFT JOIN users u ON b.email = u.email
                WHERE b.status IN ('paid','PAID')
                AND (b.reader_id = ? OR b.reader_id = 0)
                ORDER BY b.booking_date ASC, b.booking_time ASC
                LIMIT 1
            """.trimIndent()

            val c = db.rawQuery(sql, arrayOf(readerId.toString()))
            if (c.moveToFirst()) {
                currentBookingId = c.getInt(0)
                val customerName = c.getString(1)?.takeIf { it.isNotEmpty() } ?: c.getString(5) ?: "-"
                b.tvNextCustomerName.text = customerName
                b.tvNextBookingDate.text  = "📅 ${c.getString(2)?.takeIf { it.isNotEmpty() } ?: "-"}"
                b.tvNextBookingTime.text  = c.getString(3)?.takeIf { it.isNotEmpty() } ?: "--:--"
                b.tvNextPackageName.text  = "Paket: ${c.getString(4) ?: "-"}"
                b.btnStartReading.visibility = View.VISIBLE
                b.btnStartReading.text = "Mulai Ramalan"
                b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                isProcessing = false
            } else {
                currentBookingId = 0
                isProcessing = false
                b.tvNextCustomerName.text = "Belum Ada Antrean"
                b.tvNextBookingDate.text  = ""
                b.tvNextBookingTime.text  = "--:--"
                b.tvNextPackageName.text  = "Siap melayani sesi baru"
                b.btnStartReading.visibility = View.GONE
            }
            c.close()
        } catch (e: Exception) {
            e.printStackTrace()
            b.tvNextCustomerName.text = "Belum Ada Antrean"
            b.btnStartReading.visibility = View.GONE
        }
    }

    private fun loadReadingHistory() {
        try {
            val sql = """
                SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.status, q.question, q.answer
                FROM bookings b
                LEFT JOIN users u ON b.email = u.email
                LEFT JOIN questions q ON b.id = q.booking_id
                WHERE b.reader_id = ?
                AND b.status IN ('completed','COMPLETED','done','DONE')
                ORDER BY b.id DESC
                LIMIT 20
            """.trimIndent()

            val c = db.rawQuery(sql, arrayOf(readerId.toString()))
            historyList.clear()

            while (c.moveToNext()) {
                val customerName = c.getString(1)?.takeIf { it.isNotEmpty() } ?: c.getString(2) ?: "-"
                historyList.add(
                    ReaderHistoryItem(
                        id           = c.getInt(0),
                        customerName = customerName,
                        packageName  = c.getString(3) ?: "-",
                        bookingDate  = c.getString(4) ?: "-",
                        status       = (c.getString(5) ?: "").uppercase(),
                        question     = "Q: " + (c.getString(6) ?: "Tidak ada pertanyaan"),
                        answer       = "A: " + (c.getString(7) ?: "Belum dijawab")
                    )
                )
            }
            c.close()

            val container = b.containerReaderHistory
            container.removeAllViews()

            if (historyList.isEmpty()) {
                val tvEmpty = TextView(this).apply {
                    text = "Belum ada sesi yang selesai"
                    setTextColor(0xFFAD88C6.toInt())
                    textSize = 13f
                    setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                container.addView(tvEmpty)
                return
            }

            for (item in historyList) {
                val inflater = LayoutInflater.from(this)
                val binding  = ItemHistoryBinding.inflate(inflater, container, false)
                binding.tvItemPackage.text  = item.packageName
                binding.tvItemDate.text     = item.bookingDate
                binding.tvItemStatus.text   = item.status
                binding.tvItemQuestion.text = item.question
                binding.tvItemAnswer.text   = item.answer
                binding.tvItemReader.text   = "Customer: ${item.customerName}"
                binding.btnCancelOrder.visibility  = View.GONE
                binding.btnTulisUlasan.visibility  = View.GONE
                binding.btnShareRamalan.visibility = View.GONE
                container.addView(binding.root)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToFloat(): Float = this * resources.displayMetrics.density

    override fun onResume() {
        super.onResume()
        loadStats()
        loadEarnings()
        loadNextBooking()
        loadCalendarBookings()
        loadReadingHistory()
    }
}