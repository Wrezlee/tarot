package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAdminBinding
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        b.btnExportLaporan.setOnClickListener { exportLaporan() }

        loadStats()
        drawBarChart()
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
                        "• Kelola & ubah status booking\n" +
                        "• Grafik paket terlaris\n" +
                        "• Export laporan ke file .txt\n\n" +
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
            R.id.btnMenuUser      -> startActivity(Intent(this, ManageUserActivity::class.java))
            R.id.btnMenuPackages  -> startActivity(Intent(this, ManageTarotActivity::class.java))
            R.id.btnMenuTestimony -> startActivity(Intent(this, TestimoniActivity::class.java))
            R.id.btnMenuReport    -> startActivity(Intent(this, ReportActivity::class.java))
            R.id.btnMenuBooking   -> startActivity(Intent(this, ManageBookingActivity::class.java))
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
            if (cursorRev.moveToFirst()) b.tvTotalRevenue.text = "Rp${cursorRev.getInt(0)}"
            cursorRev.close()

            val cursorUser = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'customer'", null)
            if (cursorUser.moveToFirst()) b.tvActiveSeekers.text = cursorUser.getInt(0).toString()
            cursorUser.close()

            val cursorReader = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'reader'", null)
            if (cursorReader.moveToFirst()) b.tvVerifiedReaders.text = cursorReader.getInt(0).toString()
            cursorReader.close()
        } catch (e: Exception) {
            b.tvTotalRevenue.text    = "Rp0"
            b.tvActiveSeekers.text   = "0"
            b.tvVerifiedReaders.text = "0"
        }
    }

    // ─── GRAFIK PAKET TERLARIS ────────────────────────────────────────────────

    fun drawBarChart() {
        try {
            val cursor = db.rawQuery(
                """SELECT package_name, COUNT(*) as total
                   FROM bookings
                   WHERE status IN ('done','DONE','completed','COMPLETED')
                   GROUP BY package_name
                   ORDER BY total DESC
                   LIMIT 5""",
                null
            )

            val labels = ArrayList<String>()
            val values = ArrayList<Int>()

            while (cursor.moveToNext()) {
                labels.add(cursor.getString(0) ?: "-")
                values.add(cursor.getInt(1))
            }
            cursor.close()

            if (labels.isEmpty()) {
                b.chartTopPackages.visibility = View.GONE
                return
            }

            b.chartTopPackages.visibility = View.VISIBLE

            val maxVal = values.maxOrNull() ?: 1

            val width  = 900
            val height = 400
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            canvas.drawColor(Color.WHITE)

            val barColors = intArrayOf(
                Color.parseColor("#7469B6"),
                Color.parseColor("#AD88C6"),
                Color.parseColor("#E1AFD1"),
                Color.parseColor("#9C8FCC"),
                Color.parseColor("#C4B5E0")
            )

            val paintBar   = Paint(Paint.ANTI_ALIAS_FLAG)
            val paintText  = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.parseColor("#251819")
                textSize  = 24f
                textAlign = Paint.Align.CENTER
            }
            val paintValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color     = Color.WHITE
                textSize  = 28f
                textAlign = Paint.Align.CENTER
                typeface  = android.graphics.Typeface.DEFAULT_BOLD
            }
            val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color       = Color.parseColor("#E0E0E0")
                strokeWidth = 1f
            }

            val paddingLeft   = 20f
            val paddingRight  = 20f
            val paddingTop    = 30f
            val paddingBottom = 60f
            val chartW = width - paddingLeft - paddingRight
            val chartH = height - paddingTop - paddingBottom

            // Grid lines
            for (i in 0..4) {
                val y = paddingTop + chartH - (chartH * i / 4f)
                canvas.drawLine(paddingLeft, y, width - paddingRight, y, paintGrid)
            }

            val barCount = values.size
            val barWidth = (chartW / barCount) * 0.6f
            val gap      = (chartW / barCount) * 0.4f

            for (i in 0 until barCount) {
                val barH   = (values[i].toFloat() / maxVal) * chartH
                val left   = paddingLeft + i * (barWidth + gap) + gap / 2
                val top    = paddingTop + chartH - barH
                val right  = left + barWidth
                val bottom = paddingTop + chartH

                paintBar.color = barColors[i % barColors.size]
                canvas.drawRoundRect(RectF(left, top, right, bottom), 12f, 12f, paintBar)

                // Nilai di dalam bar
                if (barH > 40f) {
                    canvas.drawText(values[i].toString(), left + barWidth / 2, top + 36f, paintValue)
                }

                // Label di bawah
                val labelX = left + barWidth / 2
                val label  = labels[i].let { if (it.length > 10) it.take(10) + "…" else it }
                canvas.drawText(label, labelX, height - paddingBottom + 36f, paintText)
            }

            b.chartTopPackages.setImageBitmap(bitmap)

        } catch (e: Exception) {
            e.printStackTrace()
            b.chartTopPackages.visibility = View.GONE
        }
    }

    // ─── EXPORT LAPORAN ───────────────────────────────────────────────────────

    fun exportLaporan() {
        try {
            val sdf  = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val now  = sdf.format(Date())
            val sb   = StringBuilder()

            sb.appendLine("============================================")
            sb.appendLine("        LAPORAN TAROT MEOW")
            sb.appendLine("        Digenerate: $now")
            sb.appendLine("============================================")
            sb.appendLine()

            // Total revenue
            val cRev = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null)
            val revenue = if (cRev.moveToFirst()) cRev.getInt(0) else 0
            cRev.close()
            sb.appendLine("TOTAL PENDAPATAN   : Rp$revenue")

            // Total booking
            val cAll = db.rawQuery("SELECT COUNT(*) FROM bookings", null)
            val totalBooking = if (cAll.moveToFirst()) cAll.getInt(0) else 0
            cAll.close()
            sb.appendLine("TOTAL BOOKING      : $totalBooking")

            // Selesai
            val cDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null)
            val done = if (cDone.moveToFirst()) cDone.getInt(0) else 0
            cDone.close()
            sb.appendLine("BOOKING SELESAI    : $done")

            // Pending
            val cPend = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID','pending','PENDING')", null)
            val pending = if (cPend.moveToFirst()) cPend.getInt(0) else 0
            cPend.close()
            sb.appendLine("BOOKING PENDING    : $pending")

            // Customer
            val cCust = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'customer'", null)
            val customers = if (cCust.moveToFirst()) cCust.getInt(0) else 0
            cCust.close()
            sb.appendLine("TOTAL CUSTOMER     : $customers")

            // Reader
            val cReader = db.rawQuery("SELECT COUNT(*) FROM users WHERE role = 'reader'", null)
            val readers = if (cReader.moveToFirst()) cReader.getInt(0) else 0
            cReader.close()
            sb.appendLine("TOTAL READER       : $readers")

            sb.appendLine()
            sb.appendLine("--------------------------------------------")
            sb.appendLine("TOP 5 PAKET TERLARIS")
            sb.appendLine("--------------------------------------------")

            val cTop = db.rawQuery(
                """SELECT package_name, COUNT(*) as total, SUM(total_price) as rev
                   FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')
                   GROUP BY package_name ORDER BY total DESC LIMIT 5""", null)
            var rank = 1
            while (cTop.moveToNext()) {
                sb.appendLine("$rank. ${cTop.getString(0)}")
                sb.appendLine("   Jumlah: ${cTop.getInt(1)} sesi | Pendapatan: Rp${cTop.getInt(2)}")
                rank++
            }
            cTop.close()

            sb.appendLine()
            sb.appendLine("--------------------------------------------")
            sb.appendLine("DETAIL BOOKING TERBARU (20 terakhir)")
            sb.appendLine("--------------------------------------------")

            val cDetail = db.rawQuery(
                """SELECT b.id, u.name, b.email, b.package_name, b.booking_date,
                          b.status, b.total_price, b.reader_name
                   FROM bookings b
                   LEFT JOIN users u ON b.email = u.email
                   ORDER BY b.id DESC LIMIT 20""", null)
            while (cDetail.moveToNext()) {
                val custName = cDetail.getString(1)?.takeIf { it.isNotEmpty() } ?: cDetail.getString(2) ?: "-"
                sb.appendLine("ID #${cDetail.getInt(0)} | $custName")
                sb.appendLine("   Paket   : ${cDetail.getString(3)}")
                sb.appendLine("   Tanggal : ${cDetail.getString(4)}")
                sb.appendLine("   Status  : ${cDetail.getString(5)?.uppercase()}")
                sb.appendLine("   Harga   : Rp${cDetail.getInt(6)}")
                sb.appendLine("   Reader  : ${cDetail.getString(7)?.takeIf { it.isNotEmpty() } ?: "-"}")
                sb.appendLine()
            }
            cDetail.close()

            sb.appendLine("============================================")
            sb.appendLine("        END OF REPORT — TAROT MEOW")
            sb.appendLine("============================================")

            // Simpan ke Downloads
            val fileName  = "TarotMeow_Laporan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists()) downloads.mkdirs()
            val file = File(downloads, fileName)
            FileWriter(file).use { it.write(sb.toString()) }

            Toast.makeText(this, "Laporan disimpan di Downloads/$fileName", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal export: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStats()
        drawBarChart()
    }
}