package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
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

        setSupportActionBar(b.toolbarAdmin)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Tarot Meow"

        b.btnMenuUser.setOnClickListener(this)
        b.btnMenuPackages.setOnClickListener(this)
        b.btnMenuTestimony.setOnClickListener(this)
        b.btnMenuReport.setOnClickListener(this)
        b.btnMenuBooking.setOnClickListener(this)

        loadStats()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_about  -> showAboutDialog()
            R.id.menu_logout -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔮 Tarot Meow")
            .setMessage(
                "Versi: 1.0\n\n" +
                        "Tarot Meow adalah aplikasi layanan pembacaan tarot profesional " +
                        "yang menghubungkan pelanggan dengan reader berpengalaman.\n\n" +
                        "Fitur Admin:\n" +
                        "• Kelola data pengguna (Admin, Reader, Customer)\n" +
                        "• Kelola paket layanan tarot\n" +
                        "• Moderasi testimoni pelanggan\n" +
                        "• Laporan keuangan & statistik\n" +
                        "• Kelola & ubah status booking\n\n" +
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

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnMenuUser      -> startActivity(Intent(this, AdminManageUserActivity::class.java))
            R.id.btnMenuPackages  -> startActivity(Intent(this, AdminManageTarotActivity::class.java))
            R.id.btnMenuTestimony -> startActivity(Intent(this, AdminTestimoniActivity::class.java))
            R.id.btnMenuReport    -> startActivity(Intent(this, AdminReportActivity::class.java))
            R.id.btnMenuBooking   -> startActivity(Intent(this, AdminManagePesananActivity::class.java))
        }
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun loadStats() {
        try {
            val cursorRev = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null)
            if (cursorRev.moveToFirst()) b.tvTotalRevenue.text = "Rp ${cursorRev.getInt(0)}"
            cursorRev.close()

            val cursorUser = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'customer'", null)
            if (cursorUser.moveToFirst()) b.tvActiveSeekers.text = cursorUser.getInt(0).toString()
            cursorUser.close()

            val cursorReader = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'reader'", null)
            if (cursorReader.moveToFirst()) b.tvVerifiedReaders.text = cursorReader.getInt(0).toString()
            cursorReader.close()
        } catch (e: Exception) {
            b.tvTotalRevenue.text    = "Rp 0"
            b.tvActiveSeekers.text   = "0"
            b.tvVerifiedReaders.text = "0"
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }
}