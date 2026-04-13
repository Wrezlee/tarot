package putra.yanuar.tarot

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityOrderBinding
import java.util.Calendar

class OrderActivity : AppCompatActivity() {

    lateinit var b: ActivityOrderBinding
    lateinit var db: SQLiteDatabase
    var selectedDate = ""
    var selectedTime = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        val emailUser = intent.getStringExtra("USER_EMAIL") ?: ""

        setupSpinner()

        b.btnSetDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = "$day/${month + 1}/$year"
                b.btnSetDate.text = selectedDate
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }

        b.btnSetTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                b.btnSetTime.text = selectedTime
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }

        b.btnConfirmOrder.setOnClickListener {
            saveOrder(emailUser)
        }
    }

    fun setupSpinner() {
        val listPaket = ArrayList<String>()
        listPaket.add("--- Pilih Paket Ritual ---")

        try {
            // PERBAIKAN 1: Nama tabel diganti ke 'tarot_packages'
            val cursor = db.rawQuery("SELECT name, price FROM tarot_packages", null)
            var currentCategory = ""

            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(0)
                    val price = cursor.getInt(1)

                    val category = when {
                        name.contains("Kartu") -> "🔮 REGULER TAROT"
                        name.contains("Telapak") -> "✋ PALM READING"
                        name.contains("Chat") -> "💬 INTERAKTIF CHAT"
                        name.contains("Call") -> "📞 INTERAKTIF CALL"
                        else -> "✨ LAINNYA"
                    }

                    if (category != currentCategory) {
                        listPaket.add(category)
                        currentCategory = category
                    }
                    listPaket.add("      $name - Rp$price")
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val adapter = ArrayAdapter(this, R.layout.item_spinner, listPaket)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spPaket.adapter = adapter
    }

    fun saveOrder(email: String) {
        val paketRaw = b.spPaket.selectedItem?.toString() ?: ""

        if (paketRaw.contains("---") || paketRaw.contains("🔮") ||
            paketRaw.contains("✋") || paketRaw.contains("💬") || paketRaw.contains("📞")) {
            Toast.makeText(this, "Silakan pilih paket ritual yang tersedia!", Toast.LENGTH_SHORT).show()
            return
        }

        val paketClean = paketRaw.trim().split(" - ")[0]

        val paymentId = b.rgPayment.checkedRadioButtonId
        if (paymentId == -1 || selectedDate == "" || selectedTime == "") {
            Toast.makeText(this, "Lengkapi jadwal dan metode pembayaran!", Toast.LENGTH_SHORT).show()
            return
        }

        val payment = if (b.rbTransfer.isChecked) "Transfer" else "Dana/ShopeePay"

        // LOGIKA HARGA: Ambil harga dasar dari string yang dipilih
        var totalPrice = 0
        try {
            val parts = paketRaw.split("Rp")
            if (parts.size > 1) {
                totalPrice = parts[1].trim().toInt()
            }
        } catch (e: Exception) { totalPrice = 0 }

        if (b.cbOracle.isChecked) totalPrice += 10000
        if (b.cbFastTrack.isChecked) totalPrice += 30000

        val notes = b.etNotes.text.toString()

        try {
            var userId = 0
            val cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(email))
            if (cursor.moveToFirst()) {
                userId = cursor.getInt(0)
            }
            cursor.close()

            // PERBAIKAN 2: Gunakan tabel bookings
            val sql = """
                INSERT INTO bookings (user_id, email, package_name, payment_method, booking_date, booking_time, notes, status, total_price) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()

            db.execSQL(sql, arrayOf(
                userId, email, paketClean, payment, selectedDate, selectedTime, notes, "paid", totalPrice
            ))

            // Simpan pertanyaan
            val cursorId = db.rawQuery("SELECT last_insert_rowid()", null)
            if (cursorId.moveToFirst()) {
                val lastBookingId = cursorId.getInt(0)
                db.execSQL("INSERT INTO questions (booking_id, question) VALUES (?, ?)",
                    arrayOf(lastBookingId, notes))
            }
            cursorId.close()

            Toast.makeText(this, "Ritual berhasil dipesan! ✨", Toast.LENGTH_LONG).show()
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}