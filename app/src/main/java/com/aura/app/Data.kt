package com.aura.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// ---------- עיצוב תאריכים ומחירים בעברית ----------

val HE: Locale = Locale.forLanguageTag("he-IL")

private val fDate = DateTimeFormatter.ofPattern("d MMMM yyyy", HE)
private val fDayMonth = DateTimeFormatter.ofPattern("d MMMM", HE)
private val fWeekday = DateTimeFormatter.ofPattern("EEEE", HE)
private val fMonthYear = DateTimeFormatter.ofPattern("LLLL yyyy", HE)
private val fTime = DateTimeFormatter.ofPattern("HH:mm", HE)

fun fmtDate(d: LocalDate): String = d.format(fDate)
fun fmtDayMonth(d: LocalDate): String = d.format(fDayMonth)
fun fmtWeekday(d: LocalDate): String = d.format(fWeekday)
fun fmtMonthYear(m: YearMonth): String = m.format(fMonthYear)
fun fmtTime(t: LocalTime): String = t.format(fTime)
private val fNumeric = DateTimeFormatter.ofPattern("dd/MM", HE)
private val fNumericYear = DateTimeFormatter.ofPattern("dd/MM/yy", HE)

/** "3 בספטמבר", ועם שנה אם זו לא השנה הנוכחית. */
fun fmtDateShort(d: LocalDate): String = if (d.year == LocalDate.now().year) fmtDayMonth(d) else fmtDate(d)

/** "01/09", ועם שנה אם זו לא השנה הנוכחית. */
fun fmtNumeric(d: LocalDate): String =
    if (d.year == LocalDate.now().year) d.format(fNumeric) else d.format(fNumericYear)

/** שדה אחד בפגישה: מי שילם. לפני התשלום – מי ישלם. */
fun payerRole(paid: Boolean): String = if (paid) "שולם על ידי" else "ישולם על ידי"

fun unpaidCountText(n: Int): String = when (n) {
    1 -> "1 פגישה שלא שולמה"
    else -> "$n פגישות שלא שולמו"
}
fun fmtMoney(n: Int): String = NumberFormat.getIntegerInstance(Locale.US).format(n) + " ₪"

fun beforeLabel(min: Int): String = when (min) {
    30 -> "30 דקות לפני"
    60 -> "שעה לפני"
    180 -> "3 שעות לפני"
    1440 -> "יום לפני"
    else -> "$min דקות לפני"
}

// ---------- מודלים ----------


fun defaultPayers(): List<Payer> = listOf(
    Payer("p_parents", "הורים"),
    Payer("p_yanov", "סבא וסבתא ינוב"),
    Payer("p_ginz", "סבא גינזבורג"),
    Payer("p_me", "אני")
)

// ---------- שמירה לקובץ ----------

object Repo {
    private const val FILE = "aura_data.json"

    data class Snapshot(val payers: List<Payer>, val sessions: List<Session>, val defaultPrice: Int)

    fun load(ctx: Context): Snapshot {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return Snapshot(defaultPayers(), emptyList(), 450)
        return try {
            val o = JSONObject(f.readText())
            val ps = o.getJSONArray("payers")
            val payers = (0 until ps.length()).map { i ->
                val p = ps.getJSONObject(i)
                Payer(p.getString("id"), p.getString("name"))
            }
            val ss = o.getJSONArray("sessions")
            val sessions = (0 until ss.length()).map { i ->
                val j = ss.getJSONObject(i)
                val paid = j.optBoolean("paid", false)
                // מיגרציה: בגרסה קודמת היה גם "מי שילם בפועל" נפרד. עכשיו יש שדה אחד – אם שולם, זה מי ששילם.
                val oldPaidBy = if (!j.has("paidById") || j.isNull("paidById")) null else j.getString("paidById")
                Session(
                    id = j.getString("id"),
                    date = LocalDate.parse(j.getString("date")),
                    time = LocalTime.parse(j.getString("time")),
                    price = j.getInt("price"),
                    payerId = if (paid && oldPaidBy != null) oldPaidBy else j.getString("payerId"),
                    paid = paid,
                    paidDate = if (j.isNull("paidDate")) null else LocalDate.parse(j.getString("paidDate")),
                    notes = j.optString("notes", ""),
                    remindBeforeMin = if (j.isNull("remindBeforeMin")) null else j.getInt("remindBeforeMin"),
                    remindPay = j.optBoolean("remindPay", false),
                    customRemindAt = if (j.isNull("customRemindAt")) null else j.getLong("customRemindAt")
                )
            }
            Snapshot(payers, sessions, o.optInt("defaultPrice", 450))
        } catch (e: Exception) {
            // קובץ פגום: שומרים עותק גיבוי ולא מוחקים אותו
            f.renameTo(File(ctx.filesDir, "$FILE.bak"))
            Snapshot(defaultPayers(), emptyList(), 450)
        }
    }

