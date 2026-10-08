package com.aura.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

// ---------- מודלים (Kotlin טהור, ללא תלות באנדרואיד) ----------

data class Payer(val id: String, val name: String)

/**
 * פגישה = יחידת חיוב נפרדת. מתעדים רק:
 *  1. האם שולמה ([paid]).
 *  2. אם כן – מי בפועל שילם ([paidById]). פגישה שלא שולמה אינה מכילה "שולם על ידי".
 *
 * [debtorId] הוא שיוך חוב לצורך ניהול החובות בלבד (אופציונלי, רק לפגישה שלא שולמה).
 * אין כאן מושג של "מי אמור לשלם".
 */
data class Session(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val price: Int,
    val paid: Boolean = false,
    val paidDate: LocalDate? = null,
    val paidById: String? = null,
    val debtorId: String? = null,
    val notes: String = "",
    val remindBeforeMin: Int? = null,
    val remindPay: Boolean = false,
    val customRemindAt: Long? = null
) {
    val dateTime: LocalDateTime get() = LocalDateTime.of(date, time)
}

enum class PayStatus { Paid, Due, Soon }

/** סיכום לאדם: על כמה פגישות שילם, וחוב פתוח שמשויך אליו. */
data class PayerStats(
    val paidCount: Int,
    val paidSum: Int,
    val dueCount: Int,
    val debt: Int
)

/**
 * כל חישובי התשלומים והחובות במקום אחד.
 * חוב = פגישה שכבר התקיימה ולא שולמה. אדם מקבל חוב רק אם הפגישה משויכת אליו במסך החובות.
 */
object Ledger {
    fun isDue(s: Session, now: LocalDateTime): Boolean = !s.paid && !s.dateTime.isAfter(now)

    fun status(s: Session, now: LocalDateTime): PayStatus = when {
        s.paid -> PayStatus.Paid
        isDue(s, now) -> PayStatus.Due
        else -> PayStatus.Soon
    }

    /** כל הפגישות שלא שולמו (שכבר התקיימו), מהחדשה לישנה. */
    fun dueSessions(sessions: List<Session>, now: LocalDateTime): List<Session> =
        sessions.filter { isDue(it, now) }.sortedByDescending { it.dateTime }

    /** חובות המשויכים לאדם מסוים. */
    fun dueFor(sessions: List<Session>, personId: String, now: LocalDateTime): List<Session> =
        dueSessions(sessions, now).filter { it.debtorId == personId }

    /** חובות שעדיין לא שויכו לאף אדם. */
    fun dueUnassigned(sessions: List<Session>, now: LocalDateTime): List<Session> =
        dueSessions(sessions, now).filter { it.debtorId == null }

    fun debtOf(sessions: List<Session>, personId: String, now: LocalDateTime): Int =
        dueFor(sessions, personId, now).sumOf { it.price }

    fun totalDebt(sessions: List<Session>, now: LocalDateTime): Int =
        dueSessions(sessions, now).sumOf { it.price }

    /** סימון כמה פגישות כשולמו על ידי אדם אחד. כל פגישה נשארת נפרדת עם המחיר שלה. */
    fun markPaid(sessions: List<Session>, ids: Set<String>, byId: String, date: LocalDate): List<Session> =
        sessions.map {
            if (it.id in ids) it.copy(paid = true, paidDate = date, paidById = byId, debtorId = null) else it
        }

    /** החזרת פגישה למצב "לא שולם": אין יותר "שולם על ידי". */
    fun markUnpaid(sessions: List<Session>, id: String): List<Session> =
        sessions.map { if (it.id == id) it.copy(paid = false, paidDate = null, paidById = null) else it }

    /** שיוך חוב לאדם (או הסרת שיוך עם null). משפיע רק על פגישות שלא שולמו. */
    fun assignDebt(sessions: List<Session>, ids: Set<String>, personId: String?): List<Session> =
        sessions.map { if (it.id in ids && !it.paid) it.copy(debtorId = personId) else it }

    fun stats(sessions: List<Session>, personId: String, now: LocalDateTime): PayerStats {
        val paidBy = sessions.filter { it.paid && it.paidById == personId }
        val due = dueFor(sessions, personId, now)
        return PayerStats(
            paidCount = paidBy.size,
            paidSum = paidBy.sumOf { it.price },
            dueCount = due.size,
            debt = due.sumOf { it.price }
        )
    }
}
