package putra.yanuar.tarot

import android.content.res.ColorStateList
import android.database.sqlite.SQLiteDatabase
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
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
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFE6E6.toInt())
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val p = (24 * resources.displayMetrics.density).toInt()
        root.setPadding(p, p * 3, p, p)
        setContentView(root)

        val tvTitle = TextView(this).apply {
            text = "LUPA PASSWORD"
            textSize = 28f
            setTextColor(0xFF7469B6.toInt())
            setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvSub = TextView(this).apply {
            text = "Masukkan email terdaftarmu. Kami akan menampilkan password akunmu."
            textSize = 13f
            setTextColor(0xFFAD88C6.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }

        val tilEmail = TextInputLayout(
            this,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            hint = "Email terdaftar"
            boxStrokeColor = 0xFF7469B6.toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = (32 * resources.displayMetrics.density).toInt()
            }
        }

        val etEmail = TextInputEditText(tilEmail.context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        tilEmail.addView(etEmail)

        val btnRecover = MaterialButton(this).apply {
            text = "CARI AKUN"
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (64 * resources.displayMetrics.density).toInt()
            ).also {
                it.topMargin = (24 * resources.displayMetrics.density).toInt()
            }
            backgroundTintList = ColorStateList.valueOf(0xFF7469B6.toInt())
            cornerRadius = (16 * resources.displayMetrics.density).toInt()
        }

        val btnBack = MaterialButton(this).apply {
            text = "Kembali ke Login"
            textSize = 13f
            setTextColor(0xFFAD88C6.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = (8 * resources.displayMetrics.density).toInt()
            }
            backgroundTintList = ColorStateList.valueOf(0x00000000)
        }

        root.addView(tvTitle)
        root.addView(tvSub)
        root.addView(tilEmail)
        root.addView(btnRecover)
        root.addView(btnBack)

        btnRecover.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Harap masukkan email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            recoverPassword(email)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun recoverPassword(email: String) {
        try {
            val cursor = db.rawQuery(
                "SELECT name, password, role FROM users WHERE email = ?",
                arrayOf(email)
            )

            if (cursor.moveToFirst()) {
                val name = cursor.getString(0) ?: "Pengguna"
                val password = cursor.getString(1) ?: "-"
                val role = cursor.getString(2) ?: "-"

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
                    .setPositiveButton("Ke Login") { _, _ ->
                        finish()
                    }
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