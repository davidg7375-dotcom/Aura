package com.aura.app

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth

private val weekdayLetters = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")

// ======================= לוח שנה =======================

@Composable
fun CalendarScreen(store: AuraStore, nav: Nav) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf(today) }

    val first = month.atDay(1)
    // ראשון = 0 ... שבת = 6
    val offset = first.dayOfWeek.value % 7
    val cells = offset + month.lengthOfMonth()
    val rows = (cells + 6) / 7

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("לוח שנה", onBack = { nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // בעברית: "קודם" מימין ומצביע ימינה, "הבא" משמאל ומצביע שמאלה
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "חודש קודם", tint = AuraColors.Ink)
                    }
                    Text(
                        fmtMonthYear(month),
                        modifier = Modifier.weight(1f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraColors.Ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "חודש הבא", tint = AuraColors.Ink)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    weekdayLetters.forEach { l ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(l, fontSize = 13.sp, color = AuraColors.Muted)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - offset + 1
                            if (dayNum < 1 || dayNum > month.lengthOfMonth()) {
                                Box(Modifier.weight(1f).height(48.dp))
                            } else {
                                val d = month.atDay(dayNum)
                                DayCell(
                                    day = d,
                                    isToday = d == today,
                                    isSelected = d == selected,
                                    sessions = store.sessions.filter { it.date == d },
                                    store = store,
                                    modifier = Modifier.weight(1f)
                                ) { selected = d }
                            }
                        }
                    }
                }
            }

            Text(
                "פגישות בתאריך זה",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = AuraColors.Ink,
                modifier = Modifier.padding(top = 22.dp, bottom = 4.dp)
            )
            Text(fmtDate(selected), fontSize = 14.sp, color = AuraColors.Muted)
            Spacer(Modifier.height(10.dp))

            val daySessions = store.sessions.filter { it.date == selected }.sortedBy { it.time }
            if (daySessions.isEmpty()) {
                Text("אין פגישות בתאריך זה", fontSize = 15.sp, color = AuraColors.Muted)
            } else {
                daySessions.forEach { s ->
                    SessionCard(s, store, { nav.go(Route.Details(s.id)) })
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("+ הוסף פגישה", { nav.go(Route.Form(date = selected)) })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    sessions: List<Session>,
    store: AuraStore,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSelected) AuraColors.Green else Color.Transparent)
                .then(
                    if (isToday && !isSelected) Modifier.border(1.5.dp, AuraColors.Green, CircleShape) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                day.dayOfMonth.toString(),
                fontSize = 15.sp,
                fontWeight = if (sessions.isNotEmpty() || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color.White else AuraColors.Ink
            )
        }
        Row(Modifier.height(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            sessions.take(3).forEach { s ->
                Box(
                    Modifier
                        .padding(top = 3.dp)
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(statusColor(store.statusOf(s)))
                )
            }
        }
    }
}

// ======================= הגדרות =======================

@Composable
fun SettingsScreen(store: AuraStore, nav: Nav) {
    val ctx = LocalContext.current
    var priceText by remember { mutableStateOf(store.defaultPrice.toString()) }
    val newPrice = priceText.toIntOrNull() ?: 0

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("הגדרות", onBack = { nav.back() })
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuraCard(Modifier.fillMaxWidth()) {
                Text("מחיר ברירת מחדל לפגישה", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() }.take(5) },
                    singleLine = true,
                    label = { Text("מחיר ב־₪") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                PrimaryButton(
                    "שמירה",
                    { store.updateDefaultPrice(newPrice) },
                    enabled = newPrice > 0 && newPrice != store.defaultPrice
                )
            }

            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("פגישה שבועית מוצעת", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                        Text("כל יום חמישי ב־11:00, רק באישור שלך", fontSize = 13.sp, color = AuraColors.Muted)
                    }
                    androidx.compose.material3.Switch(
                        checked = store.weeklyEnabled,
                        onCheckedChange = { store.setWeeklyEnabled(it) }
                    )
                }
            }

            var showPin by remember { mutableStateOf(false) }
            AuraCard(Modifier.fillMaxWidth()) {
                Text("קוד כניסה", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                Spacer(Modifier.height(10.dp))
                SecondaryButton("שינוי קוד", { showPin = true })
            }
            if (showPin) ChangePinDialog { showPin = false }

            AuraCard(Modifier.fillMaxWidth()) {
                Text("התראות", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
                Spacer(Modifier.height(6.dp))
                Text(
                    "תזכורות נשלחות כהתראות מהמכשיר. אם הן לא מופיעות, יש לאפשר התראות עבור Breathe.",
                    fontSize = 14.sp,
                    color = AuraColors.Muted
                )
                Spacer(Modifier.height(12.dp))
                SecondaryButton("פתיחת הגדרות התראות", {
                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                    ctx.startActivity(i)
                })
            }

            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                AuraLogo(size = 26)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
