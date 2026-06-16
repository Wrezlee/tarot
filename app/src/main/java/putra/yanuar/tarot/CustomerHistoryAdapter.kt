    package putra.yanuar.tarot

    import android.Manifest
    import android.content.ContentValues
    import android.content.Context
    import android.content.pm.PackageManager
    import android.database.sqlite.SQLiteDatabase
    import android.graphics.Bitmap
    import android.os.Build
    import android.os.Environment
    import android.provider.MediaStore
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.*
    import androidx.core.content.ContextCompat
    import com.google.android.material.button.MaterialButton
    import java.io.File
    import java.io.FileOutputStream

    class CustomerHistoryAdapter(
        private val context: Context,
        private val bookings: MutableList<Booking>,
        private val db: SQLiteDatabase,
        private val onCancelClick: (Booking) -> Unit,
        private val onUlasanClick: (Booking) -> Unit
    ) : BaseAdapter() {

        override fun getCount(): Int = bookings.size
        override fun getItem(position: Int): Booking = bookings[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_customer_history, parent, false)

            val booking = bookings[position]

            val tvPackage    = view.findViewById<TextView>(R.id.tvItemPackage)
            val tvStatus     = view.findViewById<TextView>(R.id.tvItemStatus)
            val tvDate       = view.findViewById<TextView>(R.id.tvItemDate)
            val tvReader     = view.findViewById<TextView>(R.id.tvItemReader)
            val tvQuestion   = view.findViewById<TextView>(R.id.tvItemQuestion)
            val tvAnswer     = view.findViewById<TextView>(R.id.tvItemAnswer)
            val btnCancel    = view.findViewById<MaterialButton>(R.id.btnCancelOrder)
            val btnUlasan    = view.findViewById<MaterialButton>(R.id.btnTulisUlasan)
            val btnShare     = view.findViewById<MaterialButton>(R.id.btnShareRamalan)
            val layoutUlasan = view.findViewById<LinearLayout>(R.id.layoutUlasanTerkirim)
            val tvRating     = view.findViewById<TextView>(R.id.tvIsiRating)
            val tvIsiUlasan  = view.findViewById<TextView>(R.id.tvIsiUlasan)

            val layoutQr  = view.findViewById<LinearLayout>(R.id.layoutQrTicket)
            val imgQr     = view.findViewById<ImageView>(R.id.imgQrCode)
            val tvQrId    = view.findViewById<TextView>(R.id.tvQrBookingId)
            val btnSaveQr = view.findViewById<MaterialButton>(R.id.btnSaveQr)

            tvPackage.text  = booking.packageName
            tvDate.text     = " ${booking.date}   ${booking.time}"
            tvReader.text   = if (booking.readerName.isNotEmpty()) "Reader: ${booking.readerName}" else "Reader: -"
            tvQuestion.text = if (booking.notes.isNotEmpty()) "Q: ${booking.notes}" else "Q: -"
            tvAnswer.text   = if (booking.answer.isNotEmpty()) "A: ${booking.answer}" else "A: Belum ada jawaban dari Reader."

            val statusText = booking.   status.uppercase()
            tvStatus.text = statusText
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(
                when (statusText) {
                    "DONE", "COMPLETED" -> android.graphics.Color.parseColor("#E8F5E9")
                    "PROCESSING"        -> android.graphics.Color.parseColor("#E3F2FD")
                    "CANCELLED"         -> android.graphics.Color.parseColor("#FFEBEE")
                    else                -> android.graphics.Color.parseColor("#F3E5F5")
                }
            )
            tvStatus.setTextColor(android.graphics.Color.parseColor(
                when (statusText) {
                    "DONE", "COMPLETED" -> "#388E3C"
                    "PROCESSING"        -> "#1565C0"
                    "CANCELLED"         -> "#D32F2F"
                    else                -> "#AD88C6"
                }
            ))

            // Tombol Batal — hanya untuk PENDING/PAID
            val canCancel = statusText in listOf("PENDING", "PAID")
            btnCancel.visibility = if (canCancel) View.VISIBLE else View.GONE
            if (canCancel) btnCancel.setOnClickListener { onCancelClick(booking) }

            val isDone = statusText in listOf("DONE", "COMPLETED")

            // Cek testimoni dari DB lokal
            val testi = getTestimoniByBookingId(booking.id)

            if (isDone) {
                if (testi == null) {
                    btnUlasan.visibility    = View.VISIBLE
                    layoutUlasan.visibility = View.GONE
                    btnUlasan.setOnClickListener { onUlasanClick(booking) }
                } else {
                    btnUlasan.visibility    = View.GONE
                    layoutUlasan.visibility = View.VISIBLE
                    val stars = "★".repeat(testi["rating"]?.toIntOrNull() ?: 5)
                    tvRating.text    = stars
                    tvIsiUlasan.text = testi["message"] ?: ""
                }
                btnShare.visibility = View.VISIBLE
                btnShare.setOnClickListener { shareRamalan(booking) }
            } else {
                btnUlasan.visibility    = View.GONE
                btnShare.visibility     = View.GONE
                layoutUlasan.visibility = View.GONE
            }

            // QR Ticket — sembunyikan untuk CANCELLED
            if (statusText != "CANCELLED") {
                val qrContent = getQrContent(booking.id)
                val finalQrContent = if (!qrContent.isNullOrEmpty()) {
                    qrContent
                } else {
                    val newQr = QrHelper.buildQrContent(
                        bookingId    = booking.id,
                        customerId   = booking.customerId,
                        customerName = booking.customerName,
                        packageName  = booking.packageName,
                        date         = booking.date,
                        time         = booking.time
                    )
                    saveQrContent(booking.id, newQr)
                    newQr
                }

                layoutQr.visibility = View.VISIBLE
                val bmp = QrHelper.generateQr(finalQrContent, sizePx = 300)
                imgQr.setImageBitmap(bmp)
                tvQrId.text = "Booking #${booking.id}"
                btnSaveQr.setOnClickListener { saveQrToGallery(bmp, booking.id) }
            } else {
                layoutQr.visibility = View.GONE
            }

            return view
        }

        // ── DB helpers — sesuai struktur tabel bookings di DBOpenHelper ──────────

        private fun getQrContent(bookingId: String): String? {
            return try {
                val c = db.rawQuery("SELECT qr_content FROM bookings WHERE id = ?", arrayOf(bookingId))
                c.use { if (it.moveToFirst()) it.getString(0) else null }
            } catch (_: Exception) { null }
        }

        private fun saveQrContent(bookingId: String, qrContent: String) {
            try {
                db.execSQL("UPDATE bookings SET qr_content = ? WHERE id = ?", arrayOf(qrContent, bookingId))
            } catch (_: Exception) {}
        }

        private fun getTestimoniByBookingId(bookingId: String): Map<String, String>? {
            return try {
                val c = db.rawQuery(
                    "SELECT rating, message FROM testimonials WHERE booking_id = ? LIMIT 1",
                    arrayOf(bookingId)
                )
                c.use {
                    if (it.moveToFirst()) mapOf(
                        "rating"  to it.getInt(0).toString(),
                        "message" to (it.getString(1) ?: "")
                    ) else null
                }
            } catch (_: Exception) { null }
        }

        // ── Simpan QR ke galeri ───────────────────────────────────────────────────

        private fun saveQrToGallery(bitmap: Bitmap, bookingId: String) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(context, "Izin storage diperlukan", Toast.LENGTH_SHORT).show()
                return
            }
            try {
                val filename = "TarotMeow_$bookingId.png"
                val fos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val cv = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TarotMeow")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                        ?: throw Exception("Gagal membuat URI")
                    context.contentResolver.openOutputStream(uri)
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TarotMeow")
                    dir.mkdirs()
                    FileOutputStream(File(dir, filename))
                }
                fos?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                Toast.makeText(context, "QR Tiket disimpan ke Galeri 📸", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal simpan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Share ramalan ─────────────────────────────────────────────────────────

        private fun shareRamalan(booking: Booking) {
            val text = "✨ Ramalan Tarot Meow ✨\n" +
                    "Paket  : ${booking.packageName}\n" +
                    "Tanggal: ${booking.date}\n\n" +
                    if (booking.answer.isNotEmpty()) "📜 ${booking.answer}" else "Ramalan belum tersedia."

            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Ramalan"))
        }
    }