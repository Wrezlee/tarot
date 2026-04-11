package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import putra.yanuar.tarot.databinding.FragmentBookingBinding

class BookingFragment : Fragment(), View.OnClickListener {

    lateinit var b: FragmentBookingBinding
    lateinit var v: View
    lateinit var thisParent: CustomerActivity
    lateinit var db: SQLiteDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        b = FragmentBookingBinding.inflate(inflater, container, false)
        v = b.root

        thisParent = activity as CustomerActivity
        db = thisParent.getDbObject()

        // Listener untuk tombol
        b.btnCaraOrder.setOnClickListener(this)

        return v
    }

    override fun onClick(p0: View?) {
        if (p0?.id == R.id.btnCaraOrder) {
            val builder = AlertDialog.Builder(thisParent)
            builder.setTitle("💳 CARA ORDER")
            builder.setMessage("1. Pilih Paket\n2. Transfer ke Fateema Az Zahra\n3. Kirim bukti ke WA\n4. Konsultasi dimulai ✨")
            builder.setPositiveButton("OK", null)
            builder.show()
        }
    }
}