package putra.yanuar.tarot

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBOpenHelper(context: Context) :
    SQLiteOpenHelper(context, name, null, version) {

    override fun onCreate(db: SQLiteDatabase?) {
        // 1. Tabel Users (Hanya kolom inti untuk Login)
        val sqlUsers = "create table users(" +
                "id integer primary key autoincrement, " +
                "name text not null, " +
                "email text unique not null, " +
                "role text not null, " +
                "password text not null)"

        // 2. Data Testing (Langsung pakai teks biasa)
        val insertAdmin = "insert into users(name, email, role, password) " +
                "values ('Admin Tarot', 'admin@meow.com', 'admin', 'coba123')"

        val insertReader = "insert into users(name, email, role, password) " +
                "values ('Reader Tarot', 'reader@meow.com', 'reader', 'coba123')"

        val insertCustomer = "insert into users(name, email, role, password) " +
                "values ('Customer Tarot', 'customer@meow.com', 'customer', 'coba123')"

        // Eksekusi SQL
        db?.execSQL(sqlUsers)
        db?.execSQL(insertAdmin)
        db?.execSQL(insertReader)
        db?.execSQL(insertCustomer)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("drop table if exists users")
        onCreate(db)
    }

    companion object {
        private const val name = "tarot.db"
        private const val version = 1
    }
}