package putra.yanuar.tarot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            // Arahkan ke Onboarding (di sana dicek apakah sudah pernah onboarding)
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 1000)
    }
}