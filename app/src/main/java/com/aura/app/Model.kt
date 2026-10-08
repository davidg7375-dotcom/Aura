package com.aura.app

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// ---------- מודלים (Kotlin טהור, ללא תלות באנדרואיד) ----------

data class Payer(val id: String, val name: String)

/**
 * פגישה = יחידת חיוב נפרדת. מתעדים רק:
 *  1. האם שולמה ([paid]).
 *  2. אם כן – מי בפועל שילם ([paidById]). פגישה שלא שולמה אינה מכילה "שולם על ידי".
 *
 * אין מושג של "מי אמור לשלם". החובות מחושבים אוטומטית מחלוקה שווה בין כל האנשים.
 */
data class Session(
    val id: String,
    val date: LocalDate,
    val time: LocalTime,
    val price: Int,
    val paid: Boolean = false,
    val paidDate: LocalDate? = null,
    val paidById: String? = null,
    val notes: String = "",
    val remindBeforeMin: Int? = null,
    val remindPay: Boolean = false,
    val customRemindAt: Long? = null
) {
    val dateTime: LocalDateTime get() = LocalDateTime.of(date, time)
}

/** חוב ידני: סכום שהוזן ידנית, לא קשור לפגישות ולא לחלוקה האוטומטית. */
data class ManualDebt(
    val id: String,
    val to: String,
    val amount: Int,
    val reason: String = "",
    val paid: Boolean = false,
    val createdDate: LocalDate,
    val paidDate: LocalDate? = null
)

enum class PayStatus { Paid, Due, Soon }

/** כמות בפגישות כשבר מדויק (שקלים ÷ מחיר פגישה). */
data class Units(val num: Long, val den: Long) {
    val value: Double get() = num.toDouble() / den
    /** פחות מ־2% מפגישה נחשב אפס. */
    val isZero: Boolean get() = abs(value) < 0.02
    operator fun unaryMinus(): Units = Units(-num, den)
    fun absolute(): Units = Units(kotlin.math.abs(num), den)
}

enum class BalanceKind { Owes, Credit, Even }

/**
 * מאזן של אדם בחלוקה שווה.
 * [share] – החלק שלו מכלל העלות; [paid] – מה ששילם בפועל; [amount] – ההפרש (תמיד חיובי);
 * [money] – ההפרש בשקלים עם סימן (חיובי = חייב, שלילי = זכות), להצגה משנית בלבד.
 */
data class Balance(
    val kind: BalanceKind,
    val amount: Units,
    val share: Units,
    val paid: Units,
    val money: Double
)

data class PayerStats(val paidCount: Int, val paidSum: Int, val balance: Balance)

/**
 * כל חישובי התשלומים והחובות במקום אחד.
 *
 * המודל: כל הפגישות ש"נספרות" (כבר התקיימו, או כבר שולמו) מחולקות שווה בין כל האנשים.
 * מאזן של אדם = החלק שלו פחות מה ששילם בפועל. חיובי = חייב, שלילי = זכות.
 */
object Ledger {
    fun isDue(s: Session, now: LocalDateTime): Boolean = !s.paid && !s.dateTime.isAfter(now)

    fun status(s: Session, now: LocalDateTime): PayStatus = when {
        s.paid -> PayStatus.Paid
        isDue(s, now) -> PayStatus.Due
        else -> PayStatus.Soon
    }

    /** פגישות שכבר התקיימו ולא שולמו, מהחדשה לישנה. */
    fun dueSessions(sessions: List<Session>, now: LocalDateTime): List<Session> =
        sessions.filter { isDue(it, now) }.sortedByDescending { it.dateTime }

    /** סכום הפגישות שכבר התקיימו ולא שולמו. */
    fun unpaidSum(sessions: List<Session>, now: LocalDateTime): Int =
        dueSessions(sessions, now).sumOf { it.price }

    /** פגישות שנכנסות לחלוקה: שהתקיימו או ששולמו מראש. פגישה עתידית שלא שולמה עדיין לא נספרת. */
    fun costedSessions(sessions: List<Session>, now: LocalDateTime): List<Session> =
        sessions.filter { it.paid || !it.dateTime.isAfter(now) }

    fun totalCost(sessions: List<Session>, now: LocalDateTime): Int =
        costedSessions(sessions, now).sumOf { it.price }

    /** שקלים ← פגישות (לפי מחיר פגישה סטנדרטי). */
    fun units(money: Int, unitPrice: Int): Units = Units(money.toLong(), unitPrice.coerceAtLeast(1).toLong())

