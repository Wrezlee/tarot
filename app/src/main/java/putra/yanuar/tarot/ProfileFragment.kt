package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {

    lateinit var b: FragmentProfileBinding
    lateinit var thisParent: CustomerActivity
    lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = FragmentProfileBinding.inflate(inflater, container, false)

        thisParent = activity as CustomerActivity
        db = thisParent.getDbObject()

        // Tombol overflow (logout)
        b.btnOverflowProfile.setOnClickListener { v ->
            showOverflowMenu(v)
        }

        // Tombol Edit Profile
        b.btnEditProfile.setOnClickListener {
            val i = Intent(thisParent, EditProfileActivity::class.java)
            i.putExtra("USER_EMAIL", thisParent.userEmail)
            startActivity(i)
        }

        // Card Riwayat Ramalan — klik langsung buka HistoryActivity dengan email dari database
        b.btnHistory.setOnClickListener {
            try {
                val i = Intent(thisParent, HistoryActivity::class.java)
                i.putExtra("USER_EMAIL", thisParent.userEmail)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(thisParent, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Card Bagikan Keajaiban — tampilkan dialog input lalu simpan ke tabel testimonials
        b.btnTestimony.setOnClickListener {
            try {
                showTestimonialDialog()
            } catch (e: Exception) {
                Toast.makeText(thisParent, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        loadUserData()
        return b.root
    }

    fun showOverflowMenu(v: View) {
        val popup = PopupMenu(thisParent, v)
        popup.menuInflater.inflate(R.menu.menu_cust_profil, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_cust_logout -> {
                    val intent = Intent(thisParent, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    thisParent.finish()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    /**
     * Dialog input testimoni.
     * Setelah user submit:
     *  1. Cari user_id berdasarkan email yang sedang login
     *  2. INSERT ke tabel testimonials
     *  3. Refresh tampilan (tvTestimonialsCount ter-update)
     */
    fun showTestimonialDialog() {
        val builder = AlertDialog.Builder(thisParent)
        val input = EditText(thisParent)
        input.hint = "Tulis pengalaman mistismu..."
        builder.setTitle("✨ Share Your Magic")
        builder.setView(input)

        builder.setPositiveButton("Kirim") { _, _ ->
            val pesan = input.text.toString().trim()
            if (pesan.isNotEmpty()) {
                // Ambil user_id dari tabel users berdasarkan email login
                val cursor = db.rawQuery(
                    "SELECT id FROM users WHERE email = ?",
                    arrayOf(thisParent.userEmail)
                )
                if (cursor.moveToFirst()) {
                    val userId = cursor.getInt(0)
                    try {
                        db.execSQL(
                            "INSERT INTO testimonials (user_id, message) VALUES (?, ?)",
                            arrayOf(userId.toString(), pesan)
                        )
                        Toast.makeText(
                            thisParent,
                            "Terima kasih atas testimoninya! 💕",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Refresh counter testimoni
                        loadUserData()
                    } catch (e: Exception) {
                        Toast.makeText(
                            thisParent,
                            "Gagal menyimpan testimoni: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(thisParent, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
                cursor.close()
            } else {
                Toast.makeText(thisParent, "Testimoni tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    /**
     * Muat data profil dari database:
     *  - Nama, email, role
     *  - Jumlah booking yang sudah paid/done  → tvTotalReadings
     *  - Jumlah testimoni yang pernah dikirim → tvTestimonialsCount
     */
    fun loadUserData() {
        val emailLogin = thisParent.userEmail

        val c = db.rawQuery(
            "SELECT id, name, email, role FROM users WHERE email = ?",
            arrayOf(emailLogin)
        )

        if (c.moveToFirst()) {
            val userId = c.getInt(0)
            b.tvProfileName.text  = c.getString(1)
            b.tvProfileEmail.text = c.getString(2)
            b.tvProfileRole.text  = "LEVEL: ${c.getString(3).uppercase()}"

            // Hitung total ritual yang sudah selesai (paid / done)
            val cRitual = db.rawQuery(
                """SELECT COUNT(*) FROM bookings 
                   WHERE email = ? 
                   AND (status = 'paid' OR status = 'PAID' 
                        OR status = 'done' OR status = 'DONE'
                        OR status = 'completed' OR status = 'COMPLETED')""",
                arrayOf(emailLogin)
            )
            if (cRitual.moveToFirst()) {
                b.tvTotalReadings.text = cRitual.getInt(0).toString()
            }
            cRitual.close()

            // Hitung total testimoni yang sudah dikirim user ini
            val cTesti = db.rawQuery(
                "SELECT COUNT(*) FROM testimonials WHERE user_id = ?",
                arrayOf(userId.toString())
            )
            if (cTesti.moveToFirst()) {
                b.tvTestimonialsCount.text = cTesti.getInt(0).toString()
            }
            cTesti.close()

        } else {
            b.tvProfileName.text = "User Tidak Ditemukan"
        }
        c.close()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }
}