package com.aura.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ======================= חובות =======================

@Composable
fun DebtsScreen(store: AuraStore, nav: Nav) {
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    val groups = store.payers.mapNotNull { p ->
        val l = store.dueSessions(p.id)
        if (l.isEmpty()) null else p to l
    }.sortedByDescending { g -> g.second.sumOf { it.price } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("חובות פתוחים")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val total = store.totalDebt()
            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AccountBalanceWallet, size = 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("סה\"כ חובות פתוחים", fontSize = 14.sp, color = AuraColors.Muted)
                        Text(
                            fmtMoney(total),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (total > 0) AuraColors.Debt else AuraColors.PaidFg
                        )
                    }
                }
            }

            if (groups.isEmpty()) {
                Text(
                    "אין חובות פתוחים — הכול שולם",
                    fontSize = 16.sp,
                    color = AuraColors.Muted,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }

            groups.forEach { (payer, list) ->
                val open = expanded[payer.id] ?: true
                AuraCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { expanded[payer.id] = !open },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Avatar(size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(payer.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                            Text("${list.size} פגישות לא שולמו", fontSize = 13.sp, color = AuraColors.DueFg)
                        }
                        Text(
                            fmtMoney(list.sumOf { it.price }),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraColors.Debt
                        )
                        Icon(
                            if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            null,
                            tint = AuraColors.Muted
                        )
                    }
                    if (open) {
                        Spacer(Modifier.height(8.dp))
                        list.forEachIndexed { i, s ->
                            if (i > 0) HorizontalDivider(color = AuraColors.Line)
                            DebtRow(s, onOpen = { nav.go(Route.Details(s.id)) }, onPaid = { store.setPaid(s.id, true) })
                        }
                    }
                }
            }
            val cross = store.crossDebts()
            if (cross.isNotEmpty()) {
                Text(
                    "התחשבנות בין משלמים",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AuraColors.Ink,
                    modifier = Modifier.padding(top = 10.dp)
                )
                Text(
                    "פגישות ששולמו על ידי מישהו אחר מהמשלם שלהן",
                    fontSize = 13.sp,
                    color = AuraColors.Muted
                )
                cross.forEach { c ->
                    val key = "x_${c.debtorId}_${c.creditorId}"
                    val open = expanded[key] ?: true
                    AuraCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { expanded[key] = !open },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Avatar(size = 46.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${store.payerName(c.debtorId)} חייב ל־${store.payerName(c.creditorId)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraColors.Ink
                                )
                                Text(
                                    "${c.sessions.size} פגישות ששולמו על ידי ${store.payerName(c.creditorId)}",
                                    fontSize = 13.sp,
                                    color = AuraColors.Muted
                                )
                            }
                            Text(
                                fmtMoney(c.net),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AuraColors.SoonFg
                            )
                            Icon(
                                if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                null,
                                tint = AuraColors.Muted
                            )
                        }
                        if (open) {
                            Spacer(Modifier.height(8.dp))
                            c.sessions.forEachIndexed { i, s ->
                                if (i > 0) HorizontalDivider(color = AuraColors.Line)
                                CrossRow(s) { nav.go(Route.Details(s.id)) }
                            }
                            if (c.offsetting > 0) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "בניכוי ${fmtMoney(c.offsetting)} שהיו בכיוון ההפוך",
                                    fontSize = 12.sp,
                                    color = AuraColors.Muted
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CrossRow(s: Session, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(AuraColors.SoonFg)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${fmtDate(s.date)} · פגישה", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AuraColors.Ink)
            Text("${fmtTime(s.time)} · שולם", fontSize = 13.sp, color = AuraColors.Muted)
        }
        Text(fmtMoney(s.price), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
    }
}

@Composable
private fun DebtRow(s: Session, onOpen: () -> Unit, onPaid: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(AuraColors.DueFg)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("${fmtDate(s.date)} · פגישה", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AuraColors.Ink)
            Text("${fmtTime(s.time)} · לא שולם", fontSize = 13.sp, color = AuraColors.Muted)
        }
        Text(fmtMoney(s.price), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onPaid) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AuraColors.PaidBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, "סימון כשולם", tint = AuraColors.PaidFg, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ======================= משלמים =======================

@Composable
fun PayersScreen(store: AuraStore, nav: Nav) {
    var showAdd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("משלמים") {
            IconButton(onClick = { showAdd = true }) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AuraColors.Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, "הוסף משלם", tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            store.payers.forEach { p ->
                val st = store.statsOf(p.id)
                AuraCard(Modifier.fillMaxWidth(), onClick = { nav.go(Route.PayerSessions(p.id)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(size = 52.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                            Text(payerStatsText(st), fontSize = 13.sp, color = AuraColors.Muted)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("חוב", fontSize = 12.sp, color = AuraColors.Muted)
                            Text(
                                fmtMoney(st.debt),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (st.debt > 0) AuraColors.Debt else AuraColors.PaidFg
                            )
                            if (st.owes > 0) {
                                Text("חוב לאחרים: ${fmtMoney(st.owes)}", fontSize = 11.sp, color = AuraColors.Debt)
                            }
                            if (st.owed > 0) {
                                Text("לקבל: ${fmtMoney(st.owed)}", fontSize = 11.sp, color = AuraColors.PaidFg)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showAdd) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("הוספת משלם") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("שם המשלם") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        store.addPayer(name)
                        showAdd = false
                    }
                ) { Text("הוספה") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("ביטול") } }
        )
    }
}
