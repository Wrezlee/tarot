package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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

        // Card Riwayat Ramalan → buka HistoryActivity
        b.btnHistory.setOnClickListener {
            try {
                val i = Intent(thisParent, HistoryActivity::class.java)
                i.putExtra("USER_EMAIL", thisParent.userEmail)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(thisParent, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Card Bagikan Keajaiban → arahkan ke riwayat ramalan untuk tulis ulasan
        b.btnTestimony.setOnClickListener {
            Toast.makeText(
                thisParent,
                "Tulis ulasan melalui Riwayat Ramalan setelah sesi selesai ✨",
                Toast.LENGTH_LONG
            ).show()
            // Langsung buka HistoryActivity agar customer bisa langsung ke sana
            val i = Intent(thisParent, HistoryActivity::class.java)
            i.putExtra("USER_EMAIL", thisParent.userEmail)
            startActivity(i)
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

            // Hitung total ritual yang sudah selesai
            val cRitual = db.rawQuery(
                """SELECT COUNT(*) FROM bookings 
                   WHERE email = ? 
                   AND (status = 'done' OR status = 'DONE'
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