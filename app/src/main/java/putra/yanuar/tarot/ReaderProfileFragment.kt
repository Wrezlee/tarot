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

    private lateinit var b: FragmentReaderProfileBinding
    private lateinit var db: SQLiteDatabase
    private var readerId: Int = 0

    companion object {
        private const val ARG_READER_ID = "reader_id"

        fun newInstance(readerId: Int): ReaderProfileFragment {
            val fragment = ReaderProfileFragment()
            val args = Bundle()
            args.putInt(ARG_READER_ID, readerId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        b = FragmentReaderProfileBinding.inflate(inflater, container, false)

        readerId = arguments?.getInt(ARG_READER_ID) ?: 0
        db = DBOpenHelper(requireContext()).writableDatabase

        b.btnEditReaderProfile.setOnClickListener {
            val email = getUserEmail()
            val i = Intent(requireContext(), ReaderEditProfileActivity::class.java)
            i.putExtra("USER_EMAIL", email)
            startActivity(i)
        }

        loadProfileData()
        return b.root
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun getUserEmail(): String {
        val c = db.rawQuery("SELECT email FROM users WHERE id = ?", arrayOf(readerId.toString()))
        val email = if (c.moveToFirst()) c.getString(0) ?: "" else ""
        c.close()
        return email
    }

    private fun loadProfileData() {
        val c = db.rawQuery(
            "SELECT id, name, email, role, foto FROM users WHERE id = ?",
            arrayOf(readerId.toString())
        )

        if (c.moveToFirst()) {
            val id = c.getInt(0)
            b.tvReaderProfileName.text  = c.getString(1) ?: "Reader"
            b.tvReaderProfileEmail.text = c.getString(2) ?: ""
            b.tvReaderProfileRole.text  = "LEVEL: ${(c.getString(3) ?: "reader").uppercase()}"

            val fotoBase64 = if (!c.isNull(4)) c.getString(4) else null
            if (!fotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(fotoBase64, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    b.imgReaderAvatar.setImageBitmap(bmp)
                } catch (_: Exception) {
                    b.imgReaderAvatar.setImageResource(R.drawable.meow)
                }
            } else {
                b.imgReaderAvatar.setImageResource(R.drawable.meow)
            }

            val cSesi = db.rawQuery(
                "SELECT COUNT(*) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
                arrayOf(id.toString())
            )
            if (cSesi.moveToFirst()) b.tvReaderTotalSessions.text = cSesi.getInt(0).toString()
            cSesi.close()

            val cEarn = db.rawQuery(
                "SELECT SUM(total_price) FROM bookings WHERE reader_id = ? AND status IN ('completed','COMPLETED','done','DONE')",
                arrayOf(id.toString())
            )
            if (cEarn.moveToFirst()) b.tvReaderTotalEarning.text = "Rp${cEarn.getInt(0)}"
            cEarn.close()
        } else {
            b.tvReaderProfileName.text = "Reader"
        }
        c.close()
    }
}