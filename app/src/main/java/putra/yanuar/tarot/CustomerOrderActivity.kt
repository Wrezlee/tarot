package putra.yanuar.tarot

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.putra.yanuar.tarot.DatabaseHelper
import com.putra.yanuar.tarot.QrHelper

import putra.yanuar.tarot.databinding.ActivityCustomerOrderBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CustomerOrderActivity : AppCompatActivity() {

    // ── ViewBinding ──────────────────────────────────────────────────────────
    private lateinit var binding: ActivityCustomerOrderBinding

    // ── Database ─────────────────────────────────────────────────────────────
    private lateinit var db: DatabaseHelper

    // ── State ────────────────────────────────────────────────────────────────
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedReaderId: String = ""
    private var selectedReaderName: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var packageList: List<com.putra.yanuar.tarot.TarotPackage> = emptyList()

    // ─────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DatabaseHelper(this)

        // Ambil data dari Intent (dikirim oleh CustomerActivity)
        currentUserId   = intent.getStringExtra("USER_ID")   ?: ""
        currentUserName = intent.getStringExtra("USER_NAME")  ?: ""
        selectedReaderId   = intent.getStringExtra("READER_ID")   ?: ""
        selectedReaderName = intent.getStringExtra("READER_NAME") ?: ""

        if (selectedReaderName.isNotEmpty()) {
            binding.tvSelectedReader.visibility = View.VISIBLE
            binding.tvSelectedReader.text = "Reader: $selectedReaderName"
        }

        setupPackageSpinner()
        setupDateTimePicker()
        setupConfirmButton()
    }

    // ── Setup Spinner Paket ───────────────────────────────────────────────────
    private fun setupPackageSpinner() {
        packageList = db.getAllTarotPackages()          // sesuaikan nama method DB-mu
        val names = packageList.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spPaket.adapter = adapter
    }

    // ── Date & Time Picker ────────────────────────────────────────────────────
    private fun setupDateTimePicker() {
        binding.btnSetDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                    binding.btnSetDate.text = selectedDate
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.btnSetTime.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.TimePickerDialog(
                this,
                { _, h, min ->
                    selectedTime = "%02d:%02d".format(h, min)
                    binding.btnSetTime.text = selectedTime
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }
    }

    // ── Tombol Konfirmasi ─────────────────────────────────────────────────────
    private fun setupConfirmButton() {
        binding.btnConfirmOrder.setOnClickListener {
            saveOrder()
        }
    }

    // ── Simpan Order ke Database → Generate QR ────────────────────────────────
    private fun saveOrder() {
        // Validasi input
        val selectedPkgIndex = binding.spPaket.selectedItemPosition
        if (packageList.isEmpty() || selectedPkgIndex < 0) {
            Toast.makeText(this, "Pilih paket terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Pilih tanggal sesi", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Pilih jam sesi", Toast.LENGTH_SHORT).show()
            return
        }

        val pkg = packageList[selectedPkgIndex]

        // Metode pembayaran
        val payment = when (binding.rgPayment.checkedRadioButtonId) {
            R.id.rbTransfer -> "Transfer"
            R.id.rbShopee   -> "Dana/ShopeePay"
            else             -> "Transfer"
        }

        // Layanan tambahan
        var totalPrice = pkg.price
        val addons = mutableListOf<String>()
        if (binding.cbOracle.isChecked)    { totalPrice += 10000; addons.add("Oracle Card") }
        if (binding.cbFastTrack.isChecked) { totalPrice += 30000; addons.add("Fast Track") }

        val notes = binding.etNotes.text.toString().trim()

        // Build objek booking
        val booking = Booking(
            id             = "",                // akan diisi DB (auto-increment atau UUID)
            customerId     = currentUserId,
            customerName   = currentUserName,
            readerId       = selectedReaderId,
            readerName     = selectedReaderName,
            packageId      = pkg.id,
            packageName    = pkg.name,
            paymentMethod  = payment,
            totalPrice     = totalPrice,
            date           = selectedDate,
            time           = selectedTime,
            notes          = notes,
            status         = "PENDING",
            qrContent      = ""                 // akan diisi setelah insert
        )

        // Insert ke database — db.insertBooking harus kembalikan ID (Long / String)
        val newId = db.insertBooking(booking)

        if (newId <= 0L) {
            Toast.makeText(this, "Gagal menyimpan pesanan", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingId = newId.toString()

        // Build & simpan QR content
        val qrContent = QrHelper.buildQrContent(
            bookingId    = bookingId,
            customerId   = currentUserId,
            customerName = currentUserName,
            packageName  = pkg.name,
            date         = selectedDate,
            time         = selectedTime
        )
        db.updateBookingQr(bookingId, qrContent)

        // Tampilkan dialog tiket QR
        showQrTicketDialog(bookingId, pkg.name, qrContent)
    }

    // ── Dialog Tiket QR ───────────────────────────────────────────────────────
    private fun showQrTicketDialog(bookingId: String, packageName: String, qrContent: String) {
        val qrBitmap = QrHelper.generateQr(qrContent, sizePx = 600)

        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_ticket, null)
        dialogView.findViewById<android.widget.ImageView>(R.id.imgDialogQr)
            .setImageBitmap(qrBitmap)
        dialogView.findViewById<android.widget.TextView>(R.id.tvDialogBookingId)
            .text = "#$bookingId"
        dialogView.findViewById<android.widget.TextView>(R.id.tvDialogPackage)
            .text = packageName
        dialogView.findViewById<android.widget.TextView>(R.id.tvDialogSchedule)
            .text = "$selectedDate  $selectedTime"

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("🎉 Pesanan Berhasil!")
            .setCancelable(false)
            .setPositiveButton("💾 Simpan ke Galeri") { dialog, _ ->
                saveQrToGallery(qrBitmap, bookingId)
                dialog.dismiss()
                finish()
            }
            .setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    // ── Simpan QR ke Galeri ───────────────────────────────────────────────────
    private fun saveQrToGallery(bitmap: Bitmap, bookingId: String) {
        // Minta permission jika perlu (Android < Q)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Simpan bitmap di field sementara, lalu minta permission
            pendingQrBitmap   = bitmap
            pendingBookingId  = bookingId
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_STORAGE)
            return
        }
        doSaveQr(bitmap, bookingId)
    }

    private var pendingQrBitmap:  Bitmap? = null
    private var pendingBookingId: String  = ""

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_STORAGE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            pendingQrBitmap?.let { doSaveQr(it, pendingBookingId) }
        } else {
            Toast.makeText(this, "Izin storage ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    private fun doSaveQr(bitmap: Bitmap, bookingId: String) {
        val filename = "TarotMeow_$bookingId.png"
        try {
            val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/TarotMeow"
                    )
                }
                val uri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv
                ) ?: throw Exception("Gagal membuat URI")
                contentResolver.openOutputStream(uri)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "TarotMeow"
                )
                dir.mkdirs()
                FileOutputStream(File(dir, filename))
            }
            fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Toast.makeText(this, "QR disimpan ke Galeri 📸", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQ_STORAGE = 201
    }
}