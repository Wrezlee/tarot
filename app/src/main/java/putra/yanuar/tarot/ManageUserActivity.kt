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
import putra.yanuar.tarot.databinding.ActivityManageUserBinding

class ManageUserActivity : AppCompatActivity() {

    lateinit var b: ActivityManageUserBinding
    lateinit var db: SQLiteDatabase
    val listData = ArrayList<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Inisialisasi Database
        db = DBOpenHelper(this).writableDatabase

        // Navigasi ke Activity Tambah User
        b.btnAddUser.setOnClickListener {
            val intent = Intent(this, AddUserActivity::class.java)
            startActivity(intent)
        }

        // Memuat data pertama kali
        loadUserList()
    }

    fun loadUserList() {
        listData.clear()
        // Ambil data dari tabel users
        val cursor = db.rawQuery("SELECT id, name, email, role FROM users", null)

        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"] = cursor.getInt(0).toString()
                map["name"] = cursor.getString(1)
                map["email"] = cursor.getString(2)
                map["role"] = cursor.getString(3).uppercase()
                listData.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()

        // Adapter untuk menampilkan data ke ListView
        val adapter = object : SimpleAdapter(
            this,
            listData,
            R.layout.item_user,
            arrayOf("name", "email", "role"),
            intArrayOf(R.id.tvUserName, R.id.tvUserEmail, R.id.tvUserRole)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
                val view = super.getView(position, convertView, parent)

                // Tombol titik tiga di dalam item_user.xml
                val btnMenu = view.findViewById<ImageButton>(R.id.btnMenuOption)

                val userId = listData[position]["id"] ?: ""
                val userName = listData[position]["name"] ?: ""

                // Event klik pada titik tiga
                btnMenu.setOnClickListener { v ->
                    showPopupMenu(v, userId, userName)
                }

                return view
            }
        }
        b.lvUser.adapter = adapter
    }

    // FUNGSI POPUP MENU (Menggunakan menu_data_user.xml)
    fun showPopupMenu(view: View, userId: String, userName: String) {
        val popup = PopupMenu(this, view)

        // Inflate menu dari res/menu/menu_data_user.xml
        popup.menuInflater.inflate(R.menu.menu_data_user, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            // Kita cek berdasarkan judul menu (atau ID jika kamu sudah menambahkannya di XML)
            when (item.title) {
                // Di dalam showPopupMenu ManageUserActivity.kt
                "Edit Data" -> {
                    val i = Intent(this, EditUserActivity::class.java)
                    i.putExtra("USER_ID", userId) // Kirim ID ke Activity Edit
                    startActivity(i)
                }
                "Hapus User" -> {
                    confirmDelete(userId, userName)
                }
            }
            true
        }
        popup.show()
    }

    // KONFIRMASI HAPUS
    fun confirmDelete(id: String, name: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus Petugas")
            .setMessage("Apakah Anda yakin ingin menghapus '$name'?")
            .setIcon(android.R.drawable.ic_menu_delete)
            .setPositiveButton("Ya, Hapus") { _, _ ->
                try {
                    db.execSQL("DELETE FROM users WHERE id = ?", arrayOf(id))
                    Toast.makeText(this, "$name berhasil dihapus ✨", Toast.LENGTH_SHORT).show()
                    loadUserList()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create() // Gunakan .create() dulu

        dialog.show() // Tampilkan dialog

        // SETELAH show(), kita bisa ubah warna tombolnya jadi merah
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED)

        // Opsional: Ubah warna tombol Batal jadi abu-abu agar tidak dominan
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.GRAY)
    }

    override fun onResume() {
        super.onResume()
        // Refresh otomatis saat kembali dari AddUserActivity
        loadUserList()
    }
}