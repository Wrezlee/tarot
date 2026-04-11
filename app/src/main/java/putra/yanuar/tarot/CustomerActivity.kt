package putra.yanuar.tarot

import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView
import putra.yanuar.tarot.databinding.ActivityCustomerBinding

class CustomerActivity : AppCompatActivity(),
    NavigationBarView.OnItemSelectedListener {
    lateinit var b: ActivityCustomerBinding
    lateinit var db: SQLiteDatabase
    lateinit var ft: FragmentTransaction
    lateinit var fragBooking: BookingFragment
    lateinit var fragProfile: ProfileFragment

    // Variabel untuk menampung email yang login
    lateinit var userEmail: String

    public fun getDbObject(): SQLiteDatabase {
        return db
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityCustomerBinding.inflate(layoutInflater)
        setContentView(b.root)

        // AMBIL EMAIL DARI INTENT LOGIN
        userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        fragBooking = BookingFragment()
        fragProfile = ProfileFragment()
        db = DBOpenHelper(this).writableDatabase

        b.navbarCustomer.setOnItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        ft = supportFragmentManager.beginTransaction()

        when (p0.itemId) {
            R.id.itemHome -> {
                b.nestedScrollView.visibility = View.VISIBLE
                b.container.visibility = View.GONE
            }
            R.id.itemBooking -> {
                ft.replace(R.id.container, fragBooking)
                ft.commit()

                b.nestedScrollView.visibility = View.GONE
                b.container.visibility = View.VISIBLE
            }
            R.id.itemProfile -> {
                ft.replace(R.id.container, fragProfile)
                ft.commit()

                b.nestedScrollView.visibility = View.GONE
                b.container.visibility = View.VISIBLE
            }
        }
        return true
    }
}