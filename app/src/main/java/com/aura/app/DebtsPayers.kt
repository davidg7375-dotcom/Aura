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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import kotlin.math.roundToInt

// ======================= חובות =======================

@Composable
fun DebtsScreen(store: AuraStore, nav: Nav) {
    val selected = remember { mutableStateListOf<String>() }
    var showPay by remember { mutableStateOf(false) }
    var showList by remember { mutableStateOf(false) }

    val due = store.dueSessions()
    val dueIds = due.map { it.id }.toSet()
    LaunchedEffect(dueIds) { selected.retainAll(dueIds) }

    val rows = store.payers
        .map { p -> p to store.balanceOf(p.id) }
        .sortedWith(
            compareByDescending<Pair<Payer, Balance>> { it.second.money }
                .thenBy { it.first.name }
        )

    val chosen = store.sessions.filter { it.id in selected }
    val chosenSum = chosen.sumOf { it.price }
    val unpaidSum = store.unpaidSum()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("חובות")
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.AccountBalanceWallet, size = 48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("לא שולם", fontSize = 14.sp, color = AuraColors.Muted)
                        Text(
                            if (due.isEmpty()) "הכול שולם" else sessionsPhrase(Ledger.units(unpaidSum, store.defaultPrice)),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (due.isEmpty()) AuraColors.PaidFg else AuraColors.Debt
                        )
                    }
                    if (due.isNotEmpty()) {
                        Text(fmtMoney(unpaidSum), fontSize = 13.sp, color = AuraColors.Muted)
                    }
                }
            }

            rows.forEach { (p, b) ->
                AuraCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                            Text(
                                "החלק: ${sessionsAmount(b.share)} · שילם: ${sessionsAmount(b.paid)}",
                                fontSize = 12.sp,
                                color = AuraColors.Muted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                balanceText(b),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = balanceColor(b.kind)
                            )
                            if (b.kind != BalanceKind.Even) {
                                Text(
                                    fmtMoney(kotlin.math.abs(b.money).roundToInt()),
                                    fontSize = 11.sp,
                                    color = AuraColors.Muted
                                )
                            }
                        }
                    }
                }
            }

            if (due.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showList = !showList }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "סימון פגישות כשולמו · ${unpaidCountText(due.size)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = AuraColors.GreenDark,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        if (showList) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        null,
                        tint = AuraColors.Muted
                    )
                }
                if (showList) {
                    AuraCard(Modifier.fillMaxWidth()) {
                        val allSelected = due.all { it.id in selected }
                        TextButton(onClick = {
                            if (allSelected) selected.clear()
                            else due.forEach { if (it.id !in selected) selected.add(it.id) }
                        }) {
                            Text(
                                if (allSelected) "ביטול בחירה" else "בחירת כל הפגישות",
                                fontSize = 13.sp,
                                color = AuraColors.GreenDark
                            )
                        }
                        due.forEachIndexed { i, s ->
                            if (i > 0) HorizontalDivider(color = AuraColors.Line)
                            DebtRow(
                                s = s,
                                checked = s.id in selected,
                                onToggle = { if (s.id in selected) selected.remove(s.id) else selected.add(s.id) },
                                onOpen = { nav.go(Route.Details(s.id)) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (selected.isNotEmpty()) {
            Surface(color = AuraColors.Card, shadowElevation = 8.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        "סימון כשולמו · ${fmtMoney(chosenSum)}",
                        { showPay = true },
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showPay) {
        var pickedId by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showPay = false },
            title = { Text("שולם על ידי") },
            text = {
                Column {
                    Text(
                        "${selected.size} פגישות · ${fmtMoney(chosenSum)}. כל פגישה תישאר רשומה בנפרד.",
                        fontSize = 13.sp,
                        color = AuraColors.Muted
                    )
                    Spacer(Modifier.height(8.dp))
                    store.payers.forEach { p ->
                        PersonOption(p.name, pickedId == p.id) { pickedId = p.id }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pickedId != null,
                    onClick = {
                        pickedId?.let { store.markPaidMany(selected.toList(), it) }
                        selected.clear()
                        showPay = false
                    }
                ) { Text("סימון כשולם") }
            },
            dismissButton = { TextButton(onClick = { showPay = false }) { Text("ביטול") } }
        )
    }
}

@Composable
private fun PersonOption(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(name, fontSize = 16.sp)
    }
}

@Composable
private fun DebtRow(s: Session, checked: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = AuraColors.Green,
                uncheckedColor = AuraColors.Muted,
                checkmarkColor = Color.White
            )
        )
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpen)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(fmtNumeric(s.date), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AuraColors.Ink)
            Text("  —  ", fontSize = 13.sp, color = AuraColors.Muted)
            Text(fmtMoney(s.price), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
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
                        Text(
                            balanceText(st.balance),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = balanceColor(st.balance.kind)
                        )
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
