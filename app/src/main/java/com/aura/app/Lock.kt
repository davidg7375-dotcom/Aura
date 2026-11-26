package com.aura.app

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest
import java.security.SecureRandom

/** קוד כניסה בן 4 ספרות. נשמר כ-hash עם salt בלבד (לא הקוד עצמו), בנפרד מקובץ הנתונים. */
object AppLock {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("aura_lock", Context.MODE_PRIVATE)

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

    fun isSet(ctx: Context): Boolean = prefs(ctx).getString("hash", null) != null

    fun set(ctx: Context, pin: String) {
        val salt = ByteArray(12).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        prefs(ctx).edit().putString("salt", salt).putString("hash", hash(pin, salt)).apply()
    }

    fun verify(ctx: Context, pin: String): Boolean {
        val p = prefs(ctx)
        val salt = p.getString("salt", null) ?: return false
        return p.getString("hash", null) == hash(pin, salt)
    }
}

/** מצב נעילה גלובלי: האפליקציה נפתחת נעולה, ונעולה שוב אחרי זמן ברקע. */
object LockState {
    var locked by mutableStateOf(true)
}

@Composable
private fun PinPad(title: String, subtitle: String, error: Boolean, onComplete: (String) -> Unit) {
    var pin by remember(title) { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AuraLogo(size = 40)
        Spacer(Modifier.height(28.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            if (error) "קוד שגוי, נסו שוב" else subtitle,
            fontSize = 14.sp,
            color = if (error) AuraColors.DueFg else AuraColors.Muted
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) AuraColors.Green else AuraColors.Sage)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                r.forEach { k ->
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (k.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else AuraColors.Card)
                            .then(
                                if (k.isEmpty()) Modifier else Modifier.clickable {
                                    if (k == "⌫") pin = pin.dropLast(1)
                                    else if (pin.length < 4) {
                                        pin += k
                                        if (pin.length == 4) {
                                            val done = pin
                                            pin = ""
                                            onComplete(done)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(k, fontSize = 26.sp, color = AuraColors.Ink)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** מסך הכניסה: קביעת קוד בפעם הראשונה, ואחר כך הזנת קוד בכל פתיחה. */
@Composable
fun LockGate(content: @Composable () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    if (!LockState.locked) {
        content()
        return
    }
    var first by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    val isSet = remember { AppLock.isSet(ctx) }
    AuraBackground {
        if (isSet) {
            PinPad("הזינו קוד כניסה", "4 ספרות", error) { p ->
                if (AppLock.verify(ctx, p)) { error = false; LockState.locked = false } else error = true
            }
        } else if (first == null) {
            PinPad("קביעת קוד כניסה", "בחרו קוד בן 4 ספרות", error) { p -> first = p; error = false }
        } else {
            PinPad("אימות הקוד", "הזינו שוב את אותו קוד", error) { p ->
                if (p == first) { AppLock.set(ctx, p); LockState.locked = false } else { error = true; first = null }
            }
        }
    }
}

/** שינוי קוד מההגדרות: קוד נוכחי ← קוד חדש ← אימות. */
@Composable
fun ChangePinDialog(onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var step by remember { mutableStateOf(0) }
    var fresh by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize().background(AuraColors.Bg)) {
            when (step) {
                0 -> PinPad("הקוד הנוכחי", "הזינו את הקוד הקיים", error) { p ->
                    if (AppLock.verify(ctx, p)) { error = false; step = 1 } else error = true
                }
                1 -> PinPad("קוד חדש", "בחרו קוד בן 4 ספרות", false) { p -> fresh = p; step = 2 }
                else -> PinPad("אימות הקוד החדש", "הזינו שוב", error) { p ->
                    if (p == fresh) { AppLock.set(ctx, p); onDismiss() } else { error = true; step = 1 }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.statusBarsPadding()) { Text("ביטול") }
        }
    }
}
