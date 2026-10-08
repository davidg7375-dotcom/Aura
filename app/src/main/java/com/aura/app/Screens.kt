package com.aura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================= בית =======================

@Composable
fun HomeScreen(store: AuraStore, nav: Nav) {
    val next = store.nextSession()
    val recent = store.recentSessions(6)
    val debt = store.totalDebt()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AuraLogo(size = 34)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { nav.go(Route.Settings) }) {
                Icon(Icons.Rounded.Settings, "הגדרות", tint = AuraColors.Ink)
            }
        }

        val onNext: (() -> Unit)? = next?.let { n -> { nav.go(Route.Details(n.id)) } }
        AuraCard(Modifier.fillMaxWidth(), onNext) {
            if (next != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("הפגישה הבאה", fontSize = 14.sp, color = AuraColors.Muted)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${fmtDayMonth(next.date)} · ${fmtTime(next.time)}",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraColors.Ink
                        )
                        Text(fmtWeekday(next.date), fontSize = 14.sp, color = AuraColors.Muted)
                        Spacer(Modifier.height(10.dp))
                        Text(fmtMoney(next.price), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                    }
                    IconBadge(Icons.Rounded.CalendarMonth, size = 58.dp, onClick = { nav.go(Route.Calendar) })
                }
            } else {
                Text("הפגישה הבאה", fontSize = 14.sp, color = AuraColors.Muted)
                Spacer(Modifier.height(6.dp))
                Text("אין פגישה קרובה", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                Spacer(Modifier.height(14.dp))
                SecondaryButton("הוספת פגישה", { nav.go(Route.Form()) })
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard(
                "פגישות קרובות",
                store.upcomingCount().toString(),
                AuraColors.Ink,
                Modifier.weight(1f)
            ) { nav.tab(Route.Sessions) }
            SummaryCard(
                "חובות פתוחים",
                fmtMoney(debt),
                if (debt > 0) AuraColors.Debt else AuraColors.Ink,
                Modifier.weight(1f)
            ) { nav.tab(Route.Debts) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("פגישות אחרונות", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { nav.tab(Route.Sessions) }) {
                Text("הצג הכל", color = AuraColors.GreenDark)
            }
        }

        if (recent.isEmpty()) {
            Text(
                "עדיין אין פגישות קודמות",
                fontSize = 15.sp,
                color = AuraColors.Muted,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            recent.forEach { s ->
                SessionCard(s, store, { nav.go(Route.Details(s.id)) })
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    AuraCard(modifier, onClick) {
        Text(label, fontSize = 14.sp, color = AuraColors.Muted)
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

// ======================= פגישות =======================

enum class StatusFilter { All, Paid, Unpaid }

@Composable
fun SessionsScreen(store: AuraStore, nav: Nav, fixedPayerId: String? = null) {
    var filter by remember { mutableStateOf(StatusFilter.All) }
    var payerFilter by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var editPayer by remember { mutableStateOf(false) }

    val personId = fixedPayerId ?: payerFilter
    val list = store.sessions
        .filter { s ->
            (personId == null || s.paidById == personId || s.debtorId == personId) && when (filter) {
                StatusFilter.All -> true
                StatusFilter.Paid -> s.paid
                StatusFilter.Unpaid -> !s.paid
            }
        }
        .sortedByDescending { it.dateTime }

    Column(Modifier.fillMaxSize()) {
        if (fixedPayerId != null) {
            ScreenHeader(store.payerName(fixedPayerId), onBack = { nav.back() }) {
                IconButton(onClick = { editPayer = true }) {
                    Icon(Icons.Rounded.Edit, "עריכת משלם", tint = AuraColors.Ink)
                }
            }
        } else {
            ScreenHeader("פגישות") {
                IconButton(onClick = { nav.go(Route.Calendar) }) {
                    Icon(Icons.Rounded.CalendarMonth, "לוח שנה", tint = AuraColors.Ink)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pill("הכול", filter == StatusFilter.All, { filter = StatusFilter.All })
            Pill("שולם", filter == StatusFilter.Paid, { filter = StatusFilter.Paid })
            Pill("לא שולם", filter == StatusFilter.Unpaid, { filter = StatusFilter.Unpaid })
            if (fixedPayerId == null) {
                Box {
                    Pill(
                        label = payerFilter?.let { store.payerName(it) } ?: "לפי אדם",
                        selected = payerFilter != null,
                        onClick = { menuOpen = true },
                        trailing = Icons.Rounded.KeyboardArrowDown
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("כולם") },
                            onClick = { payerFilter = null; menuOpen = false }
                        )
                        store.payers.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = { payerFilter = p.id; menuOpen = false }
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (fixedPayerId != null) {
                item {
                    val st = store.statsOf(fixedPayerId)
                    PayerSummaryCard(st)
                }
            }
            if (fixedPayerId == null) {
                item {
                    AuraCard(Modifier.fillMaxWidth(), onClick = { nav.go(Route.Bulk) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Rounded.History, size = 42.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("הוספת פגישות קודמות", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                                Text("כמה פגישות כבר היו ומי שילם על כל אחת", fontSize = 13.sp, color = AuraColors.Muted)
                            }
                        }
                    }
                }
            }
            if (list.isEmpty()) {
                item {
                    Text(
                        "אין פגישות להצגה",
                        fontSize = 15.sp,
                        color = AuraColors.Muted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            items(list, key = { it.id }) { s ->
                SessionCard(s, store, { nav.go(Route.Details(s.id)) })
            }
        }
    }

    if (editPayer && fixedPayerId != null) {
        PayerEditDialog(
            store = store,
            payerId = fixedPayerId,
            onDismiss = { editPayer = false },
            onDeleted = { editPayer = false; nav.back() }
        )
    }
}

fun payerStatsText(st: PayerStats): String {
    val parts = mutableListOf<String>()
    if (st.paidCount == 1) parts.add("שילם על פגישה אחת")
    else if (st.paidCount > 1) parts.add("שילם על ${st.paidCount} פגישות")
    if (st.dueCount > 0) parts.add(unpaidCountText(st.dueCount))
    return if (parts.isEmpty()) "אין פגישות" else parts.joinToString(" · ")
}

@Composable
private fun PayerSummaryCard(st: PayerStats) {
    AuraCard(Modifier.fillMaxWidth()) {
        Text(payerStatsText(st), fontSize = 14.sp, color = AuraColors.Muted)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("חוב נוכחי", fontSize = 15.sp, color = AuraColors.Ink)
            Spacer(Modifier.weight(1f))
            Text(
                fmtMoney(st.debt),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (st.debt > 0) AuraColors.Debt else AuraColors.PaidFg
            )
        }
        if (st.paidSum > 0) {
            Spacer(Modifier.height(6.dp))
            Row {
                Text("שולם על ידו בסך הכל", fontSize = 13.sp, color = AuraColors.Muted)
                Spacer(Modifier.weight(1f))
                Text(fmtMoney(st.paidSum), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.PaidFg)
            }
        }
    }
}

// ======================= פרטי פגישה =======================

@Composable
fun DetailsScreen(store: AuraStore, nav: Nav, id: String) {
    val s = store.session(id)
    LaunchedEffect(s == null) {
        if (s == null) nav.back()
    }
    if (s == null) return

    var showPayer by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showPaidBy by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("פרטי פגישה", onBack = { nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuraCard(Modifier.fillMaxWidth()) {
                DetailRow("תאריך", fmtDate(s.date))
                DetailRow("שעה", fmtTime(s.time))
                DetailRow("מחיר", fmtMoney(s.price))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("סטטוס תשלום", fontSize = 14.sp, color = AuraColors.Muted)
                    Spacer(Modifier.weight(1f))
                    StatusChip(store.statusOf(s))
                }
                if (s.paid) {
                    HorizontalDivider(color = AuraColors.Line)
                    DetailRow("שולם על ידי", s.paidById?.let { store.payerName(it) } ?: "—")
                    if (s.paidDate != null) DetailRow("תאריך תשלום", fmtDate(s.paidDate))
                }
                if (s.notes.isNotBlank()) {
                    HorizontalDivider(color = AuraColors.Line)
                    DetailRow("הערות", s.notes)
                }
                val rem = mutableListOf<String>()
                s.remindBeforeMin?.let { rem.add(beforeLabel(it)) }
                if (s.remindPay) rem.add("סימון תשלום")
                s.customRemindAt?.let { rem.add("מותאמת אישית") }
                if (rem.isNotEmpty()) {
                    HorizontalDivider(color = AuraColors.Line)
                    DetailRow("תזכורות", rem.joinToString(" · "))
                }
            }

            if (s.paid) {
                SecondaryButton("שינוי: שולם על ידי", { showPayer = true })
                SecondaryButton("סימון כלא שולם", { store.markUnpaid(s.id) })
            } else {
                PrimaryButton("סימון כשולם", { showPaidBy = true })
            }
            SecondaryButton("עריכת פגישה", { nav.go(Route.Form(editId = s.id)) })
            SecondaryButton("מחיקת פגישה", { showDelete = true }, danger = true)
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showPayer) {
        AlertDialog(
            onDismissRequest = { showPayer = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPayer = false }) { Text("ביטול") } },
            title = { Text("שולם על ידי") },
            text = {
                Column {
                    store.payers.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .clickable {
                                    store.updatePaidInfo(s.id, p.id, s.paidDate ?: java.time.LocalDate.now())
                                    showPayer = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = p.id == s.paidById, onClick = {
                                store.updatePaidInfo(s.id, p.id, s.paidDate ?: java.time.LocalDate.now())
                                showPayer = false
                            })
                            Text(p.name, fontSize = 16.sp)
                        }
                    }
                }
            }
        )
    }

    if (showPaidBy) {
        AlertDialog(
            onDismissRequest = { showPaidBy = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPaidBy = false }) { Text("ביטול") } },
            title = { Text("שולם על ידי") },
            text = {
                Column {
                    store.payers.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .clickable {
                                    store.markPaid(s.id, p.id)
                                    showPaidBy = false
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = false, onClick = {
                                store.markPaid(s.id, p.id)
                                showPaidBy = false
                            })
                            Text(p.name, fontSize = 16.sp)
                        }
                    }
                }
            }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("מחיקת פגישה") },
            text = { Text("למחוק את הפגישה מ־${fmtDate(s.date)}? לא ניתן לשחזר.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    store.deleteSession(s.id)
                    nav.back()
                }) { Text("מחיקה", color = AuraColors.DueFg) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("ביטול") } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 14.sp, color = AuraColors.Muted)
        Spacer(Modifier.width(16.dp))
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = AuraColors.Ink,
            modifier = Modifier.weight(2f, fill = false)
        )
    }
}

// ======================= עריכת / מחיקת משלם =======================

@Composable
fun PayerEditDialog(store: AuraStore, payerId: String, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    var name by remember { mutableStateOf(store.payerName(payerId)) }
    val hasSessions = store.sessions.any { it.paidById == payerId || it.debtorId == payerId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("עריכת משלם") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("שם") }
                )
                Spacer(Modifier.height(12.dp))
                if (hasSessions) {
                    Text("לא ניתן למחוק משלם שיש לו פגישות.", fontSize = 13.sp, color = AuraColors.Muted)
                } else {
                    TextButton(onClick = {
                        if (store.deletePayer(payerId)) onDeleted()
                    }) { Text("מחיקת משלם", color = AuraColors.DueFg) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                store.renamePayer(payerId, name)
                onDismiss()
            }) { Text("שמירה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    )
}
