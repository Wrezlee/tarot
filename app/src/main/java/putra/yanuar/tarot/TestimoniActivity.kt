package putra.yanuar.tarot

import android.app.AlertDialog
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityTestimoniBinding
import putra.yanuar.tarot.databinding.ItemTestimoniBinding

class TestimoniActivity : AppCompatActivity() {

    lateinit var b: ActivityTestimoniBinding
    lateinit var db: SQLiteDatabase
    val listData = ArrayList<TestimoniItem>()

    data class TestimoniItem(
        val id: Int,
        val name: String,
        val message: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTestimoniBinding.inflate(layoutInflater)
        setContentView(b.root)

        db = DBOpenHelper(this).writableDatabase
        loadTestimoni()
    }

    fun loadTestimoni() {
        listData.clear()

        try {
            val cursor = db.rawQuery(
                "SELECT t.id, u.name, t.message FROM testimonials t JOIN users u ON t.user_id = u.id ORDER BY t.id DESC",
                null
            )
            while (cursor.moveToNext()) {
                val item = TestimoniItem(
                    id = cursor.getInt(0),
                    name = cursor.getString(1) ?: "Anonim",
                    message = cursor.getString(2) ?: "-"
                )
                listData.add(item)
            }
            cursor.close()

            if (listData.isEmpty()) {
                Toast.makeText(this, "Belum ada testimoni", Toast.LENGTH_SHORT).show()
            }

            b.lvTestimoni.adapter = TestimoniAdapter()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteTestimoni(testimoniId: Int, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Testimoni")
            .setMessage("Apakah kamu yakin ingin menghapus ulasan ini?")
            .setPositiveButton("Ya, Hapus") { _, _ ->
                try {
                    db.execSQL(
                        "DELETE FROM testimonials WHERE id = ?",
                        arrayOf(testimoniId.toString())
                    )
                    Toast.makeText(this, "Testimoni berhasil dihapus", Toast.LENGTH_SHORT).show()
                    listData.removeAt(position)
                    (b.lvTestimoni.adapter as TestimoniAdapter).notifyDataSetChanged()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menghapus: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    inner class TestimoniAdapter : BaseAdapter() {
        override fun getCount(): Int = listData.size
        override fun getItem(position: Int): TestimoniItem = listData[position]
        override fun getItemId(position: Int): Long = listData[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val item = getItem(position)
            val binding: ItemTestimoniBinding
            val view: View

            if (convertView == null) {
                binding = ItemTestimoniBinding.inflate(LayoutInflater.from(this@TestimoniActivity), parent, false)
                view = binding.root
                view.tag = binding
            } else {
                binding = convertView.tag as ItemTestimoniBinding
                view = convertView
            }

            binding.tvTestimoniName.text = item.name
            binding.tvTestimoniMessage.text = item.message

            binding.btnDeleteTestimoni.setOnClickListener {
                deleteTestimoni(item.id, position)
            }

            return view
        }
    }
}