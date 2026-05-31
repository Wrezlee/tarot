package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        b.btnEditProfile.setOnClickListener {
            val i = Intent(thisParent, EditProfileActivity::class.java)
            i.putExtra("USER_EMAIL", thisParent.userEmail)
            startActivity(i)
        }

        b.btnHistory.setOnClickListener {
            try {
                val i = Intent(thisParent, HistoryActivity::class.java)
                i.putExtra("USER_EMAIL", thisParent.userEmail)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(thisParent, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        loadUserData()
        return b.root
    }

    fun loadUserData() {
        val emailLogin = thisParent.userEmail

        val c = db.rawQuery(
            "SELECT id, name, email, role, foto FROM users WHERE email = ?",
            arrayOf(emailLogin)
        )

        if (c.moveToFirst()) {
            val userId = c.getInt(0)
            b.tvProfileName.text  = c.getString(1)
            b.tvProfileEmail.text = c.getString(2)
            b.tvProfileRole.text  = "LEVEL: ${c.getString(3).uppercase()}"

            // Load foto profil dari Base64 jika ada
            val fotoBase64 = if (!c.isNull(4)) c.getString(4) else null
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    b.imgAvatar.setImageBitmap(bmp)
                } catch (e: Exception) {
                    b.imgAvatar.setImageResource(R.drawable.meow)
                }
            } else {
                b.imgAvatar.setImageResource(R.drawable.meow)
            }

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