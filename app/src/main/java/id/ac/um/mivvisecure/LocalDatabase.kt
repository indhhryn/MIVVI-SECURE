package id.ac.um.mivvisecure

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AnalysisRecord(
    val id: Long,
    val source: String,
    val sender: String,
    val body: String,
    val receivedAt: Long,
    val score: Int,
    val category: String,
    val featuresJson: String,
    val isDemo: Boolean,
    val packageName: String
)

class LocalDatabase(context: Context) : SQLiteOpenHelper(context, "mivvi_secure.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE analyses (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            source TEXT NOT NULL,
            sender TEXT NOT NULL DEFAULT '',
            body TEXT NOT NULL DEFAULT '',
            received_at INTEGER NOT NULL,
            score INTEGER NOT NULL DEFAULT 0,
            category TEXT NOT NULL DEFAULT 'TIDAK TERSEDIA',
            features_json TEXT NOT NULL DEFAULT '[]',
            is_demo INTEGER NOT NULL DEFAULT 0,
            package_name TEXT NOT NULL DEFAULT ''
        )""".trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 is the initial schema. Add explicit migration blocks when the schema changes.
    }

    fun insert(source: String, sender: String, body: String, receivedAt: Long,
               result: AnalysisResult?, isDemo: Boolean = false, packageName: String = ""): Long {
        val values = ContentValues().apply {
            put("source", source)
            put("sender", sender)
            put("body", body)
            put("received_at", receivedAt)
            put("score", result?.score ?: 0)
            put("category", result?.category ?: "KONTEN TIDAK TERSEDIA")
            put("features_json", result?.features?.joinToString("\u001f") {
                listOf(it.id, it.label, it.detected.toString(), it.points.toString(), it.explanation).joinToString("\u001e")
            } ?: "")
            put("is_demo", if (isDemo) 1 else 0)
            put("package_name", packageName)
        }
        return writableDatabase.insert("analyses", null, values)
    }

    fun recent(limit: Int = 100): List<AnalysisRecord> {
        val result = mutableListOf<AnalysisRecord>()
        readableDatabase.query("analyses", null, null, null, null, null, "received_at DESC", limit.toString()).use { c ->
            while (c.moveToNext()) {
                result += AnalysisRecord(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("source")),
                    c.getString(c.getColumnIndexOrThrow("sender")),
                    c.getString(c.getColumnIndexOrThrow("body")),
                    c.getLong(c.getColumnIndexOrThrow("received_at")),
                    c.getInt(c.getColumnIndexOrThrow("score")),
                    c.getString(c.getColumnIndexOrThrow("category")),
                    c.getString(c.getColumnIndexOrThrow("features_json")),
                    c.getInt(c.getColumnIndexOrThrow("is_demo")) == 1,
                    c.getString(c.getColumnIndexOrThrow("package_name"))
                )
            }
        }
        return result
    }

    fun queryFiltered(source: String? = null, category: String? = null, query: String? = null, limit: Int = 100): List<AnalysisRecord> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (!source.isNullOrBlank()) {
            conditions += "source = ?"
            args += source
        }
        if (!category.isNullOrBlank()) {
            if (category == "PERLU_DIPERIKSA") {
                conditions += "(category = 'MENCURIGAKAN' OR category = 'PHISHING')"
            } else {
                conditions += "category = ?"
                args += category
            }
        }
        if (!query.isNullOrBlank()) {
            conditions += "(sender LIKE ? OR body LIKE ?)"
            args += "%$query%"
            args += "%$query%"
        }

        val selection = if (conditions.isNotEmpty()) conditions.joinToString(" AND ") else null
        val selectionArgs = if (args.isNotEmpty()) args.toTypedArray() else null

        val result = mutableListOf<AnalysisRecord>()
        readableDatabase.query("analyses", null, selection, selectionArgs, null, null, "received_at DESC", limit.toString()).use { c ->
            while (c.moveToNext()) {
                result += AnalysisRecord(
                    c.getLong(c.getColumnIndexOrThrow("id")),
                    c.getString(c.getColumnIndexOrThrow("source")),
                    c.getString(c.getColumnIndexOrThrow("sender")),
                    c.getString(c.getColumnIndexOrThrow("body")),
                    c.getLong(c.getColumnIndexOrThrow("received_at")),
                    c.getInt(c.getColumnIndexOrThrow("score")),
                    c.getString(c.getColumnIndexOrThrow("category")),
                    c.getString(c.getColumnIndexOrThrow("features_json")),
                    c.getInt(c.getColumnIndexOrThrow("is_demo")) == 1,
                    c.getString(c.getColumnIndexOrThrow("package_name"))
                )
            }
        }
        return result
    }

    fun count(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM analyses", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    fun highRiskCount(): Int = readableDatabase.rawQuery("SELECT COUNT(*) FROM analyses WHERE score >= 6", null).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }
    fun delete(id: Long): Int = writableDatabase.delete("analyses", "id=?", arrayOf(id.toString()))
    fun clear(): Int = writableDatabase.delete("analyses", null, null)
}
