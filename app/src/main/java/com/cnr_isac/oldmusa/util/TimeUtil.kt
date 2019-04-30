package com.cnr_isac.oldmusa.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    val ISO_0_OFFSET_DATE_TIME = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).also {
        it.timeZone = TimeZone.getTimeZone("GMT")
    }

    fun Calendar.copy(): Calendar {
        val new = Calendar.getInstance(timeZone)
        new.time = time
        return new
    }

    fun midnightOf(year: Int, month: Int, day: Int) = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}