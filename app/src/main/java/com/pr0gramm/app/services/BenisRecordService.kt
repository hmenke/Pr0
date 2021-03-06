package com.pr0gramm.app.services

import android.database.sqlite.SQLiteDatabase
import com.pr0gramm.app.Instant
import com.pr0gramm.app.TimeFactory
import com.pr0gramm.app.orm.BenisRecord
import com.pr0gramm.app.ui.base.withBackgroundContext
import com.pr0gramm.app.util.Holder
import com.pr0gramm.app.util.arrayOfStrings
import com.pr0gramm.app.util.checkNotMainThread
import com.pr0gramm.app.util.mapToList

class BenisRecordService(private val database: Holder<SQLiteDatabase>) {
    suspend fun findValuesLaterThan(ownerId: Int, time: Instant): List<BenisRecord> = withBackgroundContext {
        val columns = arrayOf("time", "benis")

        database.get().query("benis_record", columns,
                "time >= ? and owner_id=?", arrayOfStrings(time.millis, ownerId),
                null, null, "time ASC").use { cursor ->

            cursor.mapToList { BenisRecord(getLong(0), getInt(1)) }
        }
    }

    suspend fun storeValue(ownerId: Int, benis: Int) {
        checkNotMainThread()

        val sql = "INSERT INTO benis_record (owner_id, time, benis) VALUES (?, ?, ?)"
        database.get().execSQL(sql, arrayOfStrings(ownerId, TimeFactory.currentTimeMillis(), benis))
    }
}