package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase
    private lateinit var userEmail: String
    private var readerId: Int = 0
    private var readerName: String = ""
    private var currentBookingId: Int = 0
    private var isProcessing: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(b.root)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        db = DBOpenHelper(this).writableDatabase

        // Ambil data reader
        val cursorReader = db.rawQuery("SELECT id, name, is_online FROM users WHERE email = ?", arrayOf(userEmail))
        if (cursorReader.moveToFirst()) {
            readerId = cursorReader.getInt(0)
            readerName = cursorReader.getString(1) ?: ""
            val isOnline = cursorReader.getInt(2) == 1
            b.switchStatus.isChecked = isOnline
            b.tvStatusLabel.text = if (isOnline) "Online & Active" else "Offline / Sibuk"
        }
        cursorReader.close()

        setSupportActionBar(b.toolbarReader)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        b.btnStartReading.setOnClickListener(this)
        b.btnViewHistory.setOnClickListener(this)

        b.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                b.tvStatusLabel.text = "Online & Active"
                db.execSQL("UPDATE users SET is_online = 1 WHERE id = ?", arrayOf(readerId.toString()))
                Toast.makeText(this, "Status Reader: Online", Toast.LENGTH_SHORT).show()
            } else {
                b.tvStatusLabel.text = "Offline / Sibuk"
                db.execSQL("UPDATE users SET is_online = 0 WHERE id = ?", arrayOf(readerId.toString()))
                Toast.makeText(this, "Status Reader: Offline", Toast.LENGTH_SHORT).show()
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
                // Set offline saat logout
                db.execSQL("UPDATE users SET is_online = 0 WHERE id = ?", arrayOf(readerId.toString()))
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
                    // Selesaikan sesi
                    completeReading()
                } else {
                    // Mulai sesi
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
        if (currentBookingId > 0) {
            try {
                // Update status dan assign reader jika belum di-assign
                db.execSQL(
                    "UPDATE bookings SET status = 'processing', reader_id = ?, reader_name = ? WHERE id = ?",
                    arrayOf(readerId.toString(), readerName, currentBookingId.toString())
                )
                isProcessing = true
                b.btnStartReading.text = "Selesaikan Ramalan"
                b.btnStartReading.setBackgroundColor(0xFF4CAF50.toInt())
                Toast.makeText(this, "Sesi ramalan dimulai!", Toast.LENGTH_SHORT).show()
                loadStats()
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal memulai: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun completeReading() {
        if (currentBookingId > 0) {
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
                        Toast.makeText(this, "Sesi ramalan selesai! Status diperbarui.", Toast.LENGTH_SHORT).show()
                        loadStats()
                        loadNextBooking()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Gagal menyelesaikan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    private fun loadReaderProfile() {
        val sql = "SELECT name FROM users WHERE email = ?"
        val c = db.rawQuery(sql, arrayOf(userEmail))
        if (c.moveToFirst()) {
            b.tvReaderGreeting.text = "Selamat Datang, ${c.getString(0)}"
        }
        c.close()
    }

    private fun loadStats() {
        try {
            // Pending queue untuk reader ini
            val cursorPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid', 'PAID') AND (reader_id = ? OR reader_id = 0)",
                arrayOf(readerId.toString())
            )
            if (cursorPending.moveToFirst()) {
                b.tvPendingCount.text = cursorPending.getInt(0).toString()
            }
            cursorPending.close()

            // Completed untuk reader ini
            val cursorDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('completed', 'COMPLETED', 'done', 'DONE') AND reader_id = ?",
                arrayOf(readerId.toString())
            )
            if (cursorDone.moveToFirst()) {
                b.tvCompletedCount.text = cursorDone.getInt(0).toString()
            }
            cursorDone.close()
        } catch (e: Exception) {
            b.tvPendingCount.text = "0"
            b.tvCompletedCount.text = "0"
        }
    }

    private fun loadNextBooking() {
        try {
            // Cari booking untuk reader ini, urutkan berdasarkan tanggal
            val sql = """
                SELECT id, name, booking_time, package_name 
                FROM bookings 
                WHERE status IN ('paid', 'PAID') 
                AND (reader_id = ? OR reader_id = 0)
                ORDER BY booking_date ASC, booking_time ASC 
                LIMIT 1
            """.trimIndent()
            val c = db.rawQuery(sql, arrayOf(readerId.toString()))
            if (c.moveToFirst()) {
                currentBookingId = c.getInt(0)
                b.tvNextCustomerName.text = c.getString(1)
                b.tvNextBookingTime.text = c.getString(2)
                b.tvNextPackageName.text = "Paket: ${c.getString(3)}"
                b.btnStartReading.visibility = View.VISIBLE
                b.btnStartReading.text = "Mulai Ramalan"
                b.btnStartReading.setBackgroundColor(0xFF7469B6.toInt())
                isProcessing = false
            } else {
                currentBookingId = 0
                isProcessing = false
                b.tvNextCustomerName.text = "Belum Ada Antrean"
                b.tvNextBookingTime.text = "--:--"
                b.tvNextPackageName.text = "Siap melayani sesi baru"
                b.btnStartReading.visibility = View.GONE
            }
            c.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        loadNextBooking()
    }

    override fun onDestroy() {
        // Set offline saat activity dihancurkan
        try {
            db.execSQL("UPDATE users SET is_online = 0 WHERE id = ?", arrayOf(readerId.toString()))
        } catch (e: Exception) { e.printStackTrace() }
        super.onDestroy()
    }
}