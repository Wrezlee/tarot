package putra.yanuar.tarot

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context) : SQLiteOpenHelper(context, "tarot_meow_db", null, 9) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT, email TEXT, password TEXT, role TEXT,
                is_online INTEGER DEFAULT 0, foto TEXT DEFAULT '',
                lat REAL DEFAULT 0, lng REAL DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tarot_packages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT, category TEXT, description TEXT, price INTEGER,
                question_limit INTEGER, duration INTEGER,
                is_online INTEGER DEFAULT 1, is_offline INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS addons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT, price INTEGER, description TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS bookings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER, reader_id INTEGER DEFAULT 0, reader_name TEXT,
                package_name TEXT, type TEXT, booking_date TEXT, booking_time TEXT,
                name TEXT, email TEXT, phone TEXT, payment_method TEXT,
                status TEXT DEFAULT 'pending', total_price INTEGER, notes TEXT,
                answer TEXT DEFAULT '',
                qr_content TEXT DEFAULT ''
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                booking_id INTEGER, question TEXT, answer TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS testimonials (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER, booking_id INTEGER DEFAULT 0,
                package_name TEXT DEFAULT '', rating INTEGER DEFAULT 0,
                message TEXT, created_at TEXT DEFAULT (datetime('now','localtime'))
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reader_notes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                reader_id INTEGER, booking_id INTEGER,
                note TEXT, created_at TEXT DEFAULT (datetime('now','localtime'))
            )
        """)

        seedData(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            try { db.execSQL("ALTER TABLE users ADD COLUMN foto TEXT DEFAULT ''") } catch (_: Exception) {}
        }
        if (oldVersion < 5) {
            try { db.execSQL("ALTER TABLE testimonials ADD COLUMN rating INTEGER DEFAULT 0") } catch (_: Exception) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reader_notes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        reader_id INTEGER, booking_id INTEGER,
                        note TEXT, created_at TEXT DEFAULT (datetime('now','localtime'))
                    )
                """)
            } catch (_: Exception) {}
        }
        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE bookings ADD COLUMN qr_content TEXT DEFAULT ''") } catch (_: Exception) {}
        }
        if (oldVersion < 8) {
            try { db.execSQL("ALTER TABLE bookings ADD COLUMN answer TEXT DEFAULT ''") } catch (_: Exception) {}
        }
        if (oldVersion < 9) {
            try { db.execSQL("ALTER TABLE users ADD COLUMN lat REAL DEFAULT 0") } catch (_: Exception) {}
            try { db.execSQL("ALTER TABLE users ADD COLUMN lng REAL DEFAULT 0") } catch (_: Exception) {}
        }
    }

    private fun seedData(db: SQLiteDatabase) {
        db.execSQL("INSERT INTO users (name, email, password, role, is_online) VALUES ('Admin Meow', 'admin@gmail.com', '123', 'admin', 0)")
        db.execSQL("INSERT INTO users (name, email, password, role, is_online) VALUES ('Mas Ruli', 'reader@gmail.com', '123', 'reader', 1)")
        db.execSQL("INSERT INTO users (name, email, password, role, is_online) VALUES ('Putra Yanuar', 'customer@gmail.com', '123', 'customer', 0)")

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
        for (pkg in packages) db.execSQL("INSERT INTO tarot_packages (name, category, price) VALUES $pkg")

        db.execSQL("INSERT INTO addons (name, price) VALUES ('Oracle Card', 10000)")
        db.execSQL("INSERT INTO addons (name, price) VALUES ('Fast Track', 30000)")
    }
}