package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentReaderProfileBinding

class ReaderProfileFragment : Fragment() {

    lateinit var b: FragmentReaderProfileBinding
    lateinit var thisParent: ReaderActivity
    lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentReaderProfileBinding.inflate(inflater, container, false)
        thisParent = activity as ReaderActivity
        db = thisParent.getDbObject()

        b.btnEditReaderProfile.setOnClickListener {
            val i = Intent(thisParent, EditProfileActivity::class.java)
            i.putExtra("USER_EMAIL", thisParent.getUserEmail())
            startActivity(i)
        }

        loadProfileData()
        return b.root
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        val email = thisParent.getUserEmail()

        val c = db.rawQuery(
            "SELECT id, name, email, role, foto FROM users WHERE email = ?",
            arrayOf(email)
        )

        if (c.moveToFirst()) {
            val readerId = c.getInt(0)
            b.tvReaderProfileName.text  = c.getString(1)
            b.tvReaderProfileEmail.text = c.getString(2)
            b.tvReaderProfileRole.text  = "LEVEL: ${c.getString(3).uppercase()}"
            b.tvReaderInfoEmail.text    = c.getString(2)

            // Load foto profil
            val fotoBase64 = if (!c.isNull(4)) c.getString(4) else null
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    b.imgReaderAvatar.setImageBitmap(bmp)
                } catch (e: Exception) {
                    b.imgReaderAvatar.setImageResource(R.drawable.meow)
                }
            } else {
                b.imgReaderAvatar.setImageResource(R.drawable.meow)
            }

            // Total sesi selesai
            val cSesi = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
                arrayOf(readerId.toString())
            )
            if (cSesi.moveToFirst()) b.tvReaderTotalSessions.text = cSesi.getInt(0).toString()
            cSesi.close()

            // Total earning
            val cEarn = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
                arrayOf(readerId.toString())
            )
            if (cEarn.moveToFirst()) b.tvReaderTotalEarning.text = "Rp${cEarn.getInt(0)}"
            cEarn.close()
        } else {
            b.tvReaderProfileName.text = "Reader"
        }
        c.close()
    }
}