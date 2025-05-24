package com.supernova.wheresmydevice.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class WhitelistEntry(
    val id: String = UUID.randomUUID().toString(),
    val phoneNumber: String,
    val name: String = ""
)

class WhitelistDao(context: Context) {
    private val dbHelper = WhitelistDbHelper(context)
    
    fun addEntry(entry: WhitelistEntry) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WhitelistDbHelper.COLUMN_ID, entry.id)
            put(WhitelistDbHelper.COLUMN_PHONE_NUMBER, entry.phoneNumber)
            put(WhitelistDbHelper.COLUMN_NAME, entry.name)
        }
        
        db.insert(WhitelistDbHelper.TABLE_NAME, null, values)
        db.close()
    }
    
    fun getAllEntries(): List<WhitelistEntry> {
        val entries = mutableListOf<WhitelistEntry>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            WhitelistDbHelper.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            null
        )
        
        with(cursor) {
            while (moveToNext()) {
                val idIndex = getColumnIndex(WhitelistDbHelper.COLUMN_ID)
                val phoneNumberIndex = getColumnIndex(WhitelistDbHelper.COLUMN_PHONE_NUMBER)
                val nameIndex = getColumnIndex(WhitelistDbHelper.COLUMN_NAME)
                
                val id = getString(idIndex)
                val phoneNumber = getString(phoneNumberIndex)
                val name = if (nameIndex >= 0) getString(nameIndex) else ""
                
                entries.add(WhitelistEntry(id, phoneNumber, name))
            }
        }
        
        cursor.close()
        db.close()
        return entries
    }
    
    fun deleteEntry(id: String) {
        val db = dbHelper.writableDatabase
        db.delete(
            WhitelistDbHelper.TABLE_NAME,
            "${WhitelistDbHelper.COLUMN_ID} = ?",
            arrayOf(id)
        )
        db.close()
    }
    
    fun isPhoneNumberWhitelisted(phoneNumber: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            WhitelistDbHelper.TABLE_NAME,
            null,
            "${WhitelistDbHelper.COLUMN_PHONE_NUMBER} = ?",
            arrayOf(phoneNumber),
            null,
            null,
            null
        )
        
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }
    
    private class WhitelistDbHelper(context: Context) : SQLiteOpenHelper(
        context,
        DATABASE_NAME,
        null,
        DATABASE_VERSION
    ) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID TEXT PRIMARY KEY, " +
                "$COLUMN_PHONE_NUMBER TEXT UNIQUE, " +
                "$COLUMN_NAME TEXT)"
            )
        }
        
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                try {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_NAME TEXT DEFAULT ''")
                } catch (e: Exception) {
                    db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
                    onCreate(db)
                }
            }
        }
        
        companion object {
            const val DATABASE_VERSION = 2
            const val DATABASE_NAME = "WhitelistEntries.db"
            const val TABLE_NAME = "whitelist"
            const val COLUMN_ID = "id"
            const val COLUMN_PHONE_NUMBER = "phone_number"
            const val COLUMN_NAME = "name"
        }
    }
} 