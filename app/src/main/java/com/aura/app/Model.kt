package com.aura.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------- מודלים (Kotlin טהור, ללא תלות באנדרואיד) ----------

data class Payer(val id: String, val name: String)

data class Session(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val price: Int,
    val payerId: String,
    val paid: Boolean = false,
    val paidDate: LocalDate? = null,
    val notes: String = "",
    val remindBeforeMin: Int? = null,
    val remindPay: Boolean = false,
    val customRemindAt: Long? = null
) {
    val dateTime: LocalDateTime get() = LocalDateTime.of(date, time)
}

enum class PayStatus { Paid, Due, Soon }

data class PayerStats(val total: Int, val paid: Int, val due: Int, val soon: Int, val debt: Int)

/**
 * כל חישובי התשלומים והחובות במקום אחד.
 * מודל: לכל פגישה יש סטטוס תשלום ואדם אחד ([Session.payerId]) –
 * אם שולמה, זה מי ששילם עליה; אם לא, זה האדם שהחוב רשום עליו.
 */
object Ledger {
    fun isDue(s: Session, now: LocalDateTime): Boolean = !s.paid && !s.dateTime.isAfter(now)

    fun status(s: Session, now: LocalDateTime): PayStatus = when {
        s.paid -> PayStatus.Paid
        isDue(s, now) -> PayStatus.Due
        else -> PayStatus.Soon
    }

    /** פגישות שכבר התקיימו ולא שולמו, מהחדשה לישנה. */
    fun dueSessions(sessions: List<Session>, payerId: String?, now: LocalDateTime): List<Session> =
        sessions
            .filter { isDue(it, now) && (payerId == null || it.payerId == payerId) }
            .sortedByDescending { it.dateTime }

    fun debtOf(sessions: List<Session>, payerId: String, now: LocalDateTime): Int =
        dueSessions(sessions, payerId, now).sumOf { it.price }

    fun totalDebt(sessions: List<Session>, now: LocalDateTime): Int =
        dueSessions(sessions, null, now).sumOf { it.price }

    /** סימון כמה פגישות כשולמו על ידי אדם אחד. כל פגישה נשארת נפרדת עם המחיר שלה. */
    fun markPaid(sessions: List<Session>, ids: Set<String>, byId: String, date: LocalDate): List<Session> =
        sessions.map { if (it.id in ids) it.copy(paid = true, paidDate = date, payerId = byId) else it }

    fun stats(sessions: List<Session>, payerId: String, now: LocalDateTime): PayerStats {
        val mine = sessions.filter { it.payerId == payerId }
        val due = mine.filter { isDue(it, now) }
        return PayerStats(
            total = mine.size,
            paid = mine.count { it.paid },
            due = due.size,
            soon = mine.count { !it.paid && !isDue(it, now) },
            debt = due.sumOf { it.price }
        )
    }
}
