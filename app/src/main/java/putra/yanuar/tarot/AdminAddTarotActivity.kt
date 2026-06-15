package putra.yanuar.tarot

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import putra.yanuar.tarot.databinding.ActivityAdminAddTarotBinding


class AdminAddTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityAdminAddTarotBinding

    private val BASE_URL = "http://10.114.14.139:8000/api/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityAdminAddTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        val daftarKategori = arrayOf(
            "tarot",
            "chat",
            "call",
            "palm"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            daftarKategori
        )

        b.actvCategory.setAdapter(adapter)

        b.actvCategory.setOnClickListener {
            b.actvCategory.showDropDown()
        }

        b.btnSaveTarot.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {

        val name = b.etTarotName.text.toString().trim()
        val category = b.actvCategory.text.toString().trim()
        val price = b.etTarotPrice.text.toString().trim()

        if (
            name.isEmpty() ||
            category.isEmpty() ||
            price.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Harap lengkapi semua kolom!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Method.POST,
            BASE_URL + "packages",

            {
                Toast.makeText(
                    this,
                    "Paket $name berhasil disimpan ",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            },

            {
                Toast.makeText(
                    this,
                    "Gagal simpan data",
                    Toast.LENGTH_SHORT
                ).show()
            }

        ) {

            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "name" to name,
                    "category" to category,
                    "price" to price
                )
            }
        }

        queue.add(request)
    }
}