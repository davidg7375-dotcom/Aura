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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private data class BulkRow(
    val key: String,
    val date: LocalDate,
    val price: String,
    val payerId: String,
    val paid: Boolean,
    val paidById: String?
)

/** הוספת כמה פגישות קודמות בבת אחת: לכל פגישה תאריך, מחיר, משלם, וסטטוס תשלום (ומי שילם בפועל). */
@Composable
fun BulkScreen(store: AuraStore, nav: Nav) {
    val rows = remember {
        mutableStateListOf(
            BulkRow(
                key = UUID.randomUUID().toString(),
                date = LocalDate.now().minusDays(7),
                price = store.defaultPrice.toString(),
                payerId = store.lastPayerId(),
                paid = true,
                paidById = null
            )
        )
    }
    var pickIndex by remember { mutableStateOf<Int?>(null) }

    val canSave = rows.isNotEmpty() && rows.all { (it.price.toIntOrNull() ?: 0) > 0 }

    fun save() {
        val list = rows.map { r ->
            Session(
                id = UUID.randomUUID().toString(),
                date = r.date,
                time = LocalTime.of(18, 0),
                price = r.price.toInt(),
                payerId = r.payerId,
                paid = r.paid,
                paidDate = if (r.paid) r.date else null,
                paidById = if (r.paid && r.paidById != null && r.paidById != r.payerId) r.paidById else null
            )
        }
        store.addSessions(list)
        nav.tab(Route.Sessions)
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader("פגישות קודמות", onBack = { nav.back() })

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "הוסיפו את הפגישות שכבר היו. לכל פגישה בוחרים מי המשלם, אם שולם, ומי שילם בפועל. השעה נקבעת ל־18:00 וניתן לשנות אותה בעריכת הפגישה.",
                fontSize = 14.sp,
                color = AuraColors.Muted
            )

            rows.forEachIndexed { i, r ->
                key(r.key) {
                    AuraCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AuraColors.Sage)
                                    .clickable { pickIndex = i }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    fmtDate(r.date),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraColors.GreenDark
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            if (rows.size > 1) {
                                IconButton(onClick = { rows.removeAt(i) }) {
                                    Icon(Icons.Rounded.Close, "הסרת פגישה", tint = AuraColors.Muted)
                                }
                            }
                        }

                        AuraTextField(
                            value = r.price,
                            onChange = { v -> rows[i] = r.copy(price = v.filter { it.isDigit() }.take(5)) },
                            label = "מחיר ב־₪",
                            keyboardType = KeyboardType.Number
                        )

                        Spacer(Modifier.height(8.dp))
                        PayerPicker("משלם", store, r.payerId) { rows[i] = r.copy(payerId = it) }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("שולם", r.paid, { rows[i] = r.copy(paid = true) })
                            Pill("לא שולם", !r.paid, { rows[i] = r.copy(paid = false, paidById = null) })
                        }

                        if (r.paid) {
                            Spacer(Modifier.height(10.dp))
                            PayerPicker("שולם ע״י", store, r.paidById ?: r.payerId) {
                                rows[i] = r.copy(paidById = it)
                            }
                        }
                    }
                }
            }

            SecondaryButton("הוספת פגישה נוספת", {
                val last = rows.last()
                rows.add(last.copy(key = UUID.randomUUID().toString(), date = last.date.minusDays(7)))
            })
            Spacer(Modifier.height(4.dp))
        }

        Box(
            Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            PrimaryButton(
                if (rows.size == 1) "שמירת פגישה" else "שמירת ${rows.size} פגישות",
                { save() },
                enabled = canSave
            )
        }
    }

    pickIndex?.let { idx ->
        if (idx in rows.indices) {
            DatePickDialog(rows[idx].date, { pickIndex = null }) { picked ->
                rows[idx] = rows[idx].copy(date = picked)
            }
        }
    }
}

@Composable
private fun PayerPicker(label: String, store: AuraStore, selectedId: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AuraColors.Bg)
                .clickable { open = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 13.sp, color = AuraColors.Muted)
            Spacer(Modifier.width(10.dp))
            Text(
                store.payerName(selectedId),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = AuraColors.Ink,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = AuraColors.Muted, modifier = Modifier.size(22.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            store.payers.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.name) },
                    onClick = { onPick(p.id); open = false }
                )
            }
        }
    }
}
