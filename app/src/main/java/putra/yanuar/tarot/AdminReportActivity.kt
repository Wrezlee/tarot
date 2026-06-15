package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAdminReportBinding


class AdminReportActivity : AppCompatActivity() {

    lateinit var b: ActivityAdminReportBinding
    lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAdminReportBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).readableDatabase
        loadReport()
    }

    fun loadReport() {
        try {
            val cursorTotal = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null
            )
            if (cursorTotal.moveToFirst()) {
                val total = cursorTotal.getInt(0)
                b.tvReportTotalRevenue.text = "Rp $total"
            }
            cursorTotal.close()

            val cursorCount = db.rawQuery("SELECT COUNT(*) FROM bookings", null)
            if (cursorCount.moveToFirst()) {
                b.tvReportTotalBooking.text = cursorCount.getInt(0).toString()
            }
            cursorCount.close()

            val cursorDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('done','DONE','completed','COMPLETED')", null
            )
            if (cursorDone.moveToFirst()) {
                b.tvReportDone.text = cursorDone.getInt(0).toString()
            }
            cursorDone.close()

            val cursorPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID','pending','PENDING')", null
            )
            if (cursorPending.moveToFirst()) {
                b.tvReportPending.text = cursorPending.getInt(0).toString()
            }
            cursorPending.close()

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