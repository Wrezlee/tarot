package putra.yanuar.tarot

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import putra.yanuar.tarot.databinding.ActivityEditTarotBinding

class EditTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityEditTarotBinding
    var tarotId: String = ""

    private val BASE_URL = "http://10.114.14.139:8000/api/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityEditTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        tarotId = intent.getStringExtra("TAROT_ID") ?: ""

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

        if (tarotId.isNotEmpty()) {
            loadData()
        }

        b.btnSaveTarot.setOnClickListener {
            updateData()
        }
    }

    private fun loadData() {

        val queue = Volley.newRequestQueue(this)

        val request = StringRequest(
            Request.Method.GET,
            BASE_URL + "packages/$tarotId",

            { response ->

                val obj = JSONObject(response)

                b.etTarotName.setText(
                    obj.getString("name")
                )

                b.actvCategory.setText(
                    obj.getString("category"),
                    false
                )

                b.etTarotPrice.setText(
                    obj.getInt("price").toString()
                )
            },

            {
                Toast.makeText(
                    this,
                    "Gagal load data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        queue.add(request)
    }

    private fun updateData() {

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
                "Lengkapi semua kolom",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val queue = Volley.newRequestQueue(this)

        val request = object : StringRequest(
            Method.PUT,
            BASE_URL + "packages/$tarotId",

            {
                Toast.makeText(
                    this,
                    "Paket berhasil diperbarui ",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            },

            {
                Toast.makeText(
                    this,
                    "Gagal update",
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