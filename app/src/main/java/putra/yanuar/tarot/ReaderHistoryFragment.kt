package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentReaderHistoryBinding
import putra.yanuar.tarot.databinding.ItemReaderHistoryBinding
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
            // Format tanggal yang sama persis dengan yang disimpan di DB
            // CustomerOrderActivity: "%d/%d/%d".format(d, m + 1, y) → "17/6/2026"
            val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())

            val calToday = Calendar.getInstance()
            val todayStr = sdf.format(calToday.time)

            // Hitung start of week (Senin) dengan cara yang aman
            val calWeekStart = Calendar.getInstance()
            // Set ke hari Senin minggu ini
            calWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            // Jika hari ini Minggu (DAY_OF_WEEK=1), mundur 6 hari ke Senin lalu
            if (calToday.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                calWeekStart.add(Calendar.DAY_OF_MONTH, -6)
            }
            // Reset ke jam 00:00:00 supaya perbandingan akurat
            calWeekStart.set(Calendar.HOUR_OF_DAY, 0)
            calWeekStart.set(Calendar.MINUTE, 0)
            calWeekStart.set(Calendar.SECOND, 0)
            calWeekStart.set(Calendar.MILLISECOND, 0)

            // Ambil semua booking selesai milik reader ini (termasuk reader_id=0)
            val cursor = db.rawQuery(
                """SELECT total_price, booking_date FROM bookings
                   WHERE (reader_id = ? OR reader_id = 0 OR reader_id IS NULL)
                   AND status IN ('completed','COMPLETED','done','DONE')""",
                arrayOf(readerId.toString())
            )

            var totalAll = 0
            var totalToday = 0
            var totalWeek = 0

            while (cursor.moveToNext()) {
                val price   = cursor.getInt(0)
                val dateStr = cursor.getString(1) ?: ""
                totalAll += price

                try {
                    // Cek apakah tanggal booking = hari ini
                    if (dateStr == todayStr) {
                        totalToday += price
                    }

                    // Cek apakah tanggal booking dalam minggu ini
                    val bookingDate = sdf.parse(dateStr)
                    if (bookingDate != null) {
                        val calBooking = Calendar.getInstance()
                        calBooking.time = bookingDate
                        // Booking minggu ini jika >= Senin minggu ini
                        if (!calBooking.before(calWeekStart)) {
                            totalWeek += price
                        }
                    }
                } catch (_: Exception) {
                    // Skip jika format tanggal tidak valid
                }
            }
            cursor.close()

            b.tvEarningTotal.text = "Rp ${formatRupiah(totalAll)}"
            b.tvEarningToday.text = "Rp ${formatRupiah(totalToday)}"
            b.tvEarningWeek.text  = "Rp ${formatRupiah(totalWeek)}"

        } catch (e: Exception) {
            e.printStackTrace()
            b.tvEarningTotal.text = "Rp 0"
            b.tvEarningToday.text = "Rp 0"
            b.tvEarningWeek.text  = "Rp 0"
        }
    }

    // Format angka dengan titik ribuan: 130000 → "130.000"
    private fun formatRupiah(amount: Int): String {
        return String.format(Locale.getDefault(), "%,d", amount).replace(',', '.')
    }

    private fun loadStats() {
        try {
            // FIX: sertakan reader_id=0 supaya konsisten dengan loadEarnings dan loadHistory
            val cDone = db.rawQuery(
                """SELECT COUNT(*) FROM bookings 
                   WHERE status IN ('completed','COMPLETED','done','DONE') 
                   AND (reader_id = ? OR reader_id = 0 OR reader_id IS NULL)""",
                arrayOf(readerId.toString())
            )
            if (cDone.moveToFirst()) b.tvStatsDone.text = cDone.getInt(0).toString()
            cDone.close()

            val cPending = db.rawQuery(
                """SELECT COUNT(*) FROM bookings 
                   WHERE status IN ('paid','PAID','pending','PENDING') 
                   AND (reader_id = ? OR reader_id = 0 OR reader_id IS NULL)""",
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
            // Tampilkan semua status kecuali cancelled, termasuk booking reader_id=0
            val cursor = db.rawQuery(
                """SELECT b.id, u.name, b.email, b.package_name, b.booking_date,
                          b.booking_time, b.status, b.notes, b.answer, b.total_price
                   FROM bookings b
                   LEFT JOIN users u ON b.email = u.email
                   WHERE (b.reader_id = ? OR b.reader_id = 0 OR b.reader_id IS NULL)
                   AND b.status NOT IN ('cancelled','CANCELLED')
                   ORDER BY b.id DESC LIMIT 50""",
                arrayOf(readerId.toString())
            )

            val container = b.containerReaderHistory
            container.removeAllViews()

            if (!cursor.moveToFirst()) {
                cursor.close()
                val tvEmpty = TextView(requireContext()).apply {
                    text = "Belum ada booking"
                    setTextColor(0xFFAD88C6.toInt())
                    textSize = 13f
                    setPadding(0, 16.dpToPx(), 0, 16.dpToPx())
                }
                container.addView(tvEmpty)
                return
            }

            do {
                val bookingId    = cursor.getInt(0)
                val customerName = cursor.getString(1)?.takeIf { it.isNotEmpty() }
                    ?: cursor.getString(2) ?: "-"
                val packageName  = cursor.getString(3) ?: "-"
                val date         = cursor.getString(4) ?: "-"
                val time         = cursor.getString(5) ?: "--:--"
                val status       = (cursor.getString(6) ?: "").uppercase()
                val notes        = cursor.getString(7) ?: ""
                val answer       = cursor.getString(8) ?: ""
                val totalPrice   = cursor.getInt(9)

                val itemBinding = ItemReaderHistoryBinding.inflate(
                    layoutInflater, container, false
                )

                itemBinding.tvReaderItemPackage.text      = packageName
                itemBinding.tvReaderItemCustomerName.text = customerName
                itemBinding.tvReaderItemDate.text         = "📅 $date   ⏰ $time"
                itemBinding.tvReaderItemQuestion.text     =
                    if (notes.isNotEmpty()) "Q: $notes" else "Q: Tidak ada pertanyaan"
                itemBinding.tvReaderItemAnswer.text       =
                    if (answer.isNotEmpty()) "A: $answer" else "A: Belum ada jawaban."
                itemBinding.tvReaderItemEarning.text      = "Rp ${formatRupiah(totalPrice)}"

                val (bgColor, textColor) = when (status) {
                    "COMPLETED", "DONE" -> Pair(0xFFE8F5E9.toInt(), 0xFF388E3C.toInt())
                    "PROCESSING"        -> Pair(0xFFE3F2FD.toInt(), 0xFF1565C0.toInt())
                    "PAID"              -> Pair(0xFFFFF3E0.toInt(), 0xFFE65100.toInt())
                    "PENDING"           -> Pair(0xFFF3E5F5.toInt(), 0xFFAD88C6.toInt())
                    else                -> Pair(0xFFF3E5F5.toInt(), 0xFFAD88C6.toInt())
                }
                itemBinding.tvReaderItemStatus.text = status
                itemBinding.tvReaderItemStatus.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(bgColor)
                itemBinding.tvReaderItemStatus.setTextColor(textColor)

                if (status == "PROCESSING") {
                    itemBinding.layoutInputJawaban.visibility = View.VISIBLE

                    if (answer.isNotEmpty()) {
                        itemBinding.etReaderAnswer.setText(answer)
                    }

                    itemBinding.btnKirimJawaban.setOnClickListener {
                        val newAnswer = itemBinding.etReaderAnswer.text.toString().trim()
                        if (newAnswer.isEmpty()) {
                            Toast.makeText(requireContext(), "Tulis jawaban dulu!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        try {
                            db.execSQL(
                                "UPDATE bookings SET answer = ? WHERE id = ?",
                                arrayOf(newAnswer, bookingId.toString())
                            )
                            Toast.makeText(requireContext(), "Jawaban disimpan ✅", Toast.LENGTH_SHORT).show()
                            itemBinding.tvReaderItemAnswer.text = "A: $newAnswer"
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    itemBinding.btnTandaiSelesai.setOnClickListener {
                        val newAnswer = itemBinding.etReaderAnswer.text.toString().trim()
                        if (newAnswer.isEmpty()) {
                            Toast.makeText(requireContext(), "Isi jawaban sebelum menyelesaikan sesi!", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        try {
                            db.execSQL(
                                "UPDATE bookings SET status = 'completed', answer = ? WHERE id = ?",
                                arrayOf(newAnswer, bookingId.toString())
                            )
                            Toast.makeText(requireContext(), "Sesi selesai! 🔮", Toast.LENGTH_SHORT).show()
                            // Refresh semua data termasuk pendapatan
                            loadEarnings()
                            loadStats()
                            loadHistory()
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    itemBinding.layoutInputJawaban.visibility = View.GONE
                }

                container.addView(itemBinding.root)

            } while (cursor.moveToNext())

            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()
}