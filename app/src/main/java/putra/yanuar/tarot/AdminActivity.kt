package putra.yanuar.tarot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityAdminBinding

class AdminActivity : AppCompatActivity() {
    lateinit var b: ActivityAdminBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(b.root)
    }
}