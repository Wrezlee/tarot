package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(), View.OnClickListener {

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

        // Registrasi Listener
        b.btnEditProfile.setOnClickListener(this)
        b.btnHistory.setOnClickListener(this)
        b.btnTestimony.setOnClickListener(this)
        b.btnLogout.setOnClickListener(this)

        loadUserData()

        return b.root
    }

    override fun onClick(p0: View?) {
        // Tambahkan try-catch untuk melacak error jika masih force close
        try {
            when (p0?.id) {
                R.id.btnEditProfile -> {
                    val i = Intent(thisParent, EditProfileActivity::class.java)
                    i.putExtra("USER_EMAIL", thisParent.userEmail)
                    startActivity(i)
                }
                R.id.btnHistory -> {
                    val i = Intent(thisParent, HistoryActivity::class.java)
                    i.putExtra("USER_EMAIL", thisParent.userEmail)
                    startActivity(i)
                }
                R.id.btnTestimony -> {
                    showTestimonialDialog()
                }
                R.id.btnLogout -> {
                    val intent = Intent(thisParent, MainActivity::class.java)
                    startActivity(intent)
                    thisParent.finish()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(thisParent, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun showTestimonialDialog() {
        val builder = AlertDialog.Builder(thisParent)
        val input = EditText(thisParent)
        input.hint = "Tulis pengalaman mistismu..."

        builder.setTitle("✨ Share Your Magic")
        builder.setView(input)
        builder.setPositiveButton("Kirim") { _, _ ->
            val pesan = input.text.toString()
            if (pesan.isNotEmpty()) {
                val cursor = db.rawQuery("SELECT id FROM users WHERE email = ?", arrayOf(thisParent.userEmail))
                if (cursor.moveToFirst()) {
                    val userId = cursor.getInt(0)
                    // Perbaikan parameter execSQL
                    db.execSQL("INSERT INTO testimonials (user_id, message) VALUES (?, ?)", arrayOf(userId.toString(), pesan))
                    Toast.makeText(thisParent, "Terima kasih atas testimoninya! 💕", Toast.LENGTH_SHORT).show()
                    loadUserData()
                }
                cursor.close()
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    fun loadUserData() {
        val emailLogin = thisParent.userEmail

        // 1. Ambil Data Profil User
        val sqlUser = "SELECT id, name, email, role FROM users WHERE email = ?"
        val c = db.rawQuery(sqlUser, arrayOf(emailLogin))

        if (c.moveToFirst()) {
            val userId = c.getInt(0)
            b.tvProfileName.text = c.getString(1)
            b.tvProfileEmail.text = c.getString(2)
            b.tvProfileRole.text = "LEVEL: ${c.getString(3).uppercase()}"

            // 2. Hitung Statistik Rituals
            // Gunakan filter status agar hanya menghitung yang sudah lunas/selesai
            // Sesuaikan 'paid' dengan status yang kamu masukkan di OrderActivity
            val cRitual = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE email = ? AND (status = 'paid' OR status = 'PAID' OR status = 'done')",
                arrayOf(emailLogin)
            )

            if (cRitual.moveToFirst()) {
                b.tvTotalReadings.text = cRitual.getInt(0).toString()
            }
            cRitual.close()

            // 3. Hitung Statistik Testimonials
            val cTesti = db.rawQuery("SELECT COUNT(*) FROM testimonials WHERE user_id = ?", arrayOf(userId.toString()))
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