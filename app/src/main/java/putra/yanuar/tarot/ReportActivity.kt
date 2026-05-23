package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReportBinding

class ReportActivity : AppCompatActivity() {

    lateinit var b: ActivityReportBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityReportBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).readableDatabase
        loadReport()
    }

    fun loadReport() {
        try {
            // Total pendapatan hanya dari booking yang sudah selesai (done/completed)
            val cursorTotal = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null
            )
            if (cursorTotal.moveToFirst()) {
                val total = cursorTotal.getInt(0)
                b.tvReportTotalRevenue.text = "Rp$total"
            }
            cursorTotal.close()

            // Jumlah total booking
            val cursorCount = db.rawQuery("SELECT COUNT(*) FROM bookings", null)
            if (cursorCount.moveToFirst()) {
                b.tvReportTotalBooking.text = cursorCount.getInt(0).toString()
            }
            cursorCount.close()

            // Booking selesai
            val cursorDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null
            )
            if (cursorDone.moveToFirst()) {
                b.tvReportDone.text = cursorDone.getInt(0).toString()
            }
            cursorDone.close()

            // Booking pending/paid (belum selesai)
            val cursorPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID','pending','PENDING')", null
            )
            if (cursorPending.moveToFirst()) {
                b.tvReportPending.text = cursorPending.getInt(0).toString()
            }
            cursorPending.close()

            // Paket terlaris (hanya dari booking yang sudah selesai)
            val cursorTop = db.rawQuery(
                "SELECT package_name, COUNT(*) as total FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED') GROUP BY package_name ORDER BY total DESC LIMIT 1", null
            )
            if (cursorTop.moveToFirst()) {
                b.tvReportTopPackage.text = cursorTop.getString(0)
            } else {
                b.tvReportTopPackage.text = "-"
            }
            cursorTop.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}