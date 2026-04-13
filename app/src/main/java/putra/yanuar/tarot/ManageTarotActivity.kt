package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityManageTarotBinding

class ManageTarotActivity : AppCompatActivity() {

    lateinit var b: ActivityManageTarotBinding
    lateinit var db: SQLiteDatabase
    val listData = ArrayList<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageTarotBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Inisialisasi Database
        db = DBOpenHelper(this).writableDatabase

        // Menuju form tambah (Sudah dihubungkan ke AddTarotActivity)
        b.btnAddTarot.setOnClickListener {
            val intent = Intent(this, AddTarotActivity::class.java)
            startActivity(intent)
        }

        // Memuat data pertama kali
        loadTarotList()
    }

    fun loadTarotList() {
        listData.clear()
        // Ambil data paket dari tabel tarot_packages
        val cursor = db.rawQuery("SELECT id, name, description, price FROM tarot_packages", null)

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"] = cursor.getInt(0).toString()
                map["name"] = cursor.getString(1)
                map["desc"] = cursor.getString(2)
                map["price"] = "Rp" + cursor.getInt(3).toString()
                listData.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()

        val adapter = object : SimpleAdapter(
            this,
            listData,
            R.layout.item_tarot,
            arrayOf("name", "desc", "price"),
            intArrayOf(R.id.tvTarotName, R.id.tvTarotDesc, R.id.tvTarotPrice)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)

                // Ambil tombol titik tiga dari layout item_tarot.xml
                val btnMenu = view.findViewById<ImageButton>(R.id.btnMenuTarot)

                val tarotId = listData[position]["id"] ?: ""
                val tarotName = listData[position]["name"] ?: ""

                btnMenu.setOnClickListener { v ->
                    showPopupMenu(v, tarotId, tarotName)
                }
                return view
            }
        }
        b.lvTarot.adapter = adapter
    }

    // FUNGSI POPUP MENU (Menggunakan menu_data_tarot.xml)
    fun showPopupMenu(view: View, id: String, name: String) {
        val popup = PopupMenu(this, view)

        // Inflate dari file XML menu yang sudah kamu buat
        popup.menuInflater.inflate(R.menu.menu_data_tarot, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit -> {
                    // Navigasi ke Activity Edit Paket
                    val intent = Intent(this, EditTarotActivity::class.java)
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

    // KONFIRMASI HAPUS
    fun confirmDelete(id: String, name: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Paket")
            .setMessage("Apakah Anda yakin ingin menghapus paket ritual '$name'?")
            .setIcon(android.R.drawable.ic_menu_delete)
            .setPositiveButton("Ya, Hapus") { _, _ ->
                try {
                    db.execSQL("DELETE FROM tarot_packages WHERE id = ?", arrayOf(id))
                    Toast.makeText(this, "Paket $name berhasil dihapus ✨", Toast.LENGTH_SHORT).show()
                    loadTarotList() // Refresh data list
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create()

        dialog.show()
        // Mengubah warna tombol "Ya, Hapus" menjadi merah (High Contrast)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED)
    }

    override fun onResume() {
        super.onResume()
        // Refresh otomatis saat kembali dari AddTarotActivity atau EditTarotActivity
        loadTarotList()
    }
}