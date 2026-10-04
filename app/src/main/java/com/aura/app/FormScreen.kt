package com.aura.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID

private val beforeOptions: List<Pair<String, Int?>> = listOf(
    "ללא" to null,
    "30 דקות" to 30,
    "שעה" to 60,
    "3 שעות" to 180,
    "יום" to 1440
)

@Composable
fun FormScreen(store: AuraStore, nav: Nav, editId: String?, presetDate: LocalDate?) {
    val existing = remember(editId) { editId?.let { store.session(it) } }

    var date by remember { mutableStateOf(existing?.date ?: presetDate ?: LocalDate.now()) }
    var time by remember { mutableStateOf(existing?.time ?: LocalTime.of(18, 0)) }
    var priceText by remember { mutableStateOf((existing?.price ?: store.defaultPrice).toString()) }
    var payerId by remember { mutableStateOf(existing?.payerId ?: store.lastPayerId()) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var paid by remember { mutableStateOf(existing?.paid ?: false) }
    var paidDate by remember { mutableStateOf(existing?.paidDate ?: LocalDate.now()) }

    var remindOn by remember {
        mutableStateOf(
            existing != null &&
                (existing.remindBeforeMin != null || existing.remindPay || existing.customRemindAt != null)
        )
    }
    var remindBefore by remember { mutableStateOf(existing?.remindBeforeMin) }
    var remindPay by remember { mutableStateOf(existing?.remindPay ?: false) }
    val customInit = existing?.customRemindAt?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    var customOn by remember { mutableStateOf(customInit != null) }
    var customDate by remember { mutableStateOf(customInit?.toLocalDate() ?: date) }
    var customTime by remember { mutableStateOf(customInit?.toLocalTime() ?: LocalTime.of(9, 0)) }

    var dialog by remember { mutableStateOf<String?>(null) }
    var payerMenu by remember { mutableStateOf(false) }

    val ctx = LocalContext.current
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val price = priceText.toIntOrNull() ?: 0
    val canSave = price > 0 && store.payers.any { it.id == payerId }

    fun save() {
        val customAt: Long? = if (remindOn && customOn) {
            LocalDateTime.of(customDate, customTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null
        val s = Session(
            id = existing?.id ?: UUID.randomUUID().toString(),
            date = date,
            time = time,
            price = price,
            payerId = payerId,
            paid = paid,
            paidDate = if (paid) paidDate else null,
            notes = notes.trim(),
            remindBeforeMin = if (remindOn) remindBefore else null,
            remindPay = remindOn && remindPay,
            customRemindAt = customAt
        )
        store.upsertSession(s)
        nav.back()
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        ScreenHeader(
            title = if (existing == null) "הוספת פגישה" else "עריכת פגישה",
            onBack = { nav.back() }
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FormRow(Icons.Outlined.CalendarMonth, "תאריך", fmtDate(date)) { dialog = "date" }
            FormRow(Icons.Outlined.Schedule, "שעה", fmtTime(time)) { dialog = "time" }

            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Payments, null, tint = AuraColors.GreenDark, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    AuraTextField(
                        value = priceText,
                        onChange = { v -> priceText = v.filter { it.isDigit() }.take(5) },
                        label = "מחיר ב־₪",
                        keyboardType = KeyboardType.Number
                    )
                }
            }

            Box {
                FormRow(Icons.Outlined.Person, "משלם", store.payerName(payerId)) { payerMenu = true }
                DropdownMenu(expanded = payerMenu, onDismissRequest = { payerMenu = false }) {
                    store.payers.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.name) },
                            onClick = { payerId = p.id; payerMenu = false }
                        )
                    }
                }
            }

            AuraCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Edit, null, tint = AuraColors.GreenDark, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    AuraTextField(
                        value = notes,
                        onChange = { notes = it },
                        label = "הערות (אופציונלי)",
                        singleLine = false
                    )
                }
            }

            ToggleRow(null, "שולם", paid) { paid = it }
            if (paid) {
                FormRow(Icons.Outlined.CalendarMonth, "תאריך תשלום", fmtDate(paidDate)) { dialog = "paidDate" }
            }

            ToggleRow(Icons.Outlined.Notifications, "תזכורת", remindOn) { on ->
                remindOn = on
                if (on) {
                    if (remindBefore == null && !remindPay && !customOn) remindBefore = 1440
                    askNotificationPermission()
                }
            }

            if (remindOn) {
                AuraCard(Modifier.fillMaxWidth()) {
                    Text("תזכורת לפני הפגישה", fontSize = 14.sp, color = AuraColors.Muted)
                    Spacer(Modifier.size(8.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        beforeOptions.forEach { (label, min) ->
                            Pill(label, remindBefore == min, { remindBefore = min })
                        }
                    }
                }
                ToggleRow(null, "תזכורת לסימון תשלום", remindPay) { remindPay = it }
                ToggleRow(null, "תזכורת מותאמת אישית", customOn) { customOn = it }
                if (customOn) {
                    FormRow(Icons.Outlined.CalendarMonth, "תאריך התזכורת", fmtDate(customDate)) { dialog = "customDate" }
                    FormRow(Icons.Outlined.Schedule, "שעת התזכורת", fmtTime(customTime)) { dialog = "customTime" }
                }
            }
            Spacer(Modifier.size(4.dp))
        }

        Box(
            Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            PrimaryButton("שמור פגישה", { save() }, enabled = canSave)
        }
    }

    when (dialog) {
        "date" -> DatePickDialog(date, { dialog = null }) { date = it }
        "time" -> TimePickDialog(time, { dialog = null }) { time = it }
        "paidDate" -> DatePickDialog(paidDate, { dialog = null }) { paidDate = it }
        "customDate" -> DatePickDialog(customDate, { dialog = null }) { customDate = it }
        "customTime" -> TimePickDialog(customTime, { dialog = null }) { customTime = it }
    }
}

@Composable
private fun FormRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    AuraCard(Modifier.fillMaxWidth(), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = AuraColors.GreenDark, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(label, fontSize = 12.sp, color = AuraColors.Muted)
                Text(value, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AuraColors.Ink)
            }
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector?, label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    AuraCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = AuraColors.GreenDark, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(14.dp))
            }
            Text(label, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = AuraColors.Ink)
            Spacer(Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AuraColors.Green,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = AuraColors.Sage,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun AuraTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = AuraColors.Green,
            unfocusedIndicatorColor = AuraColors.Line,
            focusedLabelColor = AuraColors.GreenDark,
            cursorColor = AuraColors.Green
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onPick: (LocalDate) -> Unit) {
    val st = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                st.selectedDateMillis?.let { ms ->
                    onPick(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("אישור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } }
    ) {
        DatePicker(state = st)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(initial: LocalTime, onDismiss: () -> Unit, onPick: (LocalTime) -> Unit) {
    val st = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(LocalTime.of(st.hour, st.minute))
                onDismiss()
            }) { Text("אישור") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = st)
            }
        }
    )
}
