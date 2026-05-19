package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var b: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase
    private lateinit var userEmail: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(b.root)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        db = DBOpenHelper(this).writableDatabase

        setSupportActionBar(b.toolbarReader)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        b.btnStartReading.setOnClickListener(this)
        b.btnViewHistory.setOnClickListener(this)
        
        b.switchStatus.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                b.tvStatusLabel.text = "Online & Active"
                Toast.makeText(this, "Status Reader: Online", Toast.LENGTH_SHORT).show()
            } else {
                b.tvStatusLabel.text = "Offline / Sibuk"
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
                Toast.makeText(this, "Membuka sesi ramalan...", Toast.LENGTH_SHORT).show()
            }
            R.id.btnViewHistory -> {
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra("USER_EMAIL", userEmail)
                startActivity(intent)
            }
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
            val cursorPending = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE status IN ('paid', 'PAID')", null)
            if (cursorPending.moveToFirst()) {
                b.tvPendingCount.text = cursorPending.getInt(0).toString()
            }
            cursorPending.close()

            val cursorDone = db.rawQuery("SELECT COUNT(*) FROM bookings WHERE status IN ('completed', 'COMPLETED', 'done', 'DONE')", null)
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
            val sql = "SELECT name, booking_time, package_name FROM bookings WHERE status IN ('paid', 'PAID') ORDER BY booking_date ASC, booking_time ASC LIMIT 1"
            val c = db.rawQuery(sql, null)
            if (c.moveToFirst()) {
                b.tvNextCustomerName.text = c.getString(0)
                b.tvNextBookingTime.text = c.getString(1)
                b.tvNextPackageName.text = "Paket: ${c.getString(2)}"
                b.btnStartReading.visibility = View.VISIBLE
            } else {
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
}