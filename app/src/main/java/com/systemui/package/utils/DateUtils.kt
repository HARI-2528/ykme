package com.systemui.package.utils

import java.time.Instant, LocalDate, LocalDateTime, ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    val IST = ZoneId.of("Asia/Kolkata")

    fun epochToISTDateTime(epochMs: Long): LocalDateTime = 
        Instant.ofEpochMilli(epochMs).atZone(IST).toLocalDateTime()

    fun epochToISTDate(epochMs: Long): LocalDate = epochToISTDateTime(epochMs).toLocalDate()

    fun todayIST(): LocalDate = LocalDate.now(IST)
    fun yesterdayIST(): LocalDate = todayIST().minusDays(1)

    fun formatForDisplay(ldt: LocalDateTime): String = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(ldt)

    fun formatTimeOnly(ldt: LocalDateTime): String = 
        DateTimeFormatter.ofPattern("HH:mm").format(ldt)
}