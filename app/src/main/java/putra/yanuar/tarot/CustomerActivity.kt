package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.navigation.NavigationBarView
import putra.yanuar.tarot.databinding.ActivityCustomerBinding
import java.util.Calendar

class CustomerActivity : AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener {

    lateinit var b: ActivityCustomerBinding
    lateinit var db: SQLiteDatabase
    lateinit var ft: FragmentTransaction
    lateinit var fragBooking: CustomerBookingFragment
    lateinit var fragProfile: CustomerProfileFragment
    lateinit var userEmail: String

    fun getDbObject(): SQLiteDatabase = db

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityCustomerBinding.inflate(layoutInflater)
        setContentView(b.root)

        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        fragBooking = CustomerBookingFragment()
        fragProfile = CustomerProfileFragment()
        db = DBOpenHelper(this).writableDatabase

        setSupportActionBar(b.toolbarCustomer)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.title = "Tarot Meow"

        b.navbarCustomer.setOnItemSelectedListener(this)

        setupDates()
        loadReaders()
        updateBookingBadge()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_cust_profil, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_cust_about -> { showAboutDialog(); true }
            R.id.menu_cust_logout -> { logout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔮 Tarot Meow")
            .setMessage(
                "Versi: 1.0\n\n" +
                        "Tarot Meow adalah aplikasi layanan pembacaan tarot profesional yang " +
                        "menghubungkan pelanggan dengan reader berpengalaman.\n\n" +
                        "Fitur Customer:\n" +
                        "• Lihat jadwal & pilih reader online\n" +
                        "• Pesan paket ritual tarot favoritmu\n" +
                        "• Booking via tanggal (Hari ini, Besok, Lusa)\n" +
                        "• Riwayat semua sesi ramalan\n" +
                        "• Tulis ulasan setelah sesi selesai\n\n" +
                        "Kontak: +62 856-4947-1086 (WhatsApp)\n" +
                        "TikTok: @tarotmeow111\n\n" +
                        "Dikembangkan oleh Kelompok 1 PSI\n" +
                        "Fateema Az Zahra                     (243107030134)\n" +
                        "Maharani Citra Dwi Syahputri  (243107030140)\n" +
                        "Moch. Yanuar Putra Wibowo   (243107030146)\n" +
                        "Muh. Muafan Al Farisi              (243107030079)\n" +
                        "© 2026 Tarot Meow. All rights reserved."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun logout() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun updateBookingBadge() {
        try {
            var userId = 0
            val c = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(userEmail))
            if (c.moveToFirst()) userId = c.getInt(0)
            c.close()

            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE user_id = ? AND status IN ('paid','PAID','pending','PENDING','processing','PROCESSING')",
                arrayOf(userId.toString())
            )
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            val badge = b.navbarCustomer.getOrCreateBadge(R.id.itemBooking)
            if (count > 0) {
                badge.isVisible = true
                badge.number = count
            } else {
                badge.isVisible = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDates() {
        val cal = Calendar.getInstance()
        val monthNames = arrayOf("Jan","Feb","Mar","Apr","Mei","Jun","Jul","Agu","Sep","Okt","Nov","Des")

        b.tvDay0.text   = cal.get(Calendar.DAY_OF_MONTH).toString()
        b.tvMonth0.text = monthNames[cal.get(Calendar.MONTH)]
        b.tvLabel0.text = "HARI INI"

        val cal1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
        b.tvDay1.text   = cal1.get(Calendar.DAY_OF_MONTH).toString()
        b.tvMonth1.text = monthNames[cal1.get(Calendar.MONTH)]
        b.tvLabel1.text = "BESOK"

        val cal2 = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 2) }
        b.tvDay2.text   = cal2.get(Calendar.DAY_OF_MONTH).toString()
        b.tvMonth2.text = monthNames[cal2.get(Calendar.MONTH)]
        b.tvLabel2.text = "LUSA"

        val dateCards  = listOf(b.cardDate0, b.cardDate1, b.cardDate2)
        val dateDays   = listOf(cal.get(Calendar.DAY_OF_MONTH), cal1.get(Calendar.DAY_OF_MONTH), cal2.get(Calendar.DAY_OF_MONTH))
        val dateMonths = listOf(cal.get(Calendar.MONTH) + 1, cal1.get(Calendar.MONTH) + 1, cal2.get(Calendar.MONTH) + 1)
        val dateYears  = listOf(cal.get(Calendar.YEAR), cal1.get(Calendar.YEAR), cal2.get(Calendar.YEAR))

        dateCards.forEachIndexed { i, card ->
            card.setOnClickListener {
                val dateStr = "${dateDays[i]}/${dateMonths[i]}/${dateYears[i]}"
                val intent = Intent(this, CustomerOrderActivity::class.java)
                intent.putExtra("USER_EMAIL", userEmail)
                intent.putExtra("SELECTED_DATE", dateStr)
                startActivity(intent)
            }
        }
    }

    private fun loadReaders() {
        val container = b.containerReaders
        container.removeAllViews()

        try {
            val cursor = db.rawQuery(
                "SELECT id, name, email, is_online FROM users WHERE role = 'reader'", null
            )

            if (!cursor.moveToFirst()) {
                cursor.close()
                val tv = TextView(this)
                tv.text = "Belum ada reader tersedia"
                tv.setTextColor(0xFFAD88C6.toInt())
                tv.textSize = 13f
                container.addView(tv)
                return
            }

            do {
                val readerId    = cursor.getInt(0)
                val readerName  = cursor.getString(1)
                val readerEmail = cursor.getString(2)
                val isOnline    = cursor.getInt(3) == 1

                val card = MaterialCardView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.bottomMargin = 16.dpToPx() }
                    radius = 24f.dpToFloat()
                    cardElevation = 4f
                    strokeColor = if (isOnline) 0xFFE1AFD1.toInt() else 0xFFCCCCCC.toInt()
                    strokeWidth = 3
                    setCardBackgroundColor(if (isOnline) 0xFFFFFFFF.toInt() else 0xFFF5F5F5.toInt())
                    isClickable = isOnline
                    isFocusable = isOnline
                    if (isOnline) {
                        val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
                        val rippleDrawable = typedArray.getDrawable(0)
                        typedArray.recycle()
                        foreground = rippleDrawable
                    }
                }

                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(20.dpToPx(), 20.dpToPx(), 20.dpToPx(), 20.dpToPx())
                    alpha = if (isOnline) 1.0f else 0.5f
                }

                val avatar = ShapeableImageView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(90.dpToPx(), 90.dpToPx())
                    setImageResource(R.drawable.meow)
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                        .setAllCornerSizes(20f.dpToFloat())
                        .build()
                }

