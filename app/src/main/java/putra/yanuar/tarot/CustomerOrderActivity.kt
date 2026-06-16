package putra.yanuar.tarot

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
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
import putra.yanuar.tarot.databinding.ActivityCustomerOrderBinding
import java.io.File
import java.io.FileOutputStream
import java.util.*

class CustomerOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerOrderBinding
    private lateinit var db: SQLiteDatabase

    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var selectedReaderId: String = ""
    private var selectedReaderName: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""
    private var userEmail: String = ""

    data class TarotPackageLocal(
        val id: String,
        val name: String,
        val category: String,
        val price: Int,
        val isOffline: Boolean
    )

    private var packageList: List<TarotPackageLocal> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = DBOpenHelper(this).writableDatabase

        userEmail          = intent.getStringExtra("USER_EMAIL")   ?: ""
        selectedReaderId   = intent.getStringExtra("READER_ID")?.toString()   ?: ""
        selectedReaderName = intent.getStringExtra("READER_NAME")  ?: ""
        selectedDate       = intent.getStringExtra("SELECTED_DATE") ?: ""

        if (userEmail.isNotEmpty()) {
            val c = db.rawQuery("SELECT id, name FROM users WHERE email = ?", arrayOf(userEmail))
            if (c.moveToFirst()) {
                currentUserId   = c.getInt(0).toString()
                currentUserName = c.getString(1) ?: ""
            }
            c.close()
        }

        if (selectedReaderName.isNotEmpty()) {
            binding.tvSelectedReader.visibility = View.VISIBLE
            binding.tvSelectedReader.text = "Reader: $selectedReaderName"
        }

        if (selectedDate.isNotEmpty()) {
            binding.btnSetDate.text = selectedDate
        }

        setupPackageSpinner()
        setupDateTimePicker()
        setupConfirmButton()

        binding.btnLihatLokasiReader.setOnClickListener {
            openReaderLocation()
        }
    }

    private fun setupPackageSpinner() {
        val list = mutableListOf<TarotPackageLocal>()
        try {
            val c = db.rawQuery("SELECT id, name, category, price, is_offline FROM tarot_packages ORDER BY name ASC", null)
            while (c.moveToNext()) {
                list.add(TarotPackageLocal(
                    id        = c.getInt(0).toString(),
                    name      = c.getString(1) ?: "",
                    category  = c.getString(2) ?: "",
                    price     = c.getInt(3),
                    isOffline = c.getInt(4) == 1
                ))
            }
            c.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        packageList = list

        val names = list.map { "${it.name}  —  Rp${it.price}" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spPaket.adapter = adapter

        binding.spPaket.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateLokasiButtonVisibility(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (list.isNotEmpty()) updateLokasiButtonVisibility(0)
    }

    private fun updateLokasiButtonVisibility(position: Int) {
        val pkg = packageList.getOrNull(position)
        val show = pkg?.isOffline == true && selectedReaderId.isNotEmpty()
        binding.btnLihatLokasiReader.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun openReaderLocation() {
        if (selectedReaderId.isEmpty()) {
            Toast.makeText(this, "Pilih reader terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        val c = db.rawQuery("SELECT name, lat, lng FROM users WHERE id = ?", arrayOf(selectedReaderId))
        if (c.moveToFirst()) {
            val name = c.getString(0) ?: selectedReaderName
            val lat  = c.getDouble(1)
            val lng  = c.getDouble(2)
            c.close()
            if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(this, "Reader belum membagikan lokasi", Toast.LENGTH_SHORT).show()
            } else {
                val i = Intent(this, ReaderLocationActivity::class.java)
                i.putExtra("READER_NAME", name)
                i.putExtra("LAT", lat)
                i.putExtra("LNG", lng)
                startActivity(i)
            }
        } else {
            c.close()
            Toast.makeText(this, "Data reader tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDateTimePicker() {
        binding.btnSetDate.setOnClickListener {
            val cal = Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    selectedDate = "%d/%d/%d".format(d, m + 1, y)
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

    private fun setupConfirmButton() {
        binding.btnConfirmOrder.setOnClickListener {
            saveOrder()
        }
    }

    private fun saveOrder() {
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

        val payment = when (binding.rgPayment.checkedRadioButtonId) {
            R.id.rbTransfer -> "Transfer"
            R.id.rbShopee   -> "Dana/ShopeePay"
            else             -> "Transfer"
        }

        var totalPrice = pkg.price
        if (binding.cbOracle.isChecked)    totalPrice += 10000
        if (binding.cbFastTrack.isChecked) totalPrice += 30000

        val notes = binding.etNotes.text.toString().trim()

        try {
            db.execSQL(
                """INSERT INTO bookings
                   (user_id, reader_id, reader_name, package_name, booking_date, booking_time,
                    email, payment_method, status, total_price, notes)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'pending', ?, ?)""",
                arrayOf(
                    currentUserId,
                    selectedReaderId.ifEmpty { "0" },
                    selectedReaderName,
                    pkg.name,
                    selectedDate,
                    selectedTime,
                    userEmail,
                    payment,
                    totalPrice.toString(),
                    notes
                )
            )

            val cursor = db.rawQuery("SELECT last_insert_rowid()", null)
            var newId = "0"
            if (cursor.moveToFirst()) newId = cursor.getLong(0).toString()
            cursor.close()

            if (newId == "0") {
                Toast.makeText(this, "Gagal menyimpan pesanan", Toast.LENGTH_SHORT).show()
                return
            }

            val qrContent = QrHelper.buildQrContent(
                bookingId    = newId,
                customerId   = currentUserId,
                customerName = currentUserName,
                packageName  = pkg.name,
                date         = selectedDate,
                time         = selectedTime
            )

            try {
                db.execSQL("ALTER TABLE bookings ADD COLUMN qr_content TEXT DEFAULT ''")
            } catch (_: Exception) {}

            db.execSQL(
                "UPDATE bookings SET qr_content = ? WHERE id = ?",
                arrayOf(qrContent, newId)
            )

            showQrTicketDialog(newId, pkg.name, qrContent)

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showQrTicketDialog(bookingId: String, packageName: String, qrContent: String) {
        val qrBitmap = QrHelper.generateQr(qrContent, sizePx = 600)

        val dialogBinding = putra.yanuar.tarot.databinding.DialogQrTicketBinding
            .inflate(layoutInflater)

        dialogBinding.imgDialogQr.setImageBitmap(qrBitmap)
        dialogBinding.tvDialogBookingId.text = "#$bookingId"
        dialogBinding.tvDialogPackage.text   = packageName
        dialogBinding.tvDialogSchedule.text  = "$selectedDate  $selectedTime"

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setTitle("🎉 Pesanan Berhasil!")
            .setCancelable(false)
            .setPositiveButton("Simpan ke Galeri") { dialog, _ ->
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

    private fun saveQrToGallery(bitmap: Bitmap, bookingId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingQrBitmap  = bitmap
            pendingBookingId = bookingId
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_STORAGE)
            return
        }
        doSaveQr(bitmap, bookingId)
    }

    private var pendingQrBitmap:  Bitmap? = null
    private var pendingBookingId: String  = ""

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
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
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TarotMeow")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                    ?: throw Exception("Gagal membuat URI")
                contentResolver.openOutputStream(uri)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TarotMeow")
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