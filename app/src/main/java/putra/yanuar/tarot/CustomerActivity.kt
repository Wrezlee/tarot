package putra.yanuar.tarot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityCustomerBinding

class CustomerActivity : AppCompatActivity (){
    lateinit var b: ActivityCustomerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityCustomerBinding.inflate(layoutInflater)
        setContentView(b.root)
    }
}
