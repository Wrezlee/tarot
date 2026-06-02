package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import putra.yanuar.tarot.databinding.ActivityManageBookingBinding
import putra.yanuar.tarot.databinding.ItemBookingBinding

class ManageBookingActivity : AppCompatActivity() {

    lateinit var b: ActivityManageBookingBinding
    lateinit var db: SQLiteDatabase

    val listData     = ArrayList<BookingItem>()
    val listDataFull = ArrayList<BookingItem>()
    var currentStatus = "Semua"
    var currentQuery  = ""

    data class BookingItem(
        val id: Int,
        val customerName: String,
        val email: String,
        val packageName: String,
        val readerName: String,
        val date: String,
        val time: String,
        val status: String,
        val totalPrice: Int,
        val payment: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageBookingBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        loadBookings()
    }

    fun loadBookings() {
        listData.clear()
        listDataFull.clear()

        try {
            val cursor = db.rawQuery(
                """SELECT b.id, u.name, b.email, b.package_name, b.reader_name,
                          b.booking_date, b.booking_time, b.status, b.total_price, b.payment_method
                   FROM bookings b
                   LEFT JOIN users u ON b.email = u.email
                   ORDER BY b.id DESC""",
                null
            )

            while (cursor.moveToNext()) {
                listData.add(
                    BookingItem(
                        id           = cursor.getInt(0),
                        customerName = cursor.getString(1)?.takeIf { it.isNotEmpty() } ?: cursor.getString(2) ?: "-",
                        email        = cursor.getString(2) ?: "-",
                        packageName  = cursor.getString(3) ?: "-",
                        readerName   = cursor.getString(4)?.takeIf { it.isNotEmpty() } ?: "-",
                        date         = cursor.getString(5) ?: "-",
                        time         = cursor.getString(6) ?: "--:--",
                        status       = (cursor.getString(7) ?: "pending").uppercase(),
                        totalPrice   = cursor.getInt(8),
                        payment      = cursor.getString(9) ?: "-"
                    )
                )
            }
            cursor.close()

            listDataFull.addAll(listData)
            b.lvBooking.adapter = BookingAdapter()
            setupFilter()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun setupFilter() {
        val statusList = arrayOf("Semua","PENDING","PAID","PROCESSING","COMPLETED","CANCELLED")
        val spinnerAdapter = ArrayAdapter(this, R.layout.item_spinner, statusList)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        b.spinnerBookingStatus.adapter = spinnerAdapter

        b.spinnerBookingStatus.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentStatus = statusList[pos]
                applyFilter()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        b.searchViewBooking.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText?.trim() ?: ""
                applyFilter()
                return true
            }
        })
    }

    fun applyFilter() {
        listData.clear()
        for (item in listDataFull) {
            val matchQuery  = currentQuery.isEmpty() ||
                    item.customerName.contains(currentQuery, ignoreCase = true) ||
                    item.packageName.contains(currentQuery, ignoreCase = true)
            val matchStatus = currentStatus == "Semua" || item.status == currentStatus
            if (matchQuery && matchStatus) listData.add(item)
        }
        (b.lvBooking.adapter as? BookingAdapter)?.notifyDataSetChanged()
    }

    fun showChangeStatusDialog(item: BookingItem) {
        val statusOptions = arrayOf("pending","paid","processing","completed","cancelled")
        val displayOptions = arrayOf("PENDING","PAID","PROCESSING","COMPLETED","CANCELLED")
        val currentIdx = statusOptions.indexOfFirst { it.equals(item.status, ignoreCase = true) }.coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Ubah Status Booking #${item.id}")
            .setMessage("Customer: ${item.customerName}\nPaket: ${item.packageName}")
            .setSingleChoiceItems(displayOptions, currentIdx) { dialog, which ->
                try {
                    db.execSQL(
                        "UPDATE bookings SET status = ? WHERE id = ?",
                        arrayOf(statusOptions[which], item.id.toString())
                    )
                    Toast.makeText(this, "Status diubah ke ${displayOptions[which]}", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadBookings()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    inner class BookingAdapter : BaseAdapter() {
        override fun getCount(): Int = listData.size
        override fun getItem(pos: Int): BookingItem = listData[pos]
        override fun getItemId(pos: Int): Long = listData[pos].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val binding: ItemBookingBinding
            val view: View

            if (convertView == null) {
                binding = ItemBookingBinding.inflate(LayoutInflater.from(this@ManageBookingActivity), parent, false)
                view = binding.root
                view.tag = binding
            } else {
                binding = convertView.tag as ItemBookingBinding
                view = convertView
            }

            binding.tvBookingCustomer.text = item.customerName
            binding.tvBookingPackage.text  = item.packageName
            binding.tvBookingDate.text     = "📅 ${item.date}  ⏰ ${item.time}"
            binding.tvBookingReader.text   = "Reader: ${item.readerName}"
            binding.tvBookingPayment.text  = "💳 ${item.payment}"
            binding.tvBookingPrice.text    = "Rp${item.totalPrice}"
            binding.tvBookingStatus.text   = item.status

            val statusColor = when (item.status) {
                "COMPLETED"  -> 0xFF4CAF50.toInt()
                "PAID"       -> 0xFF2196F3.toInt()
                "PROCESSING" -> 0xFFFF9800.toInt()
                "CANCELLED"  -> 0xFFE57373.toInt()
                else         -> 0xFFAD88C6.toInt()
            }
            binding.tvBookingStatus.setTextColor(statusColor)

            binding.btnUbahStatus.setOnClickListener { showChangeStatusDialog(item) }

            return view
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings()
    }
}