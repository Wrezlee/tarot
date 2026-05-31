package putra.yanuar.tarot

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private var currentPage = 0
    private lateinit var prefs: SharedPreferences

    data class OnboardingPage(
        val emoji: String,
        val title: String,
        val desc: String
    )

    private val pages = listOf(
        OnboardingPage(
            "🔮",
            "Selamat Datang di\nTarot Meow",
            "Platform pembacaan tarot profesional yang menghubungkanmu dengan reader berpengalaman."
        ),
        OnboardingPage(
            "📅",
            "Booking Mudah &\nCepat",
            "Pilih reader favoritmu, tentukan jadwal, dan pesan paket ritual tarot sesuai kebutuhanmu."
        ),
        OnboardingPage(
            "✨",
            "Ramalan Akurat &\nTerpercaya",
            "Dapatkan jawaban atas pertanyaan cinta, karier, dan masa depanmu dari reader terbaik kami."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        prefs = getSharedPreferences("tarot_prefs", MODE_PRIVATE)

        if (prefs.getBoolean("onboarding_done", false)) {
            goToMain()
            return
        }

        buildUI()
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFE6E6.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        setContentView(root)

        val p = (24 * resources.displayMetrics.density).toInt()

        val contentArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val tvEmoji = TextView(this).apply {
            textSize = 80f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(this).apply {
            textSize = 26f
            setTextColor(0xFF7469B6.toInt())
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = (24 * resources.displayMetrics.density).toInt()
            }
        }

        val tvDesc = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFAD88C6.toInt())
            gravity = Gravity.CENTER

            // PERBAIKAN DI SINI
            setLineSpacing(0f, 1.4f)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = (12 * resources.displayMetrics.density).toInt()
            }
        }

        contentArea.addView(tvEmoji)
        contentArea.addView(tvTitle)
        contentArea.addView(tvDesc)

        val dotsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        val dots = ArrayList<TextView>()

        for (i in pages.indices) {
            val dot = TextView(this).apply {
                text = "●"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.marginStart = (6 * resources.displayMetrics.density).toInt()
                    it.marginEnd = (6 * resources.displayMetrics.density).toInt()
                }
            }

            dots.add(dot)
            dotsRow.addView(dot)
        }

        val btnNext = MaterialButton(this).apply {
            text = "Selanjutnya"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0xFF7469B6.toInt())
            cornerRadius = (16 * resources.displayMetrics.density).toInt()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * resources.displayMetrics.density).toInt()
            ).also {
                it.marginStart = p
                it.marginEnd = p
                it.bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        val btnSkip = MaterialButton(this).apply {
            text = "Lewati"
            textSize = 13f
            setTextColor(0xFFAD88C6.toInt())
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(0x00000000)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart = p
                it.marginEnd = p
                it.bottomMargin = (16 * resources.displayMetrics.density).toInt()
            }
        }

        root.addView(contentArea)
        root.addView(dotsRow)
        root.addView(btnNext)
        root.addView(btnSkip)

        fun updatePage() {
            val page = pages[currentPage]

            tvEmoji.text = page.emoji
            tvTitle.text = page.title
            tvDesc.text = page.desc

            for (i in dots.indices) {
                dots[i].setTextColor(
                    if (i == currentPage) {
                        0xFF7469B6.toInt()
                    } else {
                        0xFFD0C8E8.toInt()
                    }
                )
            }

            if (currentPage == pages.lastIndex) {
                btnNext.text = "Mulai Sekarang"
                btnSkip.visibility = View.GONE
            } else {
                btnNext.text = "Selanjutnya"
                btnSkip.visibility = View.VISIBLE
            }
        }

        updatePage()

        btnNext.setOnClickListener {
            if (currentPage < pages.lastIndex) {
                currentPage++
                updatePage()
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        prefs.edit()
            .putBoolean("onboarding_done", true)
            .apply()

        goToMain()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}