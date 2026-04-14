package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityManageUserBinding

class ManageUserActivity : AppCompatActivity() {

    lateinit var b: ActivityManageUserBinding
    lateinit var db: SQLiteDatabase
    val listData = ArrayList<HashMap<String, String>>()

    // Simpan item yang sedang dipilih saat long click
    var selectedUserId = ""
    var selectedUserName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase

        b.btnAddUser.setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }

        loadUserList()

        // Daftarkan ListView untuk context menu (long click)
        registerForContextMenu(b.lvUser)
    }

    fun loadUserList() {
        listData.clear()
        val cursor = db.rawQuery("SELECT id, name, email, role FROM users", null)
        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"]    = cursor.getInt(0).toString()
                map["name"]  = cursor.getString(1)
                map["email"] = cursor.getString(2)
                map["role"]  = cursor.getString(3).uppercase()
                listData.add(map)
            } while (cursor.moveToNext())
        }
        cursor.close()

        // SimpleAdapter biasa — tidak perlu override getView lagi
        val adapter = SimpleAdapter(
            this,
            listData,
            R.layout.item_user,
            arrayOf("name", "email", "role"),
            intArrayOf(R.id.tvUserName, R.id.tvUserEmail, R.id.tvUserRole)
        )
        b.lvUser.adapter = adapter
    }

    // ── Context Menu ──────────────────────────────────────────────
    override fun onCreateContextMenu(
        menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menuInflater.inflate(R.menu.context_menu_data_user, menu)

        val info = menuInfo as AdapterView.AdapterContextMenuInfo
        selectedUserId   = listData[info.position]["id"]   ?: ""
        selectedUserName = listData[info.position]["name"] ?: ""
        menu.setHeaderTitle(selectedUserName)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.ctx_edit -> {
                val i = Intent(this, EditUserActivity::class.java)
                i.putExtra("USER_ID", selectedUserId)
                startActivity(i)
                true
            }
            R.id.ctx_delete -> {
                confirmDelete(selectedUserId, selectedUserName)
                true
            }
            else -> super.onContextItemSelected(item)
        }
    }

    // ── Konfirmasi Hapus ──────────────────────────────────────────
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
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.GRAY)
    }

    override fun onResume() {
        super.onResume()
        loadUserList()
    }
}