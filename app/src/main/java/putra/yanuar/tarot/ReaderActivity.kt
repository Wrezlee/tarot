package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase
    private var userEmail: String = ""
    private var readerId: Int = 0
    private var readerName: String = ""
    private var currentBookingId: Int = 0
    private var isProcessing: Boolean = false

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
            } else {
                Toast.makeText(this, "Data reader tidak ditemukan: $userEmail", Toast.LENGTH_LONG).show()
            }
            cursorReader.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error baca data reader: ${e.message}", Toast.LENGTH_LONG).show()
        }

        try {
            setSupportActionBar(b.toolbarReader)
            supportActionBar?.setDisplayShowTitleEnabled(false)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        b.btnStartReading.setOnClickListener(this)
        b.btnViewHistory.setOnClickListener(this)

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
        loadNextBooking()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reader_menu_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                Toast.makeText(this, "Tarot Meow v1.0 Reader Dashboard", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_logout -> {
                // Logout TIDAK mengubah status online/offline, biarkan sesuai switch
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
                if (isProcessing) {
                    // Tampilkan pilihan: selesaikan atau batalkan
                    showReadingOptions()
                } else {
                    startReading()
                }
            }
            R.id.btnViewHistory -> {
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra("USER_EMAIL", userEmail)
                startActivity(intent)
            }
        }
    }

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

    /**
     * Tampilkan dialog pilihan saat sesi sedang berjalan:
     * 1. Jawab Pertanyaan Customer
     * 2. Selesaikan Ramalan
     * 3. Batalkan Ramalan
     */
    private fun showReadingOptions() {
        // Ambil pertanyaan customer terlebih dahulu
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
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val finalQuestionId = questionId
        val finalQuestion = customerQuestion

        AlertDialog.Builder(this)
            .setTitle("Sesi Ramalan Aktif")
            .setMessage("Pertanyaan Customer:\n\n\"$finalQuestion\"")
            .setPositiveButton("✍️ Jawab Pertanyaan") { _, _ ->
                showAnswerDialog(finalQuestionId, finalQuestion)
            }
            .setNeutralButton("✅ Selesaikan") { _, _ ->
                completeReading()
            }
            .setNegativeButton("❌ Batalkan Sesi") { _, _ ->
                cancelReading()
            }
            .show()
    }

    /**
     * Dialog untuk reader mengetik jawaban atas pertanyaan customer
     */
    private fun showAnswerDialog(questionId: Int, question: String) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (20 * resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding / 2, padding, 0)

        val tvQuestion = android.widget.TextView(this)
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
                db.execSQL(
                    "UPDATE questions SET answer = ? WHERE id = ?",
                    arrayOf(answer, questionId.toString())
                )
            } else {
                // Jika belum ada row questions untuk booking ini, insert baru
                db.execSQL(
                    "INSERT INTO questions (booking_id, answer) VALUES (?, ?)",
                    arrayOf(currentBookingId.toString(), answer)
                )
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
                    db.execSQL(
                        "UPDATE bookings SET status = 'completed' WHERE id = ?",
                        arrayOf(currentBookingId.toString())
                    )
                    isProcessing = false
                    currentBookingId = 0
                    b.btnStartReading.text = "Mulai Ramalan"
                    b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                    Toast.makeText(this, "Sesi ramalan selesai!", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadNextBooking()
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
            .setMessage("Yakin ingin membatalkan sesi ini? Status booking akan kembali ke 'paid' dan masuk antrean lagi.")
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
                    Toast.makeText(this, "Sesi dibatalkan, booking kembali ke antrean.", Toast.LENGTH_SHORT).show()
                    loadStats()
                    loadNextBooking()
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
            if (c.moveToFirst()) {
                b.tvReaderGreeting.text = "Selamat Datang, ${c.getString(0)}"
            }
            c.close()
        } catch (e: Exception) {
            b.tvReaderGreeting.text = "Selamat Datang, Reader"
        }
    }

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
            b.tvPendingCount.text = "0"
            b.tvCompletedCount.text = "0"
        }
    }

    private fun loadNextBooking() {
        try {
            val sql = """
                SELECT id, email, booking_time, package_name 
                FROM bookings 
                WHERE status IN ('paid','PAID') 
                AND (reader_id = ? OR reader_id = 0)
                ORDER BY booking_date ASC, booking_time ASC 
                LIMIT 1
            """.trimIndent()

            val c = db.rawQuery(sql, arrayOf(readerId.toString()))
            if (c.moveToFirst()) {
                currentBookingId = c.getInt(0)
                b.tvNextCustomerName.text = c.getString(1) ?: "-"
                b.tvNextBookingTime.text  = c.getString(2)?.takeIf { it.isNotEmpty() } ?: "--:--"
                b.tvNextPackageName.text  = "Paket: ${c.getString(3) ?: "-"}"
                b.btnStartReading.visibility = View.VISIBLE
                b.btnStartReading.text = "Mulai Ramalan"
                b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                isProcessing = false
            } else {
                currentBookingId = 0
                isProcessing = false
                b.tvNextCustomerName.text = "Belum Ada Antrean"
                b.tvNextBookingTime.text  = "--:--"
                b.tvNextPackageName.text  = "Siap melayani sesi baru"
                b.btnStartReading.visibility = View.GONE
            }
            c.close()
        } catch (e: Exception) {
            e.printStackTrace()
            b.tvNextCustomerName.text = "Belum Ada Antrean"
            b.tvNextBookingTime.text  = "--:--"
            b.tvNextPackageName.text  = "Siap melayani sesi baru"
            b.btnStartReading.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        loadNextBooking()
    }

    // onDestroy TIDAK set offline — status hanya berubah lewat toggle switch
}