package com.aura.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- לוגו ----------

@Composable
fun AuraLogo(modifier: Modifier = Modifier, size: Int = 34) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.breathe_logo),
                contentDescription = "BREATHE",
                modifier = Modifier.size((size * 1.35f).dp).clip(RoundedCornerShape((size * 0.35f).dp))
            )
            Spacer(Modifier.width((size * 0.35f).dp))
            Text(
                text = "BREATHE",
                fontSize = (size * 0.78f).sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (size / 7).sp,
                color = AuraColors.GreenDark
            )
        }
    }
}

// ---------- כרטיס ----------

@Composable
fun AuraCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val clickMod = if (onClick != null) Modifier.clip(shape).clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier,
        shape = shape,
        color = AuraColors.Card,
        border = BorderStroke(1.dp, AuraColors.Line)
    ) {
        Column(Modifier.then(clickMod).padding(16.dp), content = content)
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    onClick: (() -> Unit)? = null
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(AuraColors.Sage)
            .then(clickMod),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = AuraColors.GreenDark, modifier = Modifier.size(size / 2))
    }
}

@Composable
fun Avatar(modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(AuraColors.Avatar),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.Person,
            contentDescription = null,
            tint = AuraColors.GreenDark,
            modifier = Modifier.size(size / 2)
        )
    }
}

// ---------- סטטוס תשלום ----------

@Composable
fun StatusChip(status: PayStatus, modifier: Modifier = Modifier) {
    val bg: Color
    val fg: Color
    val label: String
    when (status) {
        PayStatus.Paid -> { bg = AuraColors.PaidBg; fg = AuraColors.PaidFg; label = "שולם" }
        PayStatus.Due -> { bg = AuraColors.DueBg; fg = AuraColors.DueFg; label = "לא שולם" }
        PayStatus.Soon -> { bg = AuraColors.SoonBg; fg = AuraColors.SoonFg; label = "קרוב" }
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        when (status) {
            PayStatus.Paid -> Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(15.dp))
            PayStatus.Due -> Icon(Icons.Rounded.Error, null, tint = fg, modifier = Modifier.size(15.dp))
            PayStatus.Soon -> Box(Modifier.size(8.dp).clip(CircleShape).background(fg))
        }
    }
}

fun statusColor(status: PayStatus): Color = when (status) {
    PayStatus.Paid -> AuraColors.PaidFg
    PayStatus.Due -> AuraColors.DueFg
    PayStatus.Soon -> AuraColors.SoonFg
}

// ---------- כרטיס פגישה (משותף לכל המסכים) ----------

@Composable
fun SessionCard(s: Session, store: AuraStore, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val status = store.statusOf(s)
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = AuraColors.Card,
        border = BorderStroke(1.dp, AuraColors.Line)
    ) {
        Row(
            Modifier
                .clip(shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${fmtDateShort(s.date)} · ${fmtTime(s.time)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AuraColors.Ink
                )
                val who = store.payerName(s.payerId)
                val line = when (status) {
                    PayStatus.Paid -> "שולם על ידי $who"
                    PayStatus.Due -> "$who · לא שולם"
                    PayStatus.Soon -> "$who · קרוב"
                }
                Text(line, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = statusColor(status))
            }
            Text(fmtMoney(s.price), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
        }
    }
}

// ---------- כותרת מסך ----------

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "חזרה", tint = AuraColors.Ink)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = AuraColors.Ink)
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

// ---------- כפתורים ----------

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AuraColors.Green,
            contentColor = Color.White,
            disabledContainerColor = AuraColors.Green.copy(alpha = 0.4f),
            disabledContentColor = Color.White
        )
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val c = if (danger) AuraColors.DueFg else AuraColors.GreenDark
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, c.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = c)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun Pill(label: String, selected: Boolean, onClick: () -> Unit, trailing: ImageVector? = null) {
    val bg = if (selected) AuraColors.Green else AuraColors.Card
    val fg = if (selected) Color.White else AuraColors.Ink
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = fg, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        if (trailing != null) {
            Icon(trailing, null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

// ---------- סרגל ניווט תחתון ----------

@Composable
fun BottomBar(nav: Nav) {
    val root = nav.stack.first()
    Surface(color = AuraColors.Card, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem("בית", root == Route.Home, Icons.Rounded.Spa, Icons.Rounded.Spa, Modifier.weight(1f)) {
                nav.tab(Route.Home)
            }
            NavItem("פגישות", root == Route.Sessions, Icons.Rounded.Event, Icons.Rounded.Event, Modifier.weight(1f)) {
                nav.tab(Route.Sessions)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AuraColors.Green)
                        .clickable { nav.go(Route.Form()) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, "הוספת פגישה", tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
            NavItem("חובות", root == Route.Debts, Icons.Rounded.AccountBalanceWallet, Icons.Rounded.AccountBalanceWallet, Modifier.weight(1f)) {
                nav.tab(Route.Debts)
            }
            NavItem("משלמים", root == Route.Payers, Icons.Rounded.Group, Icons.Rounded.Group, Modifier.weight(1f)) {
                nav.tab(Route.Payers)
            }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    filled: ImageVector,
    outlined: ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = if (selected) AuraColors.GreenDark else AuraColors.Muted
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(if (selected) filled else outlined, label, tint = c, modifier = Modifier.size(26.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = c,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
