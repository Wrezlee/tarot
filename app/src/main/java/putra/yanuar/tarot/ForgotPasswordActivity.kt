package putra.yanuar.tarot

import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var db: SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()

        db = DBOpenHelper(this).writableDatabase
        buildUI()
    }

    private fun buildUI() {
        val dm = resources.displayMetrics
        fun Int.dp() = (this * dm.density).toInt()
        fun Float.dp() = (this * dm.density).toInt()

        // Root — sama persis dengan activity_main.xml: background #FFF8F7
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFF8F7.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Logo + judul di atas card (sama seperti login)
        val logoLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 86.dp() }
        }

        val imgLogo = ImageView(this).apply {
            setImageResource(R.drawable.meow)
            layoutParams = LinearLayout.LayoutParams(216.dp(), 186.dp())
        }

        val tvAppName = TextView(this).apply {
            text = "TAROT MEOW"
            textSize = 24f
            setTextColor(0xFF5B509B.toInt())
            setTypeface(Typeface.SERIF, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 16.dp() }
        }

        logoLayout.addView(imgLogo)
        logoLayout.addView(tvAppName)

        // Card — sama seperti login: #CCFFFFFF, cornerRadius 32dp, margin 24dp
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.setMargins(24.dp(), 0, 24.dp(), 24.dp())
                it.topMargin = 0
            }
            setCardBackgroundColor(0xCCFFFFFF.toInt())
            radius = 32f * dm.density
            cardElevation = 0f
        }

        val cardInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(32.dp(), 32.dp(), 32.dp(), 32.dp())
        }

        val tvTitle = TextView(this).apply {
            text = "Lupa Password"
            textSize = 32f
            setTypeface(Typeface.SERIF, Typeface.NORMAL)
            setTextColor(0xFF251819.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvSub = TextView(this).apply {
            text = "Masukkan email terdaftarmu untuk melihat password akun."
            textSize = 13f
            setTextColor(0x80251819.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8.dp() }
        }

        // Label EMAIL — sama persis gaya login
        val tvEmailLabel = TextView(this).apply {
            text = "EMAIL"
            textSize = 10f
            setTextColor(0xFF5B509B.toInt())
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 24.dp() }
        }

        val etEmail = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            hint = ""
            setBackgroundResource(R.drawable.bg_input)
            setTextColor(0xFF251819.toInt())
            setHintTextColor(0x9D251819.toInt())
            textSize = 16f
            setPadding(16.dp(), 0, 16.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                56.dp()
            ).also { it.topMargin = 8.dp() }
        }

        val btnRecover = MaterialButton(this).apply {
            text = "CARI AKUN"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            letterSpacing = 0.1f
            setTypeface(null, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_button_gradient)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                64.dp()
            ).also { it.topMargin = 32.dp() }
            backgroundTintList = null   // biarkan drawable gradient yang jalan
            cornerRadius = 100.dp()
            elevation = 8f * dm.density
        }

        val btnBack = MaterialButton(this).apply {
            text = "Kembali ke Login"
            textSize = 13f
            setTextColor(0xFF7469B6.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = 8.dp() }
            backgroundTintList = ColorStateList.valueOf(0x00000000)
        }

        cardInner.addView(tvTitle)
        cardInner.addView(tvSub)
        cardInner.addView(tvEmailLabel)
        cardInner.addView(etEmail)
        cardInner.addView(btnRecover)
        cardInner.addView(btnBack)
        card.addView(cardInner)

        root.addView(logoLayout)
        root.addView(card)

        setContentView(root)

        btnRecover.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Harap masukkan email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            recoverPassword(email)
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun recoverPassword(email: String) {
        try {
            val cursor = db.rawQuery(
                "SELECT name, password, role FROM users WHERE email = ?",
                arrayOf(email)
            )

            if (cursor.moveToFirst()) {
                val name     = cursor.getString(0) ?: "Pengguna"
                val password = cursor.getString(1) ?: "-"
                val role     = cursor.getString(2) ?: "-"
                cursor.close()

                AlertDialog.Builder(this)
                    .setTitle("✅ Akun Ditemukan")
                    .setMessage(
                        "Halo, $name!\n\n" +
                                "Role  : ${role.uppercase()}\n" +
                                "Email : $email\n\n" +
                                "Password kamu adalah:\n\n" +
                                "🔑  $password\n\n" +
                                "Segera ganti password setelah login ya!"
                    )
                    .setPositiveButton("Ke Login") { _, _ -> finish() }
                    .setCancelable(false)
                    .show()
            } else {
                cursor.close()

                AlertDialog.Builder(this)
                    .setTitle("❌ Akun Tidak Ditemukan")
                    .setMessage(
                        "Email \"$email\" tidak terdaftar di sistem.\n\n" +
                                "Pastikan email yang kamu masukkan sudah benar."
                    )
                    .setPositiveButton("Coba Lagi", null)
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.close()
    }
}