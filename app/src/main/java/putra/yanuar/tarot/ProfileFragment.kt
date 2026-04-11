package putra.yanuar.tarot

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentProfileBinding

class ProfileFragment : Fragment(), View.OnClickListener {

    lateinit var b: FragmentProfileBinding
    lateinit var v: View
    lateinit var thisParent: CustomerActivity
    lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 1. Inisialisasi Binding
        b = FragmentProfileBinding.inflate(inflater, container, false)
        v = b.root

        // 2. Inisialisasi Parent & DB (Style Mobile07a)
        thisParent = activity as CustomerActivity
        db = thisParent.getDbObject()

        // 3. Set Listener Tombol
        b.btnEditProfile.setOnClickListener(this)
        b.btnHistory.setOnClickListener(this)
        b.btnLogout.setOnClickListener(this)

        // 4. Muat Data
        loadUserData()

        return v
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.btnEditProfile -> {
                Toast.makeText(thisParent, "Fitur Edit Profile Belum Tersedia", Toast.LENGTH_SHORT).show()
            }
            R.id.btnHistory -> {
                Toast.makeText(thisParent, "Membuka Riwayat Ramalan...", Toast.LENGTH_SHORT).show()
            }
            R.id.btnLogout -> {
                // Gunakan thisParent sebagai context, dan panggil MainActivity
                val intent = Intent(thisParent, MainActivity::class.java)
                startActivity(intent)

                // Hancurkan CustomerActivity agar user tidak bisa tekan back untuk masuk lagi
                thisParent.finish()
            }
        }
    }

    fun loadUserData() {
        // Ambil email dari Activity parent
        val emailLogin = thisParent.userEmail

        // Cari user yang emailnya sesuai, bukan cuma baris pertama
        val sql = "SELECT name, email, role FROM users WHERE email = '$emailLogin'"
        val c = db.rawQuery(sql, null)

        if (c.moveToFirst()) {
            val name = c.getString(0)
            val email = c.getString(1)
            val role = c.getString(2)

            b.tvProfileName.text = name
            b.tvProfileEmail.text = email
            b.tvProfileRole.text = "LEVEL: ${role.uppercase()}"
        } else {
            // Jika data tidak ketemu (opsional)
            b.tvProfileName.text = "Unknown User"
        }
        c.close()
    }
}