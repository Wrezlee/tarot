package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var b: ActivityAdminBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase

        // Inisialisasi Listeners Grid Menu
        b.btnMenuUser.setOnClickListener(this)
        b.btnMenuPackages.setOnClickListener(this)
        b.btnMenuTransactions.setOnClickListener(this)
        b.btnMenuQuestions.setOnClickListener(this)
        b.btnMenuTestimony.setOnClickListener(this)
        b.btnMenuReport.setOnClickListener(this)
        b.btnLogoutAdmin.setOnClickListener(this)

        loadStats()
    }

    // --- OPTIONS MENU (Checklist No. 10) ---
    // Muncul di pojok kanan atas (Action Bar)
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(Menu.NONE, 1, Menu.NONE, "Tentang Aplikasi")
        menu?.add(Menu.NONE, 2, Menu.NONE, "Logout")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> Toast.makeText(this, "Tarot Meow v1.0 by Putra Yanuar", Toast.LENGTH_SHORT).show()
            2 -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    // --- NAVIGATION LOGIC ---
    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnMenuUser -> startActivity(Intent(this, ManageUserActivity::class.java))
            R.id.btnMenuPackages -> startActivity(Intent(this, ManageTarotActivity::class.java))
            R.id.btnMenuTransactions -> Toast.makeText(this, "Data Transaksi", Toast.LENGTH_SHORT).show()
            R.id.btnMenuQuestions -> Toast.makeText(this, "Daftar Pertanyaan", Toast.LENGTH_SHORT).show()
            R.id.btnMenuTestimony -> Toast.makeText(this, "Moderasi Testimoni", Toast.LENGTH_SHORT).show()
            R.id.btnMenuReport -> Toast.makeText(this, "Laporan Keuangan", Toast.LENGTH_SHORT).show()
            R.id.btnLogoutAdmin -> logout()
        }
    }

    fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        // Menghapus history agar tidak bisa klik 'Back' kembali ke Admin
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // --- DATA LOADING ---
    fun loadStats() {
        try {
            // 1. Hitung Pendapatan
            val cursorRev = db.rawQuery("SELECT SUM(total_price) FROM bookings WHERE status IN ('paid', 'PAID')", null)
            if (cursorRev.moveToFirst()) {
                val revenue = cursorRev.getInt(0)
                b.tvTotalRevenue.text = "Rp$revenue"
            }
            cursorRev.close()

            // 2. Hitung Customer
            val cursorUser = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'customer'", null)
            if (cursorUser.moveToFirst()) {
                b.tvActiveSeekers.text = cursorUser.getInt(0).toString()
            }
            cursorUser.close()

            // 3. Hitung Readers (Petugas)
            val cursorReader = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'reader'", null)
            if (cursorReader.moveToFirst()){
                b.tvVerifiedReaders.text = cursorReader.getInt(0).toString()
            }
            cursorReader.close()

        } catch (e: Exception) {
            b.tvTotalRevenue.text = "Rp0"
            b.tvActiveSeekers.text = "0"
            b.tvVerifiedReaders.text = "0"
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }
}