                val infoCol = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        it.marginStart = 16.dpToPx()
                        it.gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    gravity = android.view.Gravity.CENTER_VERTICAL
                }

                val tvName = TextView(this).apply {
                    text = readerName
                    setTextColor(if (isOnline) 0xFF7469B6.toInt() else 0xFF999999.toInt())
                    textSize = 18f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }

                val tvRole = TextView(this).apply {
                    text = "MYSTIC READER"
                    setTextColor(if (isOnline) 0xFFAD88C6.toInt() else 0xFFBBBBBB.toInt())
                    textSize = 10f
                    letterSpacing = 0.1f
                }

                val bookingCursor = db.rawQuery(
                    "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
                    arrayOf(readerId.toString())
                )
                var totalDone = 0
                if (bookingCursor.moveToFirst()) totalDone = bookingCursor.getInt(0)
                bookingCursor.close()

                val tvStats = TextView(this).apply {
                    text = "✨ $totalDone reading selesai"
                    setTextColor(if (isOnline) 0xFFAD88C6.toInt() else 0xFFBBBBBB.toInt())
                    textSize = 11f
                    setPadding(0, 4.dpToPx(), 0, 0)
                }

                val tvStatus = TextView(this).apply {
                    text = if (isOnline) "ONLINE" else "OFFLINE"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 9f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setPadding(10.dpToPx(), 4.dpToPx(), 10.dpToPx(), 4.dpToPx())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = 8.dpToPx() }
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(if (isOnline) 0xFF4CAF50.toInt() else 0xFF9E9E9E.toInt())
                        cornerRadius = 100f
                    }
                }

                infoCol.addView(tvName)
                infoCol.addView(tvRole)
                infoCol.addView(tvStats)
                infoCol.addView(tvStatus)
                row.addView(avatar)
                row.addView(infoCol)
                card.addView(row)
                container.addView(card)

                if (isOnline) {
                    card.setOnClickListener {
                        val intent = Intent(this, CustomerOrderActivity::class.java)
                        intent.putExtra("USER_EMAIL", userEmail)
                        intent.putExtra("READER_NAME", readerName)
                        intent.putExtra("READER_ID", readerId)
                        startActivity(intent)
                    }
                }

            } while (cursor.moveToNext())
            cursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
    private fun Float.dpToFloat(): Float = this * resources.displayMetrics.density

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        ft = supportFragmentManager.beginTransaction()
        when (p0.itemId) {
            R.id.itemHome -> {
                b.nestedScrollView.visibility = View.VISIBLE
                b.container.visibility = View.GONE
            }
            R.id.itemBooking -> {
                ft.replace(R.id.container, fragBooking)
                ft.commit()
                b.nestedScrollView.visibility = View.GONE
                b.container.visibility = View.VISIBLE
            }
            R.id.itemProfile -> {
                ft.replace(R.id.container, fragProfile)
                ft.commit()
                b.nestedScrollView.visibility = View.GONE
                b.container.visibility = View.VISIBLE
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        loadReaders()
        updateBookingBadge()
    }
}