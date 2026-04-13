package putra.yanuar.tarot

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context) : SQLiteOpenHelper(context, "tarot_meow_db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabel Users
        db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT, password TEXT, role TEXT)")

        // 2. Tabel Tarot Packages (Nama sinkron dengan Activity: tarot_packages)
        db.execSQL("CREATE TABLE tarot_packages (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, category TEXT, description TEXT, price INTEGER, question_limit INTEGER, duration INTEGER, is_online INTEGER DEFAULT 1, is_offline INTEGER DEFAULT 0)")

        // 3. Tabel Addons
        db.execSQL("CREATE TABLE addons (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, price INTEGER, description TEXT)")

        // 4. Tabel Bookings
        db.execSQL("""
            CREATE TABLE bookings ( 
                id INTEGER PRIMARY KEY AUTOINCREMENT, 
                user_id INTEGER, 
                package_name TEXT, 
                type TEXT, 
                booking_date TEXT, 
                booking_time TEXT, 
                name TEXT, 
                email TEXT, 
                phone TEXT, 
                payment_method TEXT,
                status TEXT DEFAULT 'pending', 
                total_price INTEGER, 
                notes TEXT
            )
        """)

        // 5. Tabel Questions
        db.execSQL("CREATE TABLE questions (id INTEGER PRIMARY KEY AUTOINCREMENT, booking_id INTEGER, question TEXT, answer TEXT)")

        // 6. Tabel Testimonials
        db.execSQL("CREATE TABLE testimonials (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id INTEGER, message TEXT)")

        // --- SEEDING DATA ---

        // Users
        db.execSQL("INSERT INTO users (name, email, password, role) VALUES ('Admin Meow', 'admin@meow.com', '123', 'admin')")
        db.execSQL("INSERT INTO users (name, email, password, role) VALUES ('Madame Seraphina', 'reader@meow.com', '123', 'reader')")
        db.execSQL("INSERT INTO users (name, email, password, role) VALUES ('Putra Yanuar', 'customer@meow.com', '123', 'customer')")

        // Packages Data (Menggunakan Data Lama kamu ke Tabel tarot_packages)
        val packages = arrayOf(
            "('1 Kartu (1 Pertanyaan)', 'tarot', 5000)",
            "('3 Kartu (1 Pertanyaan)', 'tarot', 10000)",
            "('6 Kartu (2 Pertanyaan)', 'tarot', 30000)",
            "('9 Kartu (Deep Reading)', 'tarot', 50000)",
            "('Analisis Telapak Tangan', 'palm', 40000)",
            "('10 Menit Chat', 'chat', 50000)",
            "('15 Menit Chat', 'chat', 70000)",
            "('20 Menit Chat', 'chat', 90000)",
            "('30 Menit Chat', 'chat', 130000)",
            "('30 Menit Call', 'call', 90000)",
            "('1 Jam Call', 'call', 130000)"
        )
        for (pkg in packages) {
            db.execSQL("INSERT INTO tarot_packages (name, category, price) VALUES $pkg")
        }

        // Addons
        db.execSQL("INSERT INTO addons (name, price) VALUES ('Oracle Card', 10000)")
        db.execSQL("INSERT INTO addons (name, price) VALUES ('Fast Track', 30000)")

        // Data Testing Awal
        db.execSQL("""
            INSERT INTO bookings (user_id, package_name, type, booking_date, booking_time, name, email, status, total_price) 
            VALUES (3, '9 Kartu (Deep Reading)', 'online', '2026-04-12', '10:45', 'Fateema Az Zahra', 'customer@meow.com', 'paid', 60000)
        """)

        db.execSQL("INSERT INTO questions (booking_id, question) VALUES (1, 'Bagaimana keberuntungan saya bulan ini?')")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS tarot_packages")
        db.execSQL("DROP TABLE IF EXISTS addons")
        db.execSQL("DROP TABLE IF EXISTS bookings")
        db.execSQL("DROP TABLE IF EXISTS questions")
        db.execSQL("DROP TABLE IF EXISTS testimonials")
        onCreate(db)
    }
}