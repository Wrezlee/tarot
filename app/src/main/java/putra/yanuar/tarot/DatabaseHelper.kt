package com.putra.yanuar.tarot

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tarot.model.Booking
import com.example.tarot.model.TarotPackage
import com.example.tarot.model.User

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // ─────────────────────────────────────────────────────────────────────────
    companion object {
        const val DATABASE_NAME    = "tarot_meow.db"
        const val DATABASE_VERSION = 2          // naik dari 1 → 2 untuk kolom qr_content

        // ── Tabel users ──────────────────────────────────────────────────────
        const val TABLE_USERS  = "users"
        const val COL_U_ID     = "id"
        const val COL_U_NAME   = "name"
        const val COL_U_EMAIL  = "email"
        const val COL_U_PASS   = "password"
        const val COL_U_ROLE   = "role"
        const val COL_U_PHOTO  = "photo_path"

        // ── Tabel tarot_packages ─────────────────────────────────────────────
        const val TABLE_PKGS   = "tarot_packages"
        const val COL_P_ID     = "id"
        const val COL_P_NAME   = "name"
        const val COL_P_CAT    = "category"
        const val COL_P_PRICE  = "price"

        // ── Tabel bookings ───────────────────────────────────────────────────
        const val TABLE_BOOK        = "bookings"
        const val COL_B_ID          = "id"
        const val COL_B_CUST_ID     = "customer_id"
        const val COL_B_CUST_NAME   = "customer_name"
        const val COL_B_READER_ID   = "reader_id"
        const val COL_B_READER_NAME = "reader_name"
        const val COL_B_PKG_ID      = "package_id"
        const val COL_B_PKG_NAME    = "package_name"
        const val COL_B_PAYMENT     = "payment_method"
        const val COL_B_PRICE       = "total_price"
        const val COL_B_DATE        = "date"
        const val COL_B_TIME        = "time"
        const val COL_B_NOTES       = "notes"
        const val COL_B_STATUS      = "status"
        const val COL_B_ANSWER      = "answer"
        const val COL_B_QR          = "qr_content"      // ← kolom baru versi 2
        const val COL_B_CREATED_AT  = "created_at"

        // ── Tabel testimonials ───────────────────────────────────────────────
        const val TABLE_TESTI   = "testimonials"
        const val COL_T_ID      = "id"
        const val COL_T_BOOK_ID = "booking_id"
        const val COL_T_CUST_ID = "customer_id"
        const val COL_T_CUST_NM = "customer_name"
        const val COL_T_PKG_NM  = "package_name"
        const val COL_T_RATING  = "rating"
        const val COL_T_MSG     = "message"
        const val COL_T_CREATED = "created_at"
    }

    // ── onCreate ──────────────────────────────────────────────────────────────
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_USERS (
                $COL_U_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_U_NAME  TEXT NOT NULL,
                $COL_U_EMAIL TEXT UNIQUE NOT NULL,
                $COL_U_PASS  TEXT NOT NULL,
                $COL_U_ROLE  TEXT NOT NULL DEFAULT 'customer',
                $COL_U_PHOTO TEXT
            )"""
        )

        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_PKGS (
                $COL_P_ID    INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_P_NAME  TEXT NOT NULL,
                $COL_P_CAT   TEXT NOT NULL DEFAULT 'Tarot',
                $COL_P_PRICE INTEGER NOT NULL DEFAULT 0
            )"""
        )

        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_BOOK (
                $COL_B_ID          INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_B_CUST_ID     TEXT,
                $COL_B_CUST_NAME   TEXT,
                $COL_B_READER_ID   TEXT,
                $COL_B_READER_NAME TEXT,
                $COL_B_PKG_ID      TEXT,
                $COL_B_PKG_NAME    TEXT,
                $COL_B_PAYMENT     TEXT,
                $COL_B_PRICE       INTEGER DEFAULT 0,
                $COL_B_DATE        TEXT,
                $COL_B_TIME        TEXT,
                $COL_B_NOTES       TEXT,
                $COL_B_STATUS      TEXT DEFAULT 'PENDING',
                $COL_B_ANSWER      TEXT,
                $COL_B_QR          TEXT,
                $COL_B_CREATED_AT  TEXT DEFAULT (datetime('now','localtime'))
            )"""
        )

        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_TESTI (
                $COL_T_ID      INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_T_BOOK_ID TEXT,
                $COL_T_CUST_ID TEXT,
                $COL_T_CUST_NM TEXT,
                $COL_T_PKG_NM  TEXT,
                $COL_T_RATING  INTEGER DEFAULT 5,
                $COL_T_MSG     TEXT,
                $COL_T_CREATED TEXT DEFAULT (datetime('now','localtime'))
            )"""
        )

        // Seed admin default
        db.execSQL(
            """INSERT OR IGNORE INTO $TABLE_USERS
               ($COL_U_NAME,$COL_U_EMAIL,$COL_U_PASS,$COL_U_ROLE)
               VALUES ('Administrator','admin@tarotmeow.com','admin123','admin')"""
        )
    }

    // ── onUpgrade — tambah kolom qr_content jika belum ada ───────────────────
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_BOOK ADD COLUMN $COL_B_QR TEXT")
            } catch (e: Exception) {
                // kolom sudah ada — abaikan
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  USER METHODS
    // ═════════════════════════════════════════════════════════════════════════

    fun insertUser(user: User): Long {
        val cv = ContentValues().apply {
            put(COL_U_NAME,  user.name)
            put(COL_U_EMAIL, user.email)
            put(COL_U_PASS,  user.password)
            put(COL_U_ROLE,  user.role)
        }
        return writableDatabase.insert(TABLE_USERS, null, cv)
    }

    fun getUserByEmailAndPassword(email: String, password: String): User? {
        val c = readableDatabase.query(
            TABLE_USERS, null,
            "$COL_U_EMAIL=? AND $COL_U_PASS=?",
            arrayOf(email, password),
            null, null, null
        )
        return c.use {
            if (it.moveToFirst()) User(
                id       = it.getString(it.getColumnIndexOrThrow(COL_U_ID)),
                name     = it.getString(it.getColumnIndexOrThrow(COL_U_NAME)),
                email    = it.getString(it.getColumnIndexOrThrow(COL_U_EMAIL)),
                password = it.getString(it.getColumnIndexOrThrow(COL_U_PASS)),
                role     = it.getString(it.getColumnIndexOrThrow(COL_U_ROLE))
            ) else null
        }
    }

    fun getAllUsers(): List<User> {
        val list = mutableListOf<User>()
        val c = readableDatabase.query(TABLE_USERS, null, null, null, null, null, "$COL_U_NAME ASC")
        c.use {
            while (it.moveToNext()) list.add(
                User(
                    id       = it.getString(it.getColumnIndexOrThrow(COL_U_ID)),
                    name     = it.getString(it.getColumnIndexOrThrow(COL_U_NAME)),
                    email    = it.getString(it.getColumnIndexOrThrow(COL_U_EMAIL)),
                    password = it.getString(it.getColumnIndexOrThrow(COL_U_PASS)),
                    role     = it.getString(it.getColumnIndexOrThrow(COL_U_ROLE))
                )
            )
        }
        return list
    }

    fun getAllReaders(): List<User> {
        val list = mutableListOf<User>()
        val c = readableDatabase.query(
            TABLE_USERS, null,
            "$COL_U_ROLE=?", arrayOf("reader"),
            null, null, "$COL_U_NAME ASC"
        )
        c.use {
            while (it.moveToNext()) list.add(
                User(
                    id       = it.getString(it.getColumnIndexOrThrow(COL_U_ID)),
                    name     = it.getString(it.getColumnIndexOrThrow(COL_U_NAME)),
                    email    = it.getString(it.getColumnIndexOrThrow(COL_U_EMAIL)),
                    password = it.getString(it.getColumnIndexOrThrow(COL_U_PASS)),
                    role     = it.getString(it.getColumnIndexOrThrow(COL_U_ROLE))
                )
            )
        }
        return list
    }

    fun updateUser(user: User): Int {
        val cv = ContentValues().apply {
            put(COL_U_NAME,  user.name)
            put(COL_U_EMAIL, user.email)
            put(COL_U_PASS,  user.password)
            put(COL_U_ROLE,  user.role)
        }
        return writableDatabase.update(TABLE_USERS, cv, "$COL_U_ID=?", arrayOf(user.id))
    }

    fun deleteUser(userId: String): Int =
        writableDatabase.delete(TABLE_USERS, "$COL_U_ID=?", arrayOf(userId))

    fun countUsersByRole(role: String): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_USERS WHERE $COL_U_ROLE=?", arrayOf(role)
        )
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TAROT PACKAGE METHODS
    // ═════════════════════════════════════════════════════════════════════════

    fun insertTarotPackage(pkg: TarotPackage): Long {
        val cv = ContentValues().apply {
            put(COL_P_NAME,  pkg.name)
            put(COL_P_CAT,   pkg.category)
            put(COL_P_PRICE, pkg.price)
        }
        return writableDatabase.insert(TABLE_PKGS, null, cv)
    }

    fun getAllTarotPackages(): List<TarotPackage> {
        val list = mutableListOf<TarotPackage>()
        val c = readableDatabase.query(TABLE_PKGS, null, null, null, null, null, "$COL_P_NAME ASC")
        c.use {
            while (it.moveToNext()) list.add(
                TarotPackage(
                    id       = it.getString(it.getColumnIndexOrThrow(COL_P_ID)),
                    name     = it.getString(it.getColumnIndexOrThrow(COL_P_NAME)),
                    category = it.getString(it.getColumnIndexOrThrow(COL_P_CAT)),
                    price    = it.getInt(it.getColumnIndexOrThrow(COL_P_PRICE))
                )
            )
        }
        return list
    }

    fun updateTarotPackage(pkg: TarotPackage): Int {
        val cv = ContentValues().apply {
            put(COL_P_NAME,  pkg.name)
            put(COL_P_CAT,   pkg.category)
            put(COL_P_PRICE, pkg.price)
        }
        return writableDatabase.update(TABLE_PKGS, cv, "$COL_P_ID=?", arrayOf(pkg.id))
    }

    fun deleteTarotPackage(pkgId: String): Int =
        writableDatabase.delete(TABLE_PKGS, "$COL_P_ID=?", arrayOf(pkgId))

    // ═════════════════════════════════════════════════════════════════════════
    //  BOOKING METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /** Insert booking baru. Kembalikan row ID (> 0 jika sukses). */
    fun insertBooking(booking: Booking): Long {
        val cv = ContentValues().apply {
            put(COL_B_CUST_ID,     booking.customerId)
            put(COL_B_CUST_NAME,   booking.customerName)
            put(COL_B_READER_ID,   booking.readerId)
            put(COL_B_READER_NAME, booking.readerName)
            put(COL_B_PKG_ID,      booking.packageId)
            put(COL_B_PKG_NAME,    booking.packageName)
            put(COL_B_PAYMENT,     booking.paymentMethod)
            put(COL_B_PRICE,       booking.totalPrice)
            put(COL_B_DATE,        booking.date)
            put(COL_B_TIME,        booking.time)
            put(COL_B_NOTES,       booking.notes)
            put(COL_B_STATUS,      booking.status)
            put(COL_B_QR,          booking.qrContent)
        }
        return writableDatabase.insert(TABLE_BOOK, null, cv)
    }

    /** Simpan / update qr_content setelah booking di-insert. */
    fun updateBookingQr(bookingId: String, qrContent: String) {
        val cv = ContentValues().apply { put(COL_B_QR, qrContent) }
        writableDatabase.update(TABLE_BOOK, cv, "$COL_B_ID=?", arrayOf(bookingId))
    }

    /** Ambil qr_content dari satu booking. */
    fun getQrContent(bookingId: String): String? {
        val c = readableDatabase.query(
            TABLE_BOOK, arrayOf(COL_B_QR),
            "$COL_B_ID=?", arrayOf(bookingId),
            null, null, null
        )
        return c.use { if (it.moveToFirst()) it.getString(0) else null }
    }

    /** Ambil satu booking berdasarkan ID. */
    fun getBookingById(bookingId: String): Booking? {
        val c = readableDatabase.query(
            TABLE_BOOK, null,
            "$COL_B_ID=?", arrayOf(bookingId),
            null, null, null
        )
        return c.use { if (it.moveToFirst()) cursorToBooking(it) else null }
    }

    /** Semua booking milik customer tertentu, urut terbaru. */
    fun getBookingsByCustomer(customerId: String): List<Booking> {
        val list = mutableListOf<Booking>()
        val c = readableDatabase.query(
            TABLE_BOOK, null,
            "$COL_B_CUST_ID=?", arrayOf(customerId),
            null, null, "$COL_B_DATE DESC, $COL_B_TIME DESC"
        )
        c.use { while (it.moveToNext()) list.add(cursorToBooking(it)) }
        return list
    }

    /** Semua booking milik reader tertentu. */
    fun getBookingsByReader(readerId: String): List<Booking> {
        val list = mutableListOf<Booking>()
        val c = readableDatabase.query(
            TABLE_BOOK, null,
            "$COL_B_READER_ID=?", arrayOf(readerId),
            null, null, "$COL_B_DATE ASC, $COL_B_TIME ASC"
        )
        c.use { while (it.moveToNext()) list.add(cursorToBooking(it)) }
        return list
    }

    /** Semua booking (untuk admin). */
    fun getAllBookings(): List<Booking> {
        val list = mutableListOf<Booking>()
        val c = readableDatabase.query(
            TABLE_BOOK, null, null, null, null, null,
            "$COL_B_DATE DESC, $COL_B_TIME DESC"
        )
        c.use { while (it.moveToNext()) list.add(cursorToBooking(it)) }
        return list
    }

    /** Next booking PENDING untuk reader. */
    fun getNextBookingForReader(readerId: String): Booking? {
        val c = readableDatabase.query(
            TABLE_BOOK, null,
            "$COL_B_READER_ID=? AND $COL_B_STATUS IN ('PENDING','PAID','CONFIRMED')",
            arrayOf(readerId),
            null, null,
            "$COL_B_DATE ASC, $COL_B_TIME ASC",
            "1"
        )
        return c.use { if (it.moveToFirst()) cursorToBooking(it) else null }
    }

    fun updateBookingStatus(bookingId: String, status: String) {
        val cv = ContentValues().apply { put(COL_B_STATUS, status) }
        writableDatabase.update(TABLE_BOOK, cv, "$COL_B_ID=?", arrayOf(bookingId))
    }

    fun updateBookingAnswer(bookingId: String, answer: String) {
        val cv = ContentValues().apply { put(COL_B_ANSWER, answer) }
        writableDatabase.update(TABLE_BOOK, cv, "$COL_B_ID=?", arrayOf(bookingId))
    }

    fun deleteBooking(bookingId: String): Int =
        writableDatabase.delete(TABLE_BOOK, "$COL_B_ID=?", arrayOf(bookingId))

    fun countBookingsByReaderAndStatus(readerId: String, status: String): Int {
        val c = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_BOOK WHERE $COL_B_READER_ID=? AND $COL_B_STATUS=?",
            arrayOf(readerId, status)
        )
        return c.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getTotalRevenue(): Long {
        val c = readableDatabase.rawQuery(
            "SELECT SUM($COL_B_PRICE) FROM $TABLE_BOOK WHERE $COL_B_STATUS='DONE'", null
        )
        return c.use { if (it.moveToFirst()) it.getLong(0) else 0L }
    }

    fun getTopPackage(): String {
        val c = readableDatabase.rawQuery(
            """SELECT $COL_B_PKG_NAME, COUNT(*) AS cnt
               FROM $TABLE_BOOK
               WHERE $COL_B_STATUS='DONE'
               GROUP BY $COL_B_PKG_NAME
               ORDER BY cnt DESC
               LIMIT 1""", null
        )
        return c.use {
            if (it.moveToFirst()) it.getString(0) else "-"
        }
    }

    // ── cursor helper ─────────────────────────────────────────────────────────
    private fun cursorToBooking(c: android.database.Cursor): Booking = Booking(
        id            = c.getString(c.getColumnIndexOrThrow(COL_B_ID)),
        customerId    = c.getString(c.getColumnIndexOrThrow(COL_B_CUST_ID))    ?: "",
        customerName  = c.getString(c.getColumnIndexOrThrow(COL_B_CUST_NAME))  ?: "",
        readerId      = c.getString(c.getColumnIndexOrThrow(COL_B_READER_ID))  ?: "",
        readerName    = c.getString(c.getColumnIndexOrThrow(COL_B_READER_NAME))?: "",
        packageId     = c.getString(c.getColumnIndexOrThrow(COL_B_PKG_ID))     ?: "",
        packageName   = c.getString(c.getColumnIndexOrThrow(COL_B_PKG_NAME))   ?: "",
        paymentMethod = c.getString(c.getColumnIndexOrThrow(COL_B_PAYMENT))    ?: "",
        totalPrice    = c.getInt(c.getColumnIndexOrThrow(COL_B_PRICE)),
        date          = c.getString(c.getColumnIndexOrThrow(COL_B_DATE))       ?: "",
        time          = c.getString(c.getColumnIndexOrThrow(COL_B_TIME))       ?: "",
        notes         = c.getString(c.getColumnIndexOrThrow(COL_B_NOTES))      ?: "",
        status        = c.getString(c.getColumnIndexOrThrow(COL_B_STATUS))     ?: "PENDING",
        answer        = c.getString(c.getColumnIndexOrThrow(COL_B_ANSWER))     ?: "",
        qrContent     = c.getString(c.getColumnIndexOrThrow(COL_B_QR))        ?: ""
    )

    // ═════════════════════════════════════════════════════════════════════════
    //  TESTIMONIAL METHODS
    // ═════════════════════════════════════════════════════════════════════════

    fun insertTestimoni(
        bookingId: String, customerId: String, customerName: String,
        packageName: String, rating: Int, message: String
    ): Long {
        val cv = ContentValues().apply {
            put(COL_T_BOOK_ID, bookingId)
            put(COL_T_CUST_ID, customerId)
            put(COL_T_CUST_NM, customerName)
            put(COL_T_PKG_NM,  packageName)
            put(COL_T_RATING,  rating)
            put(COL_T_MSG,     message)
        }
        return writableDatabase.insert(TABLE_TESTI, null, cv)
    }

    fun getAllTestimoni(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val c = readableDatabase.query(
            TABLE_TESTI, null, null, null, null, null, "$COL_T_CREATED DESC"
        )
        c.use {
            while (it.moveToNext()) {
                list.add(
                    mapOf(
                        "id"            to it.getString(it.getColumnIndexOrThrow(COL_T_ID)),
                        "customer_name" to (it.getString(it.getColumnIndexOrThrow(COL_T_CUST_NM)) ?: ""),
                        "package_name"  to (it.getString(it.getColumnIndexOrThrow(COL_T_PKG_NM))  ?: ""),
                        "rating"        to it.getInt(it.getColumnIndexOrThrow(COL_T_RATING)).toString(),
                        "message"       to (it.getString(it.getColumnIndexOrThrow(COL_T_MSG))      ?: ""),
                        "created_at"    to (it.getString(it.getColumnIndexOrThrow(COL_T_CREATED))  ?: "")
                    )
                )
            }
        }
        return list
    }

    fun deleteTestimoni(testimoniId: String): Int =
        writableDatabase.delete(TABLE_TESTI, "$COL_T_ID=?", arrayOf(testimoniId))

    fun getTestimoniByBookingId(bookingId: String): Map<String, String>? {
        val c = readableDatabase.query(
            TABLE_TESTI, null,
            "$COL_T_BOOK_ID=?", arrayOf(bookingId),
            null, null, null
        )
        return c.use {
            if (it.moveToFirst()) mapOf(
                "rating"  to it.getInt(it.getColumnIndexOrThrow(COL_T_RATING)).toString(),
                "message" to (it.getString(it.getColumnIndexOrThrow(COL_T_MSG)) ?: "")
            ) else null
        }
    }
}