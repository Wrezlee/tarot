package putra.yanuar.tarot

import android.Manifest
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReaderBinding
    private lateinit var db: SQLiteDatabase

    private var readerId:   Int    = 0
    private var readerName: String = ""
    private var userEmail:  String = ""

    // Booking yang sudah ter-verifikasi QR
    private var verifiedBookingId: String = ""

    // ── Public getter untuk fragment ─────────────────────────────────────────
    fun getDbObject(): SQLiteDatabase = db
    fun getReaderId(): Int            = readerId
    fun getUserEmail(): String        = userEmail

    // ── ZXing scan launcher ──────────────────────────────────────────────────
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

        userEmail  = intent.getStringExtra("USER_EMAIL") ?: ""

        // Resolve readerId & readerName dari email
        if (userEmail.isNotEmpty()) {
            val c = db.rawQuery("SELECT id, name FROM users WHERE email = ?", arrayOf(userEmail))
            if (c.moveToFirst()) {
                readerId   = c.getInt(0)
                readerName = c.getString(1) ?: ""
            }
            c.close()
        }

        // Fallback jika dikirim langsung
        if (readerId == 0) {
            readerId   = intent.getStringExtra("USER_ID")?.toIntOrNull()   ?: 0
            readerName = intent.getStringExtra("USER_NAME") ?: ""
        }

        // Toggle status online
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
            db.execSQL("UPDATE bookings SET status = 'processing' WHERE id = ?", arrayOf(verifiedBookingId))
            Toast.makeText(this, "Sesi dimulai! 🔮", Toast.LENGTH_SHORT).show()
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)`
        if (requestCode == REQ_CAMERA &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) launchScanner()
        else Toast.makeText(this, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
    }

    private fun launchScanner() {
        val options = ScanOptions().apply {
            setPrompt("Arahkan ke QR Tiket customer 🔮")
            setBeepEnabled(true)
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

        // Ambil booking dari DB lokal
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
            .setTitle("✅ Tiket Terverifikasi!")
            .setMessage(
                "Customer  : $customerName\n" +
                        "Paket     : $pkgName\n" +
                        "Jadwal    : $date  $time\n" +
                        "Bayar     : $payment\n" +
                        "Total     : Rp${"%,d".format(totalPrice)}\n\n" +
                        "Mulai sesi ramalan sekarang?"
            )
            // PERUBAHAN: "Mulai Ramalan" sekarang juga menampilkan detail di card
            // sebelum memulai sesi (sama seperti "Lihat Detail Dulu" sebelumnya),
            // kemudian langsung set status processing.
            // Tombol "Lihat Detail Dulu" dihapus.
            .setPositiveButton("🔮 Mulai Ramalan") { _, _ ->
                // Tampilkan detail di card antrean aktif
                verifiedBookingId = bookingId
                binding.tvNextCustomerName.text = customerName
                binding.tvNextPackageName.text  = " $pkgName"
                binding.tvNextBookingDate.text  = " $date"
                binding.tvNextBookingTime.text  = time
                binding.btnStartReading.visibility = View.VISIBLE

                // Langsung mulai sesi
                db.execSQL("UPDATE bookings SET status = 'processing' WHERE id = ?", arrayOf(bookingId))
                verifiedBookingId = ""
                binding.btnStartReading.visibility = View.GONE
                Toast.makeText(this, "Sesi dimulai! 🔮", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun refreshDashboard() {
        val cPending = db.rawQuery(
            "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('pending','PENDING','paid','PAID')",
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
        binding.tvReaderGreeting.text  = "Dashboard Reader"
        binding.tvReaderSubtitle.text  = "Halo, $readerName 🔮"

        // PERUBAHAN: Next booking langsung tampil otomatis tanpa perlu scan QR dulu.
        // Query mencakup semua status aktif termasuk 'confirmed'.
        val cNext = db.rawQuery(
            """SELECT b.id, u.name, b.email, b.package_name, b.booking_date, b.booking_time
               FROM bookings b
               LEFT JOIN users u ON b.email = u.email
               WHERE b.reader_id = ? AND b.status IN ('pending','PENDING','paid','PAID','confirmed','CONFIRMED')
               ORDER BY b.booking_date ASC, b.booking_time ASC LIMIT 1""",
            arrayOf(readerId.toString())
        )
        if (cNext.moveToFirst()) {
            val custName = cNext.getString(1)?.takeIf { it.isNotEmpty() } ?: cNext.getString(2) ?: "-"
            binding.tvNextCustomerName.text = custName
            binding.tvNextBookingTime.text  = cNext.getString(5) ?: "--:--"
            binding.tvNextBookingDate.text  = " ${cNext.getString(4) ?: ""}"
            binding.tvNextPackageName.text  = " ${cNext.getString(3) ?: ""}"
            // Tampilkan data antrean tapi btnStartReading tetap tersembunyi —
            // hanya muncul setelah reader scan QR dan konfirmasi mulai.
            // (btnStartReading tidak diubah di sini agar tidak override state dari dialog)
        } else {
            binding.tvNextCustomerName.text = "Belum Ada Antrean"
            binding.tvNextBookingTime.text  = "--:--"
            binding.tvNextBookingDate.text  = ""
            binding.tvNextPackageName.text  = "Siap melayani sesi baru"
            binding.btnStartReading.visibility = View.GONE
        }
        cNext.close()
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
                        try { db.execSQL("UPDATE users SET is_online = 0 WHERE id = ?", arrayOf(readerId.toString())) } catch (_: Exception) {}
                        val intent = android.content.Intent(this, MainActivity::class.java)
                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
                true
            }
            R.id.menu_music -> {
                val nowMuted = MusicManager.toggleMute()
                item.title = if (nowMuted) " Musik OFF" else " Musik ON"
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