    /** המאזן של אדם אחד בחלוקה שווה בין [personIds]. */
    fun balance(
        sessions: List<Session>,
        personIds: List<String>,
        personId: String,
        unitPrice: Int,
        now: LocalDateTime
    ): Balance {
        val n = personIds.size.coerceAtLeast(1)
        val unit = unitPrice.coerceAtLeast(1).toLong()
        val total = totalCost(sessions, now).toLong()
        val paidSum = sessions.filter { it.paid && it.paidById == personId }.sumOf { it.price }.toLong()
        // חלק = total/n ; הפרש = total/n - paid = (total - n*paid)/n  (בשקלים), ובפגישות: חלקי unit
        val diff = Units(total - n * paidSum, n * unit)
        val kind = when {
            diff.isZero -> BalanceKind.Even
            diff.num > 0 -> BalanceKind.Owes
            else -> BalanceKind.Credit
        }
        return Balance(
            kind = kind,
            amount = diff.absolute(),
            share = Units(total, n * unit),
            paid = Units(paidSum, unit),
            money = (total - n * paidSum).toDouble() / n
        )
    }

    fun stats(
        sessions: List<Session>,
        personIds: List<String>,
        personId: String,
        unitPrice: Int,
        now: LocalDateTime
    ): PayerStats {
        val paidBy = sessions.filter { it.paid && it.paidById == personId }
        return PayerStats(
            paidCount = paidBy.size,
            paidSum = paidBy.sumOf { it.price },
            balance = balance(sessions, personIds, personId, unitPrice, now)
        )
    }

    /** סימון כמה פגישות כשולמו על ידי אדם אחד. כל פגישה נשארת נפרדת עם המחיר שלה. */
    fun markPaid(sessions: List<Session>, ids: Set<String>, byId: String, date: LocalDate): List<Session> =
        sessions.map {
            if (it.id in ids) it.copy(paid = true, paidDate = date, paidById = byId) else it
        }

    /** החזרת פגישה למצב "לא שולם": אין יותר "שולם על ידי". */
    fun markUnpaid(sessions: List<Session>, id: String): List<Session> =
        sessions.map { if (it.id == id) it.copy(paid = false, paidDate = null, paidById = null) else it }
}

// ---------- הצגת כמויות כשברים פשוטים: ½ ⅓ ¼ ¾ ----------

private val fractionGlyphs: Map<Pair<Int, Int>, String> = mapOf(
    Pair(1, 2) to "½",
    Pair(1, 3) to "⅓", Pair(2, 3) to "⅔",
    Pair(1, 4) to "¼", Pair(3, 4) to "¾",
    Pair(1, 5) to "⅕", Pair(2, 5) to "⅖", Pair(3, 5) to "⅗", Pair(4, 5) to "⅘",
    Pair(1, 6) to "⅙", Pair(5, 6) to "⅚",
    Pair(1, 8) to "⅛", Pair(3, 8) to "⅜", Pair(5, 8) to "⅝", Pair(7, 8) to "⅞"
)
private val fractionDenominators = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12)

private fun gcdInt(a: Int, b: Int): Int = if (b == 0) a else gcdInt(b, a % b)

/** (שלם, מונה, מכנה) בשבר הפשוט הקרוב ביותר, או null אם אין שבר נקי. */
private fun nearestFraction(x: Double): Triple<Int, Int, Int>? {
    for (d in fractionDenominators) {
        val n = (x * d).roundToInt()
        if (abs(x - n.toDouble() / d) <= 0.02) {
            val whole = n / d
            var num = n % d
            var den = d
            if (num != 0) {
                val g = gcdInt(num, den)
                num /= g
                den /= g
            } else {
                den = 1
            }
            return Triple(whole, num, den)
        }
    }
    return null
}

/** מספר קומפקטי בלי המילה "פגישות": "1⅓", "2", "½", "0.37". */
fun sessionsAmount(u: Units): String {
    val x = abs(u.value)
    val f = nearestFraction(x)
    if (f == null) {
        val dec = floor(x * 100 + 0.5) / 100
        return if (dec == floor(dec)) dec.toInt().toString() else String.format(java.util.Locale.US, "%.2f", dec)
    }
    val frac = if (f.second == 0) "" else (fractionGlyphs[Pair(f.second, f.third)] ?: "${f.second}/${f.third}")
    return if (f.first == 0 && f.second == 0) "0" else if (f.first == 0) frac else "${f.first}$frac"
}

/** ביטוי עם יחידה: "פגישה אחת", "2 פגישות", "½ פגישה", "1⅓ פגישות". */
fun sessionsPhrase(u: Units): String {
    val x = abs(u.value)
    val f = nearestFraction(x)
    val amount = sessionsAmount(u)
    return when {
        f != null && f.first == 1 && f.second == 0 -> "פגישה אחת"
        (f != null && f.first == 0) || (f == null && x < 1.0) -> "$amount פגישה"
        else -> "$amount פגישות"
    }
}

/** "חייב ⅓ פגישה" / "זכות: ½ פגישה" / "אין חוב". */
fun balanceText(b: Balance): String = when (b.kind) {
    BalanceKind.Owes -> "חייב ${sessionsPhrase(b.amount)}"
    BalanceKind.Credit -> "זכות: ${sessionsPhrase(b.amount)}"
    BalanceKind.Even -> "אין חוב"
}
