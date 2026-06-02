package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import putra.yanuar.tarot.databinding.ActivityManageUserBinding

class ManageUserActivity : AppCompatActivity() {

    lateinit var b: ActivityManageUserBinding
    lateinit var db: SQLiteDatabase

    val listData     = ArrayList<HashMap<String, String>>()
    val listDataFull = ArrayList<HashMap<String, String>>()

    var selectedUserId   = ""
    var selectedUserName = ""
    var currentRole      = "Semua"
    var currentQuery     = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityManageUserBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase

        b.btnAddUser.setOnClickListener {
            startActivity(Intent(this, AddUserActivity::class.java))
        }

        loadUserList()
        registerForContextMenu(b.lvUser)
    }

    fun loadUserList() {
        listData.clear()
        listDataFull.clear()

        val cursor = db.rawQuery("SELECT id, name, email, role FROM users", null)
        if (cursor.moveToFirst()) {
            do {
                val map = HashMap<String, String>()
                map["id"]    = cursor.getInt(0).toString()
                map["name"]  = cursor.getString(1)
                map["email"] = cursor.getString(2)
                map["role"]  = cursor.getString(3).uppercase()
                listData.add(map)
                listDataFull.add(HashMap(map))
            } while (cursor.moveToNext())
        }
        cursor.close()

        b.lvUser.adapter = UserAdapter()
        setupFilter()
    }

    fun setupFilter() {
        val roleList = arrayOf("Semua", "ADMIN", "READER", "CUSTOMER")
        val spinnerAdapter = ArrayAdapter(this, R.layout.item_spinner, roleList)
        spinnerAdapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
        b.spinnerRoleFilter.adapter = spinnerAdapter

        b.spinnerRoleFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                currentRole = roleList[pos]
                applyFilter()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        b.searchViewUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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
            val matchQuery = currentQuery.isEmpty() ||
                    (item["name"] ?: "").contains(currentQuery, ignoreCase = true) ||
                    (item["email"] ?: "").contains(currentQuery, ignoreCase = true)
            val matchRole = currentRole == "Semua" ||
                    (item["role"] ?: "").equals(currentRole, ignoreCase = true)
            if (matchQuery && matchRole) listData.add(item)
        }
        (b.lvUser.adapter as? UserAdapter)?.notifyDataSetChanged()
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenu.ContextMenuInfo?) {
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

    fun confirmDelete(id: String, name: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Hapus User")
            .setMessage("Apakah Anda yakin ingin menghapus '$name'?")
            .setIcon(android.R.drawable.ic_menu_delete)
            .setPositiveButton("Ya, Hapus") { _, _ ->
                try {
                    db.execSQL("DELETE FROM users WHERE id = ?", arrayOf(id))
                    Toast.makeText(this, "$name berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadUserList()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.RED)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.GRAY)
    }

    inner class UserAdapter : BaseAdapter() {
        override fun getCount(): Int = listData.size
        override fun getItem(pos: Int): HashMap<String, String> = listData[pos]
        override fun getItemId(pos: Int): Long = listData[pos]["id"]?.toLongOrNull() ?: 0L

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val view: View
            val tvName: TextView
            val tvEmail: TextView
            val tvRole: TextView

            if (convertView == null) {
                view = LayoutInflater.from(this@ManageUserActivity)
                    .inflate(R.layout.item_user, parent, false)
            } else {
                view = convertView
            }

            tvName  = view.findViewById(R.id.tvUserName)
            tvEmail = view.findViewById(R.id.tvUserEmail)
            tvRole  = view.findViewById(R.id.tvUserRole)

            tvName.text  = item["name"]  ?: "-"
            tvEmail.text = item["email"] ?: "-"
            tvRole.text  = item["role"]  ?: "-"

            val roleColor = when (item["role"]) {
                "ADMIN"    -> Pair(0xFFEDE7F6.toInt(), 0xFF7469B6.toInt())
                "READER"   -> Pair(0xFFE8F5E9.toInt(), 0xFF4CAF50.toInt())
                else       -> Pair(0xFFE3F2FD.toInt(), 0xFF2196F3.toInt())
            }
            tvRole.setBackgroundColor(roleColor.first)
            tvRole.setTextColor(roleColor.second)

            return view
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserList()
    }
}