    fun save(ctx: Context, s: Snapshot) {
        val payers = JSONArray()
        s.payers.forEach { p ->
            payers.put(JSONObject().put("id", p.id).put("name", p.name))
        }
        val sessions = JSONArray()
        s.sessions.forEach { x ->
            val j = JSONObject()
            j.put("id", x.id)
            j.put("date", x.date.toString())
            j.put("time", x.time.toString())
            j.put("price", x.price)
            j.put("payerId", x.payerId)
            j.put("paid", x.paid)
            j.put("paidDate", x.paidDate?.toString() ?: JSONObject.NULL)
            j.put("notes", x.notes)
            j.put("remindBeforeMin", x.remindBeforeMin ?: JSONObject.NULL)
            j.put("remindPay", x.remindPay)
            j.put("customRemindAt", x.customRemindAt ?: JSONObject.NULL)
            sessions.put(j)
        }
        val o = JSONObject()
        o.put("defaultPrice", s.defaultPrice)
        o.put("payers", payers)
        o.put("sessions", sessions)
        val tmp = File(ctx.filesDir, "$FILE.tmp")
        tmp.writeText(o.toString())
        tmp.renameTo(File(ctx.filesDir, FILE))
    }
}

// ---------- מצב האפליקציה ----------

class AuraStore(private val app: Context) {
    var payers by mutableStateOf(listOf<Payer>())
        private set
    var sessions by mutableStateOf(listOf<Session>())
        private set
    var defaultPrice by mutableStateOf(450)
        private set

    init {
        val s = Repo.load(app)
        payers = s.payers
        sessions = s.sessions
        defaultPrice = s.defaultPrice
    }

    private fun persist() {
        Repo.save(app, Repo.Snapshot(payers, sessions, defaultPrice))
    }

    fun payerName(id: String): String = payers.firstOrNull { it.id == id }?.name ?: "—"

    fun session(id: String): Session? = sessions.firstOrNull { it.id == id }

    fun lastPayerId(): String {
        val last = sessions.maxByOrNull { it.dateTime }
        return last?.payerId ?: payers.firstOrNull()?.id ?: ""
    }

    // --- פגישות ---

    fun upsertSession(s: Session) {
        val exists = sessions.any { it.id == s.id }
        sessions = if (exists) sessions.map { if (it.id == s.id) s else it } else sessions + s
        persist()
        Reminders.schedule(app, s, payerName(s.payerId))
    }

    fun deleteSession(id: String) {
        sessions = sessions.filter { it.id != id }
        persist()
        Reminders.cancel(app, id)
    }

    /** סימון פגישה כשולמה / לא שולמה. אם [byId] ניתן – זה מי ששילם בפועל. */
    fun setPaid(id: String, paid: Boolean, date: LocalDate = LocalDate.now(), byId: String? = null) {
        val s = session(id) ?: return
        upsertSession(
            s.copy(
                paid = paid,
                paidDate = if (paid) date else null,
                payerId = if (paid && byId != null) byId else s.payerId
            )
        )
    }

    /** אדם אחד משלם בפועל על כמה פגישות: כל אחת נשארת פגישה נפרדת עם אותו מחיר. */
    fun markPaidMany(ids: Collection<String>, byId: String, date: LocalDate = LocalDate.now()) {
        if (ids.isEmpty()) return
        val idSet = ids.toSet()
        val updated = Ledger.markPaid(sessions, idSet, byId, date)
        sessions = updated
        persist()
        updated.filter { it.id in idSet }.forEach { Reminders.schedule(app, it, payerName(it.payerId)) }
    }

    /** הוספת כמה פגישות בבת אחת (פגישות קודמות). */
    fun addSessions(list: List<Session>) {
        if (list.isEmpty()) return
        sessions = sessions + list
        persist()
        list.forEach { Reminders.schedule(app, it, payerName(it.payerId)) }
    }

    fun changePayer(id: String, payerId: String) {
        val s = session(id) ?: return
        upsertSession(s.copy(payerId = payerId))
    }

    // --- משלמים ---

    fun addPayer(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        payers = payers + Payer(UUID.randomUUID().toString(), n)
        persist()
    }

    fun renamePayer(id: String, name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        payers = payers.map { if (it.id == id) it.copy(name = n) else it }
        persist()
    }

    /** מחיקה אפשרית רק למשלם ללא פגישות. */
    fun deletePayer(id: String): Boolean {
        if (sessions.any { it.payerId == id }) return false
        payers = payers.filter { it.id != id }
        persist()
        return true
    }

    fun updateDefaultPrice(p: Int) {
        defaultPrice = p
        persist()
    }

    // --- חישובים ---

    fun isDue(s: Session, now: LocalDateTime = LocalDateTime.now()): Boolean = Ledger.isDue(s, now)

    fun statusOf(s: Session): PayStatus = Ledger.status(s, LocalDateTime.now())

    /** פגישות שכבר התקיימו ולא שולמו = חוב. */
    fun dueSessions(payerId: String? = null): List<Session> =
        Ledger.dueSessions(sessions, payerId, LocalDateTime.now())

    fun totalDebt(): Int = Ledger.totalDebt(sessions, LocalDateTime.now())

    fun nextSession(): Session? {
        val now = LocalDateTime.now()
        return sessions.filter { it.dateTime.isAfter(now) }.minByOrNull { it.dateTime }
    }

    fun upcomingCount(): Int {
        val now = LocalDateTime.now()
        return sessions.count { it.dateTime.isAfter(now) }
    }

    fun recentSessions(n: Int): List<Session> {
        val now = LocalDateTime.now()
        return sessions
            .filter { !it.dateTime.isAfter(now) }
            .sortedByDescending { it.dateTime }
            .take(n)
    }

    fun statsOf(payerId: String): PayerStats = Ledger.stats(sessions, payerId, LocalDateTime.now())
}
