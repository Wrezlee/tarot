package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentBookingBinding

class BookingFragment : Fragment(), View.OnClickListener {

    lateinit var b: FragmentBookingBinding
    lateinit var thisParent: CustomerActivity
    lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentBookingBinding.inflate(inflater, container, false)

        thisParent = activity as CustomerActivity
        db = thisParent.getDbObject()

        b.btnKonfirmasiWA.setOnClickListener(this)
        b.btnOrderSekarang.setOnClickListener(this)

        loadPackagePrices()

        return b.root
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnKonfirmasiWA -> openWhatsApp()
            R.id.btnOrderSekarang -> {
                val intent = Intent(thisParent, OrderActivity::class.java)
                intent.putExtra("USER_EMAIL", thisParent.userEmail)
                startActivity(intent)
            }
        }
    }

    fun loadPackagePrices() {
        try {
            val cursor = db.rawQuery("SELECT name, price FROM tarot_packages", null)

            if (cursor.moveToFirst()) {
                do {
                    val name = cursor.getString(0)
                    val price = cursor.getInt(1)
                    val formattedPrice = "Rp$price"

                    when (name) {
                        "1 Kartu (1 Pertanyaan)" -> b.tvHarga1K.text = formattedPrice
                        "3 Kartu (1 Pertanyaan)" -> b.tvHarga3K.text = formattedPrice
                        "6 Kartu (2 Pertanyaan)" -> b.tvHarga6K.text = formattedPrice
                        "9 Kartu (Deep Reading)" -> b.tvHargaDeep.text = formattedPrice
                        "Analisis Telapak Tangan" -> b.tvHargaPalm.text = formattedPrice
                        "10 Menit Chat" -> b.tvHargaChat10.text = formattedPrice
                        "15 Menit Chat" -> b.tvHargaChat15.text = formattedPrice
                        "20 Menit Chat" -> b.tvHargaChat20.text = formattedPrice
                        "30 Menit Chat" -> b.tvHargaChat30.text = formattedPrice
                        "30 Menit Call" -> b.tvHargaCall30.text = formattedPrice
                        "1 Jam Call" -> b.tvHargaCall60.text = formattedPrice
                    }
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openWhatsApp() {
        val phoneNumber = "+6285649471086"
        val message = "Halo min, saya ingin konfirmasi order Tarot Meow ✨"

        try {
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(thisParent, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }
}