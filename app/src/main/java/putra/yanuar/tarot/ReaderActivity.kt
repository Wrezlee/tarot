package putra.yanuar.tarot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import putra.yanuar.tarot.databinding.ActivityReaderBinding
import putra.yanuar.tarot.QrHelper
import putra.yanuar.tarot.Booking
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.putra.yanuar.tarot.DatabaseHelper

class ReaderActivity : AppCompatActivity() {

    // ── ViewBinding ──────────────────────────────────────────────────────────
    private lateinit var binding: ActivityReaderBinding

    // ── Database ─────────────────────────────────────────────────────────────
    private lateinit var db: DatabaseHelper

    // ── State ────────────────────────────────────────────────────────────────
    private var readerId:   String = ""
    private var readerName: String = ""

    // Booking yang sedang aktif / sudah ter-verifikasi QR
    private var verifiedBooking: Booking? = null

    // ── ZXing scan launcher ───────────────────────────────────────────────────
    private val scanLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            handleQrScanResult(result.contents)
        } else {
            Toast.makeText(this, "Scan dibatalkan", Toast.LENGTH_SHORT).show()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        readerId   = intent.getStringExtra("USER_ID")   ?: ""
        readerName = intent.getStringExtra("USER_NAME") ?: ""

        setupToolbar()
        setupBottomNav()
        setupScanButton()
        setupStartReadingButton()
        refreshDashboard()
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarReader)
        supportActionBar?.title = "Dashboard Reader"
    }

    // ── Bottom Navigation ─────────────────────────────────────────────────────
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

    // ── Tombol Scan QR ────────────────────────────────────────────────────────
    private fun setupScanButton() {
        binding.btnScanQr.setOnClickListener {
            checkCameraPermissionThenScan()
        }
    }

    // ── Tombol Mulai Ramalan ─────────────────────────────────────────────────
    private fun setupStartReadingButton() {
        binding.btnStartReading.setOnClickListener {
            val booking = verifiedBooking ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("🔮 Mulai Sesi?")
                .setMessage("Kamu akan memulai ramalan untuk:\n\n" +
                        "Customer : ${booking.customerName}\n" +
                        "Paket    : ${booking.packageName}\n" +
                        "Jadwal   : ${booking.date}  ${booking.time}")
                .setPositiveButton("Mulai Sekarang") { _, _ ->
                    db.updateBookingStatus(booking.id, "ON_PROGRESS")
                    Toast.makeText(this, "Sesi dimulai! 🔮", Toast.LENGTH_SHORT).show()
                    binding.btnStartReading.visibility = View.GONE
                    verifiedBooking = null
                    refreshDashboard()
                }
                .setNegativeButton("Batal", null)
                .show()
        }
    }

    // ── Check Kamera & Launch Scanner ─────────────────────────────────────────
    private fun checkCameraPermissionThenScan() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchScanner()

            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Izin Kamera Dibutuhkan")
                    .setMessage("Untuk scan QR tiket customer, aplikasi butuh akses kamera.")
                    .setPositiveButton("Izinkan") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA
                        )
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            else -> ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA), REQ_CAMERA
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            launchScanner()
        } else {
            Toast.makeText(this, "Izin kamera ditolak ", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Launch ZXing Scanner ──────────────────────────────────────────────────
    private fun launchScanner() {
        val options = ScanOptions().apply {
            setPrompt("Arahkan ke QR Tiket customer 🔮")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setBarcodeImageEnabled(false)
        }
        scanLauncher.launch(options)
    }

    // ── Handle Hasil Scan ─────────────────────────────────────────────────────
    private fun handleQrScanResult(raw: String) {
        val data = QrHelper.parseQrContent(raw)

        if (data == null) {
            AlertDialog.Builder(this)
                .setTitle(" QR Tidak Valid")
                .setMessage("Format QR tidak dikenali. Pastikan customer scan QR dari aplikasi Tarot Meow.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Cek booking di database
        val booking = db.getBookingById(data.bookingId)

        if (booking == null) {
            AlertDialog.Builder(this)
                .setTitle(" Booking Tidak Ditemukan")
                .setMessage("ID Booking #${data.bookingId} tidak ada di sistem.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Cek status booking — hanya PENDING atau PAID yang boleh di-scan
        if (booking.status.uppercase() !in listOf("PENDING", "PAID", "CONFIRMED")) {
            AlertDialog.Builder(this)
                .setTitle("⚠ Status Tidak Valid")
                .setMessage(
                    "Booking ini berstatus: ${booking.status}\n\n" +
                            "Hanya booking PENDING / PAID yang bisa dimulai."
                )
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Verifikasi berhasil — tampilkan detail & konfirmasi
        showVerifiedDialog(booking)
    }

    // ── Dialog Setelah Verifikasi Berhasil ───────────────────────────────────
    private fun showVerifiedDialog(booking: Booking) {
        AlertDialog.Builder(this)
            .setTitle(" Tiket Terverifikasi!")
            .setMessage(
                "Customer  : ${booking.customerName}\n" +
                        "Paket     : ${booking.packageName}\n" +
                        "Jadwal    : ${booking.date}  ${booking.time}\n" +
                        "Bayar     : ${booking.paymentMethod}\n" +
                        "Total     : Rp${"%,d".format(booking.totalPrice)}\n\n" +
                        "Mulai sesi ramalan sekarang?"
            )
            .setPositiveButton("🔮 Mulai Ramalan") { _, _ ->
                // Update status ke ON_PROGRESS langsung dari sini
                db.updateBookingStatus(booking.id, "ON_PROGRESS")
                verifiedBooking = null
                Toast.makeText(this, "Sesi dimulai! 🔮", Toast.LENGTH_SHORT).show()
                refreshDashboard()
            }
            .setNeutralButton("Lihat Detail Dulu") { _, _ ->
                // Simpan booking, tampilkan tombol Mulai Ramalan di card
                verifiedBooking = booking
                updateNextBookingCard(booking)
                binding.btnStartReading.visibility = View.VISIBLE
                Toast.makeText(
                    this,
                    "Tiket valid  Tap 'Mulai Ramalan' saat siap",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ── Refresh Dashboard ─────────────────────────────────────────────────────
    private fun refreshDashboard() {
        // Hitung statistik
        val pending   = db.countBookingsByReaderAndStatus(readerId, "PENDING")
        val onProgress = db.countBookingsByReaderAndStatus(readerId, "ON_PROGRESS")
        val done      = db.countBookingsByReaderAndStatus(readerId, "DONE")

        binding.tvPendingCount.text   = (pending + onProgress).toString()
        binding.tvCompletedCount.text = done.toString()

        // Nama reader di subtitle
        binding.tvReaderGreeting.text  = "Dashboard Reader"
        binding.tvReaderSubtitle.text  = "Halo, $readerName "

        // Next booking (paling awal yang PENDING)
        val nextBooking = db.getNextBookingForReader(readerId)
        if (nextBooking != null) {
            updateNextBookingCard(nextBooking)
        } else {
            binding.tvNextCustomerName.text = "Belum Ada Antrean"
            binding.tvNextBookingTime.text  = "--:--"
            binding.tvNextBookingDate.text  = ""
            binding.tvNextPackageName.text  = "Siap melayani sesi baru"
            binding.btnStartReading.visibility = View.GONE
        }
    }

    private fun updateNextBookingCard(booking: Booking) {
        binding.tvNextCustomerName.text = booking.customerName
        binding.tvNextBookingTime.text  = booking.time
        binding.tvNextBookingDate.text  = " ${booking.date}"
        binding.tvNextPackageName.text  = " ${booking.packageName}"
    }

    // ── Options menu (logout, musik, about) ──────────────────────────────────
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_logout -> {
                AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Yakin ingin keluar?")
                    .setPositiveButton("Ya") { _, _ -> finish() }
                    .setNegativeButton("Batal", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val REQ_CAMERA = 101
    }
}