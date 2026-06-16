package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentReaderHistoryBinding
import putra.yanuar.tarot.databinding.ItemCustomerHistoryBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReaderHistoryFragment : Fragment() {

    private lateinit var b: FragmentReaderHistoryBinding
    private lateinit var db: SQLiteDatabase
    private var readerId: Int = 0

    companion object {
        private const val ARG_READER_ID = "reader_id"

        fun newInstance(readerId: Int): ReaderHistoryFragment {
            val fragment = ReaderHistoryFragment()
            val args = Bundle()
            args.putInt(ARG_READER_ID, readerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentReaderHistoryBinding.inflate(inflater, container, false)

        readerId = arguments?.getInt(ARG_READER_ID) ?: 0
        db = DBOpenHelper(requireContext()).writableDatabase

        loadEarnings()
        loadStats()
        loadHistory()

        return b.root
    }

    override fun onResume() {
        super.onResume()
        loadEarnings()
        loadStats()
        loadHistory()
    }

    private fun loadEarnings() {
        try {
            val today = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                .format(Calendar.getInstance().time)

            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            cal.add(Calendar.DAY_OF_MONTH, -(dayOfWeek - Calendar.MONDAY))
            val startOfWeek = cal.time

            val cursor = db.rawQuery(
                """SELECT total_price, booking_date FROM bookings
                   WHERE reader_id = ?
                   AND status IN ('completed','COMPLETED','done','DONE')""",
                arrayOf(readerId.toString())
            )

            var totalAll = 0; var totalToday = 0; var totalWeek = 0
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

            while (cursor.moveToNext()) {
                val price   = cursor.getInt(0)
                val dateStr = cursor.getString(1) ?: ""
                totalAll += price
                try {
                    val bookingDate = sdf.parse(dateStr)
                    if (bookingDate != null) {
                        if (dateStr == today) totalToday += price
                        if (!bookingDate.before(startOfWeek)) totalWeek += price
                    }
                } catch (_: Exception) {}
            }
            cursor.close()

            b.tvEarningTotal.text = "Rp $totalAll"
            b.tvEarningToday.text = "Rp $totalToday"
            b.tvEarningWeek.text  = "Rp $totalWeek"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadStats() {
        try {
            val cDone = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('completed','COMPLETED','done','DONE') AND reader_id = ?",
                arrayOf(readerId.toString())
            )
            if (cDone.moveToFirst()) b.tvStatsDone.text = cDone.getInt(0).toString()
            cDone.close()

            val cPending = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE status IN ('paid','PAID','pending','PENDING') AND reader_id = ?",
                arrayOf(readerId.toString())
            )
            if (cPending.moveToFirst()) b.tvStatsPending.text = cPending.getInt(0).toString()
            cPending.close()
        } catch (e: Exception) {
            b.tvStatsDone.text    = "0"
            b.tvStatsPending.text = "0"
        }
    }

    private fun loadHistory() {
        try {
            val cursor = db.rawQuery(
                """SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.status, q.question, q.answer
                   FROM bookings b
                   LEFT JOIN users u ON b.email = u.email
                   LEFT JOIN questions q ON b.id = q.booking_id
                   WHERE b.reader_id = ?
                   AND b.status IN ('completed','COMPLETED','done','DONE')
                   ORDER BY b.id DESC LIMIT 20""",
                arrayOf(readerId.toString())
            )

            val container = b.containerReaderHistory
            container.removeAllViews()

            if (!cursor.moveToFirst()) {
                cursor.close()
                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada sesi yang selesai"
                    setTextColor(0xFFAD88C6.toInt())
                    textSize = 13f
                    setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
                }
                container.addView(tvEmpty)
                return
            }

            do {
                val customerName = cursor.getString(1)?.takeIf { it.isNotEmpty() }
                    ?: cursor.getString(2) ?: "-"
                val status   = (cursor.getString(5) ?: "").uppercase()
                val question = "Q: " + (cursor.getString(6) ?: "Tidak ada pertanyaan")
                val answer   = "A: " + (cursor.getString(7) ?: "Belum dijawab")

                val binding = ItemCustomerHistoryBinding.inflate(layoutInflater, container, false)
                binding.tvItemPackage.text  = cursor.getString(3) ?: "-"
                binding.tvItemDate.text     = cursor.getString(4) ?: "-"
                binding.tvItemStatus.text   = status
                binding.tvItemQuestion.text = question
                binding.tvItemAnswer.text   = answer
                binding.tvItemReader.text   = "Customer: $customerName"
                binding.btnCancelOrder.visibility  = View.GONE
                binding.btnTulisUlasan.visibility  = View.GONE
                binding.btnShareRamalan.visibility = View.GONE
                binding.layoutQrTicket.visibility  = View.GONE
                container.addView(binding.root)
            } while (cursor.moveToNext())

            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}