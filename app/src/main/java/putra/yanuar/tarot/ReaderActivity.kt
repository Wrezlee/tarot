package putra.yanuar.tarot

import android.Manifest
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import putra.yanuar.tarot.databinding.ActivityReaderBinding
import java.text.SimpleDateFormat
import java.util.*

class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase

    private var readerId:   Int    = 0
    private var readerName: String = ""
    private var userEmail:  String = ""

    private var verifiedBookingId: String = ""

    fun getDbObject(): SQLiteDatabase = db
    fun getReaderId(): Int            = readerId
    fun getUserEmail(): String        = userEmail

    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            handleQrScanResult(result.contents)
        } else {
            Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DBOpenHelper(this).writableDatabase

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        if (userEmail.isNotEmpty()) {
            val c = db.rawQuery("SELECT id, name FROM users WHERE email = ?", arrayOf(userEmail))
            if (c.moveToFirst()) {
                readerId   = c.getInt(0)
                readerName = c.getString(1) ?: ""
            }
            c.close()
        }

        if (readerId == 0) {
            readerId   = intent.getStringExtra("USER_ID")?.toIntOrNull() ?: 0
            readerName = intent.getStringExtra("USER_NAME") ?: ""
        }

        try {
            db.execSQL("UPDATE users SET is_online = 1 WHERE id = ?", arrayOf(readerId.toString()))
        } catch (_: Exception) {}

        setupToolbar()
        setupBottomNav()
        setupScanButton()
        setupStartReadingButton()
        refreshDashboard()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarReader)
        supportActionBar?.title = "Dashboard Reader"
    }

    private fun setupBottomNav() {
        binding.navbarReader.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.readerHome -> {
                    binding.scrollReaderHome.visibility = View.VISIBLE
                    binding.containerReader.visibility  = View.GONE
                    supportActionBar?.title = "Dashboard Reader"
                    true
                }
                R.id.readerHistory -> {
                    binding.scrollReaderHome.visibility = View.GONE
                    binding.containerReader.visibility  = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerReader, ReaderHistoryFragment.newInstance(readerId))
                        .commit()
                    supportActionBar?.title = "History"
                    true
                }
                R.id.readerProfile -> {
                    binding.scrollReaderHome.visibility = View.GONE
                    binding.containerReader.visibility  = View.VISIBLE
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.containerReader, ReaderProfileFragment.newInstance(readerId))
                        .commit()
                    supportActionBar?.title = "Profile"
                    true
                }
                else -> false
            }
        }
    }

    private fun setupScanButton() {
        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionThenScan()
        }
    }

    private fun setupStartReadingButton() {
        binding.btnStartReading.setOnClickListener {
            if (verifiedBookingId.isEmpty()) return@setOnClickListener
            db.execSQL(
                "UPDATE bookings SET status = 'processing' WHERE id = ?",
                arrayOf(verifiedBookingId)
            )
            Toast.makeText(this, "Sesi dimulai! Customer masuk antrean aktif.", Toast.LENGTH_SHORT).show()
            binding.btnStartReading.visibility = View.GONE
            verifiedBookingId = ""
            refreshDashboard()
        }
    }

    private fun checkCameraPermissionThenScan() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchScanner()

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Izin Kamera Dibutuhkan")
                    .setMessage("Untuk scan QR tiket customer, aplikasi butuh akses kamera.")
                    .setPositiveButton("Izinkan") { _, _ ->
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            else -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) launchScanner()
        else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setPrompt("Arahkan ke QR Tiket customer")
            setBeepEnabled(false)
            setOrientationLocked(true)
            setBarcodeImageEnabled(false)
        }
        scanLauncher.launch(options)
    }

    private fun handleQrScanResult(raw: String) {
        val data = QrHelper.parseQrContent(raw)

        if (data == null) {
            AlertDialog.Builder(this)
                .setTitle("QR Tidak Valid")
                .setMessage("Format QR tidak dikenali. Pastikan customer scan QR dari aplikasi Tarot Meow.")
                .setPositiveButton("OK", null).show()
            return
        }

        val cursor = db.rawQuery(
            "SELECT id, status, package_name, booking_date, booking_time, payment_method, total_price FROM bookings WHERE id = ?",
            arrayOf(data.bookingId)
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            AlertDialog.Builder(this)
                .setTitle("Booking Tidak Ditemukan")
                .setMessage("ID Booking #${data.bookingId} tidak ada di sistem.")
                .setPositiveButton("OK", null).show()
            return
        }

        val status     = cursor.getString(1)?.uppercase() ?: "PENDING"
        val pkgName    = cursor.getString(2) ?: "-"
        val date       = cursor.getString(3) ?: "-"
        val time       = cursor.getString(4) ?: "--:--"
        val payment    = cursor.getString(5) ?: "-"
        val totalPrice = cursor.getInt(6)
        cursor.close()

        if (status !in listOf("PENDING", "PAID", "CONFIRMED")) {
            AlertDialog.Builder(this)
                .setTitle("⚠ Status Tidak Valid")
                .setMessage("Booking ini berstatus: $status\n\nHanya booking PENDING / PAID yang bisa dimulai.")
                .setPositiveButton("OK", null).show()
            return
        }

        showVerifiedDialog(data.bookingId, data.customerName, pkgName, date, time, payment, totalPrice)
    }

    private fun showVerifiedDialog(
        bookingId: String, customerName: String, pkgName: String,
        date: String, time: String, payment: String, totalPrice: Int
    ) {
        AlertDialog.Builder(this)
            .setTitle("Tiket Terverifikasi!")
            .setMessage(
                "Customer  : $customerName\n" +
                        "Paket     : $pkgName\n" +
                        "Jadwal    : $date  $time\n" +
                        "Bayar     : $payment\n" +
                        "Total     : Rp${"%,d".format(totalPrice)}\n\n" +
                        "Mulai sesi ramalan sekarang?"
            )
            .setPositiveButton("Mulai Ramalan") { _, _ ->
                db.execSQL(
                    "UPDATE bookings SET status = 'processing' WHERE id = ?",
                    arrayOf(bookingId)
                )
                verifiedBookingId = bookingId
                Toast.makeText(this, "$customerName masuk antrean aktif!", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun refreshDashboard() {
        refreshStats()
        refreshAntreanAktif()
        refreshCalendar()
    }

    private fun refreshStats() {
        val cPending = db.rawQuery(
            "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('pending','PENDING','paid','PAID','confirmed','CONFIRMED')",
            arrayOf(readerId.toString())
        )
        val pending = if (cPending.moveToFirst()) cPending.getInt(0) else 0
        cPending.close()

        val cDone = db.rawQuery(
            "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
            arrayOf(readerId.toString())
        )
        val done = if (cDone.moveToFirst()) cDone.getInt(0) else 0
        cDone.close()

        binding.tvPendingCount.text   = pending.toString()
        binding.tvCompletedCount.text = done.toString()
        binding.tvReaderGreeting.text = "Dashboard Reader"
        binding.tvReaderSubtitle.text = "Halo, $readerName 🌙"
    }
    private fun refreshAntreanAktif() {

        val cProcessing = db.rawQuery(
            """SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.booking_time
               FROM bookings b
               LEFT JOIN users u ON b.email = u.email
               WHERE b.reader_id = ? AND b.status IN ('processing','PROCESSING')
               ORDER BY b.booking_date ASC, b.booking_time ASC""",
            arrayOf(readerId.toString())
        )

        if (cProcessing.moveToFirst()) {
            val custName = cProcessing.getString(1)?.takeIf { it.isNotEmpty() }
                ?: cProcessing.getString(2) ?: "-"
            binding.tvNextCustomerName.text = "🔮 $custName (Sedang Berlangsung)"
            binding.tvNextBookingTime.text  = cProcessing.getString(5) ?: "--:--"
            binding.tvNextBookingDate.text  = " ${cProcessing.getString(4) ?: ""}"
            binding.tvNextPackageName.text  = " ${cProcessing.getString(3) ?: ""}"
            binding.btnStartReading.visibility = View.GONE
            cProcessing.close()
            return
        }
        cProcessing.close()

        val cNext = db.rawQuery(
            """SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.booking_time
               FROM bookings b
               LEFT JOIN users u ON b.email = u.email
               WHERE b.reader_id = ? AND b.status IN ('pending','PENDING','paid','PAID','confirmed','CONFIRMED')
               ORDER BY b.booking_date ASC, b.booking_time ASC LIMIT 1""",
            arrayOf(readerId.toString())
        )

        if (cNext.moveToFirst()) {
            val custName = cNext.getString(1)?.takeIf { it.isNotEmpty() }
                ?: cNext.getString(2) ?: "-"
            binding.tvNextCustomerName.text = custName
            binding.tvNextBookingTime.text  = cNext.getString(5) ?: "--:--"
            binding.tvNextBookingDate.text  = " ${cNext.getString(4) ?: ""}"
            binding.tvNextPackageName.text  = " ${cNext.getString(3) ?: ""}"

            // Tampilkan tombol mulai jika booking ini sudah di-scan sebelumnya
            if (verifiedBookingId == cNext.getString(0)) {
                binding.btnStartReading.visibility = View.VISIBLE
            }
        } else {
            binding.tvNextCustomerName.text = "Belum Ada Antrean"
            binding.tvNextBookingTime.text  = "--:--"
            binding.tvNextBookingDate.text  = ""
            binding.tvNextPackageName.text  = "Siap melayani sesi baru"
            binding.btnStartReading.visibility = View.GONE
        }
        cNext.close()
    }


    private fun refreshCalendar() {
        val container = binding.containerCalendar
        container.removeAllViews()

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        val offsetToMonday = if (dow == Calendar.SUNDAY) -6 else -(dow - Calendar.MONDAY)
        cal.add(Calendar.DAY_OF_MONTH, offsetToMonday)
        val startOfWeek = cal.clone() as Calendar
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val endOfWeek = cal.clone() as Calendar

        val sdfDb   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisp = SimpleDateFormat("EEE, d MMM", Locale("id"))

        val startStr = sdfDb.format(startOfWeek.time)
        val endStr   = sdfDb.format(endOfWeek.time)

        val cursor = db.rawQuery(
            """SELECT b.booking_date, b.booking_time, u.name, b.email,
                      b.package_name, b.status, b.total_price
               FROM bookings b
               LEFT JOIN users u ON b.email = u.email
               WHERE b.reader_id = ?
                 AND b.booking_date >= ?
                 AND b.booking_date <= ?
               ORDER BY b.booking_date ASC, b.booking_time ASC""",
            arrayOf(readerId.toString(), startStr, endStr)
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            val tvEmpty = TextView(this).apply {
                text = "Tidak ada booking minggu ini"
                setTextColor(Color.parseColor("#AD88C6"))
                textSize = 13f
                setPadding(0, 8, 0, 8)
            }
            container.addView(tvEmpty)
            return
        }

        val grouped = LinkedHashMap<String, MutableList<Array<String>>>()
        do {
            val date    = cursor.getString(0) ?: continue
            val time    = cursor.getString(1) ?: "--:--"
            val name    = cursor.getString(2)?.takeIf { it.isNotEmpty() } ?: cursor.getString(3) ?: "-"
            val pkg     = cursor.getString(4) ?: "-"
            val status  = cursor.getString(5) ?: "-"
            val price   = cursor.getInt(6)
            grouped.getOrPut(date) { mutableListOf() }
                .add(arrayOf(time, name, pkg, status, price.toString()))
        } while (cursor.moveToNext())
        cursor.close()

        val dp = resources.displayMetrics.density

        for ((date, items) in grouped) {
            val parsedDate = try { sdfDb.parse(date) } catch (_: Exception) { null }
            val displayDate = if (parsedDate != null) sdfDisp.format(parsedDate) else date

            val tvDate = TextView(this).apply {
                text = displayDate
                setTextColor(Color.parseColor("#7469B6"))
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, (12 * dp).toInt(), 0, 4)
            }
            container.addView(tvDate)

            for ((idx, item) in items.withIndex()) {
                val (time, custName, pkg, status, priceStr) = item

                val statusColor = when (status.lowercase()) {
                    "processing"          -> "#FF9800"
                    "completed", "done"   -> "#4CAF50"
                    "cancelled"           -> "#F44336"
                    else                  -> "#AD88C6"
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(
                        (12 * dp).toInt(), (10 * dp).toInt(),
                        (12 * dp).toInt(), (10 * dp).toInt()
                    )
                    background = ContextCompat.getDrawable(
                        this@ReaderActivity,
                        android.R.drawable.dialog_holo_light_frame
                    )
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, (4 * dp).toInt(), 0, (4 * dp).toInt())
                    }
                    layoutParams = lp
                }

                val tvTime = TextView(this).apply {
                    text = time
                    setTextColor(Color.parseColor("#7469B6"))
                    textSize = 12f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        (52 * dp).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                val tvInfo = TextView(this).apply {
                    text = "$custName\n$pkg"
                    setTextColor(Color.parseColor("#251819"))
                    textSize = 12f
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }

                val tvStatus = TextView(this).apply {
                    text = status.replaceFirstChar { it.uppercase() }
                    setTextColor(Color.parseColor(statusColor))
                    textSize = 11f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                row.addView(tvTime)
                row.addView(tvInfo)
                row.addView(tvStatus)
                container.addView(row)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reader_menu_option, menu)
        menu?.findItem(R.id.menu_music)?.title =
            if (MusicManager.isMuted()) " Musik OFF" else " Musik ON"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Yakin ingin keluar?")
                    .setPositiveButton("Ya") { _, _ ->
                        try {
                            db.execSQL(
                                "UPDATE users SET is_online = 0 WHERE id = ?",
                                arrayOf(readerId.toString())
                            )
                        } catch (_: Exception) {}
                        val intent = android.content.Intent(this, MainActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                                android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
                true
            }
            R.id.menu_music -> {
                val nowMuted = MusicManager.toggleMute()
                item.title = if (nowMuted) "Musik OFF" else "Musik ON"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshDashboard()
        MusicManager.resume()
    }

    override fun onPause() {
        super.onPause()
        MusicManager.pause()
    }

    companion object {
        private const val REQ_CAMERA = 101
    }
}