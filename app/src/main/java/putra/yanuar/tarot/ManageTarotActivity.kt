package putra.yanuar.tarot

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import putra.yanuar.tarot.databinding.ActivityManageTarotBinding

class ManageTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityManageTarotBinding
    val listData = ArrayList<HashMap<String, String>>()

    private val BASE_URL = "http://10.114.14.139:8000/api/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnAddTarot.setOnClickListener {
            startActivity(Intent(this, AddTarotActivity::class.java))
        }

        loadTarotList()
    }

    fun loadTarotList() {

        val queue = Volley.newRequestQueue(this)

        val request = JsonArrayRequest(
            Request.Method.GET,
            BASE_URL + "packages",
            null,
            { response ->

                listData.clear()

                for (i in 0 until response.length()) {
                    val obj: JSONObject = response.getJSONObject(i)

                    val map = HashMap<String, String>()
                    map["id"] = obj.getInt("id").toString()
                    map["name"] = obj.getString("name")
                    map["desc"] = obj.getString("category")
                    map["price"] = "Rp" + obj.getInt("price")

                    listData.add(map)
                }

                val adapter = object : SimpleAdapter(
                    this,
                    listData,
                    R.layout.item_tarot,
                    arrayOf("name", "desc", "price"),
                    intArrayOf(
                        R.id.tvTarotName,
                        R.id.tvTarotDesc,
                        R.id.tvTarotPrice
                    )
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup?
                    ): View {

                        val view = super.getView(position, convertView, parent)

                        val btnMenu =
                            view.findViewById<ImageButton>(R.id.btnMenuTarot)

                        val tarotId =
                            listData[position]["id"] ?: ""

                        val tarotName =
                            listData[position]["name"] ?: ""

                        btnMenu.setOnClickListener {
                            showPopupMenu(it, tarotId, tarotName)
                        }

                        return view
                    }
                }

                b.lvTarot.adapter = adapter
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

    fun showPopupMenu(view: View, id: String, name: String) {

        val popup = PopupMenu(this, view)

        popup.menuInflater.inflate(
            R.menu.menu_data_tarot,
            popup.menu
        )

        popup.setOnMenuItemClickListener {

            when (it.itemId) {

                R.id.menu_edit -> {

                    val intent =
                        Intent(this, EditTarotActivity::class.java)

                    intent.putExtra("TAROT_ID", id)

                    startActivity(intent)
                }

                R.id.menu_delete -> {
                    confirmDelete(id, name)
                }
            }

            true
        }

        popup.show()
    }

    fun confirmDelete(id: String, name: String) {

        AlertDialog.Builder(this)
            .setTitle("Hapus Paket")
            .setMessage("Hapus $name ?")
            .setPositiveButton("Ya") { _, _ ->

                val queue = Volley.newRequestQueue(this)

                val request = StringRequest(
                    Request.Method.DELETE,
                    BASE_URL + "packages/$id",
                    {
                        Toast.makeText(
                            this,
                            "Berhasil dihapus",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadTarotList()
                    },
                    {
                        Toast.makeText(
                            this,
                            "Gagal hapus",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                queue.add(request)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadTarotList()
    }
}