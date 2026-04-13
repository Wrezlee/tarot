package putra.yanuar.tarot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Menyembunyikan Action Bar untuk tampilan full screen
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        // Handler untuk menunda perpindahan halaman
        Handler(Looper.getMainLooper()).postDelayed({
            // Berpindah ke MainActivity (atau LoginActivity jika ada)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            // Menutup SplashActivity agar tidak bisa kembali saat tekan tombol 'Back'
            finish()
        }, 1000)
    }
}