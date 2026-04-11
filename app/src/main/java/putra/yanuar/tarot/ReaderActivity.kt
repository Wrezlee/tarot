package putra.yanuar.tarot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import putra.yanuar.tarot.databinding.ActivityReaderBinding

class ReaderActivity : AppCompatActivity() {
    lateinit var b: ActivityReaderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        b = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(b.root)
    }
}
