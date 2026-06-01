package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import putra.yanuar.tarot.databinding.ActivityReaderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReaderActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {

    private lateinit var b: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase
    private lateinit var ft: FragmentTransaction

    private lateinit var fragHistory: ReaderHistoryFragment
    private lateinit var fragProfile: ReaderProfileFragment

    private var userEmail: String = ""
    private var readerId: Int = 0
    private var readerName: String = ""
    private var currentBookingId: Int = 0
    private var isProcessing: Boolean = false

    // ── Public accessors dipakai Fragment ──────────────────────────────────
    fun getDbObject(): SQLiteDatabase = db
    fun getReaderId(): Int = readerId
    fun getUserEmail(): String = userEmail

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

        // Init fragments
        fragHistory = ReaderHistoryFragment()
        fragProfile = ReaderProfileFragment()

        // Load reader info
        try {
            val c = db.rawQuery(
                "SELECT id, name, is_online FROM users WHERE email = ?",
                arrayOf(userEmail)
            )
            if (c.moveToFirst()) {
                readerId  = c.getInt(0)
                readerName = c.getString(1) ?: "Reader"
                val isOnline = c.getInt(2) == 1
                b.switchStatus.isChecked = isOnline
                b.tvStatusLabel.text = if (isOnline) "Online & Active" else "Offline / Sibuk"
            }
            c.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error baca data reader: ${e.message}", Toast.LENGTH_LONG).show()
        }

        setSupportActionBar(b.toolbarReader)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Tarot Meow"

        b.navbarReader.setOnItemSelectedListener(this)
        b.btnStartReading.setOnClickListener {
            if (isProcessing) showReadingOptions() else startReading()
        }

        b.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            try {
                val statusVal = if (isChecked) 1 else 0
                val statusText = if (isChecked) "Online & Active" else "Offline / Sibuk"
                b.tvStatusLabel.text = statusText
                db.execSQL("UPDATE users SET is_online = ? WHERE id = ?",
                    arrayOf(statusVal.toString(), readerId.toString()))
                Toast.makeText(this, "Status Reader: ${if (isChecked) "Online" else "Offline"}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal update status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Update greeting
        b.tvReaderGreeting.text = "Halo, $readerName"

        loadStats()
        loadNextBooking()
        loadCalendarBookings()
    }

    // ── Toolbar ────────────────────────────────────────────────────────────
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
                        "Fitur Reader:\n" +
                        "• Kelola status online/offline\n" +
                        "• Lihat antrean booking pelanggan\n" +
                        "• Mulai & selesaikan sesi ramalan\n" +
                        "• Jawab pertanyaan pelanggan\n" +
                        "• Statistik pendapatan harian & mingguan\n" +
                        "• Kalender booking minggu ini\n" +
                        "• Catatan pribadi per sesi\n\n" +
                        "Dikembangkan oleh Kelompok 1 PSI\n" +
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

    // ── Bottom Nav ─────────────────────────────────────────────────────────
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        ft = supportFragmentManager.beginTransaction()
        when (item.itemId) {
            R.id.readerHome -> {
                b.scrollReaderHome.visibility = View.VISIBLE
                b.containerReader.visibility  = View.GONE
            }
            R.id.readerHistory -> {
                ft.replace(R.id.containerReader, fragHistory)
                ft.commit()
                b.scrollReaderHome.visibility = View.GONE
                b.containerReader.visibility  = View.VISIBLE
            }
            R.id.readerProfile -> {
                ft.replace(R.id.containerReader, fragProfile)
                ft.commit()
                b.scrollReaderHome.visibility = View.GONE
                b.containerReader.visibility  = View.VISIBLE
            }
        }
        return true
    }

    // ── Stats (home) ───────────────────────────────────────────────────────
    fun loadStats() {
        try {
            val cPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID') AND (reader_id = ? OR reader_id = 0)",
                arrayOf(readerId.toString())
            )
            if (cPending.moveToFirst()) b.tvPendingCount.text = cPending.getInt(0).toString()
            cPending.close()

            val cDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('completed','COMPLETED','done','DONE') AND reader_id = ?",
                arrayOf(readerId.toString())
            )
            if (cDone.moveToFirst()) b.tvCompletedCount.text = cDone.getInt(0).toString()
            cDone.close()
        } catch (e: Exception) {
            b.tvPendingCount.text   = "0"
            b.tvCompletedCount.text = "0"
        }
    }

    // ── Next Booking ───────────────────────────────────────────────────────
    fun loadNextBooking() {
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

    // ── Calendar ───────────────────────────────────────────────────────────
    fun loadCalendarBookings() {
        try {
            val container = b.containerCalendar
            container.removeAllViews()

            val cal      = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val calStart = Calendar.getInstance()
            calStart.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY))
            val calEnd = Calendar.getInstance()
            calEnd.add(Calendar.DAY_OF_MONTH, (Calendar.SUNDAY - dayOfWeek + 7) % 7)

            val sdf       = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
            val startStr  = sdf.format(calStart.time)
            val endStr    = sdf.format(calEnd.time)

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
                } catch (ex: Exception) { ex.printStackTrace() }
            }
            cursor.close()

            if (bookingsThisWeek.isEmpty()) {
                val tv = TextView(this).apply {
                    text = "Tidak ada booking minggu ini"
                    setTextColor(0xFFAD88C6.toInt())
                    textSize = 13f
                    setPadding(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                container.addView(tv)
                return
            }

            val monthNames = arrayOf("","Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")

            for (booking in bookingsThisWeek) {
                val bookingId    = booking[0]
                val customerName = booking[1]
                val packageName  = booking[2]
                val dateStr      = booking[3]
                val timeStr      = booking[4]
                val status       = booking[5]
                val statusColor  = when (status) {
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

                val parts    = dateStr.split("/")
                val colDate  = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(60.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT)
                    setBackgroundColor(0xFFF3E5F5.toInt())
                    setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                    (layoutParams as LinearLayout.LayoutParams).marginEnd = 12.dpToPx()
                }

                colDate.addView(TextView(this).apply {
                    text = parts.getOrElse(0) { "-" }
                    textSize = 20f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF7469B6.toInt())
                    gravity = android.view.Gravity.CENTER
                })
                val monthIdx = parts.getOrElse(1) { "1" }.toIntOrNull() ?: 1
                colDate.addView(TextView(this).apply {
                    text = monthNames.getOrElse(monthIdx) { "-" }
                    textSize = 10f
                    setTextColor(0xFFAD88C6.toInt())
                    gravity = android.view.Gravity.CENTER
                })

                val colInfo = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                colInfo.addView(TextView(this).apply {
                    text = customerName; textSize = 14f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFF7469B6.toInt())
                })
                colInfo.addView(TextView(this).apply {
                    text = packageName; textSize = 11f; setTextColor(0xFFAD88C6.toInt())
                })
                colInfo.addView(TextView(this).apply {
                    text = "⏰ $timeStr"; textSize = 11f; setTextColor(0xFF7469B6.toInt())
                    setPadding(0, 4.dpToPx(), 0, 0)
                })

                val tvStatus = TextView(this).apply {
                    text = status; textSize = 9f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(0xFFFFFFFF.toInt())
                    setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(statusColor); cornerRadius = 100f
                    }
                }

                val colNote = TextView(this).apply {
                    text = "📝"; textSize = 20f
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
        } catch (e: Exception) { e.printStackTrace() }
    }

    // ── Note Dialog ────────────────────────────────────────────────────────
    private fun showNoteDialog(bookingId: Int, customerName: String) {
        var existingNote = ""
        var noteId = -1
        val cNote = db.rawQuery(
            "SELECT id, note FROM reader_notes WHERE reader_id = ? AND booking_id = ? LIMIT 1",
            arrayOf(readerId.toString(), bookingId.toString())
        )
        if (cNote.moveToFirst()) {
            noteId = cNote.getInt(0)
            existingNote = cNote.getString(1) ?: ""
        }
        cNote.close()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dpToPx(), 8.dpToPx(), 16.dpToPx(), 0)
        }
        val tvInfo = TextView(this).apply {
            text = "Catatan untuk: $customerName"
            textSize = 12f; setTextColor(0xFFAD88C6.toInt())
            setPadding(0, 0, 0, 8.dpToPx())
        }
        val etNote = EditText(this).apply {
            setText(existingNote)
            hint = "Tulis catatan rahasia kamu tentang sesi ini..."
            minLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = android.view.Gravity.TOP
        }
        layout.addView(tvInfo)
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
                        db.execSQL("UPDATE reader_notes SET note = ? WHERE id = ?",
                            arrayOf(teks, noteId.toString()))
                    }
                    Toast.makeText(this, "Catatan tersimpan", Toast.LENGTH_SHORT).show()
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

    // ── Reading Session ────────────────────────────────────────────────────
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
                questionId = cQ.getInt(0)
                customerQuestion = cQ.getString(1) ?: "Belum ada pertanyaan"
            }
            cQ.close()
        } catch (e: Exception) { e.printStackTrace() }

        val finalQuestionId = questionId
        val finalQuestion   = customerQuestion

        AlertDialog.Builder(this)
            .setTitle("Sesi Ramalan Aktif")
            .setMessage("Pertanyaan Customer:\n\n\"$finalQuestion\"")
            .setPositiveButton("✍️ Jawab Pertanyaan") { _, _ ->
                showAnswerDialog(finalQuestionId, finalQuestion)
            }
            .setNeutralButton("✅ Selesaikan") { _, _ -> completeReading() }
            .setNegativeButton("❌ Batalkan Sesi") { _, _ -> cancelReading() }
            .show()
    }

    private fun showAnswerDialog(questionId: Int, question: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dpToPx(), 10.dpToPx(), 20.dpToPx(), 0)
        }
        val tvQ = TextView(this).apply {
            text = "Q: $question"; setTextColor(0xFF7469B6.toInt()); textSize = 13f
            setPadding(0, 0, 0, 10.dpToPx())
        }
        val etAnswer = EditText(this).apply {
            hint = "Tulis jawaban/ramalan di sini..."
            minLines = 4
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            gravity = android.view.Gravity.TOP
        }
        layout.addView(tvQ)
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
                try {
                    if (questionId > 0) {
                        db.execSQL("UPDATE questions SET answer = ? WHERE id = ?",
                            arrayOf(jawaban, questionId.toString()))
                    } else {
                        db.execSQL("INSERT INTO questions (booking_id, answer) VALUES (?, ?)",
                            arrayOf(currentBookingId.toString(), jawaban))
                    }
                    Toast.makeText(this, "Jawaban berhasil dikirim!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal kirim jawaban: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun completeReading() {
        if (currentBookingId <= 0) return
        AlertDialog.Builder(this)
            .setTitle("Selesaikan Ramalan")
            .setMessage("Yakin ingin menyelesaikan sesi ramalan ini?")
            .setPositiveButton("Ya, Selesai") { _, _ ->
                try {
                    db.execSQL("UPDATE bookings SET status = 'completed' WHERE id = ?",
                        arrayOf(currentBookingId.toString()))
                    isProcessing = false
                    currentBookingId = 0
                    b.btnStartReading.text = "Mulai Ramalan"
                    b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                    Toast.makeText(this, "Sesi ramalan selesai!", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadNextBooking()
                    loadCalendarBookings()
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
                    db.execSQL("UPDATE bookings SET status = 'paid', reader_id = 0, reader_name = '' WHERE id = ?",
                        arrayOf(currentBookingId.toString()))
                    isProcessing = false
                    currentBookingId = 0
                    b.btnStartReading.text = "Mulai Ramalan"
                    b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                    Toast.makeText(this, "Sesi dibatalkan.", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadNextBooking()
                    loadCalendarBookings()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membatalkan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    // ── Extension helpers ──────────────────────────────────────────────────
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToFloat(): Float = this * resources.displayMetrics.density

    override fun onResume() {
        super.onResume()
        loadStats()
        loadNextBooking()
        loadCalendarBookings()
    }
}