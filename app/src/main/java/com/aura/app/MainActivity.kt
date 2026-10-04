package com.aura.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.util.Locale

sealed interface Route {
    data object Home : Route
    data object Sessions : Route
    data object Debts : Route
    data object Payers : Route
    data object Calendar : Route
    data object Settings : Route
    data class Details(val id: String) : Route
    data class Form(val editId: String? = null, val date: LocalDate? = null) : Route
    data class PayerSessions(val payerId: String) : Route
}

class Nav {
    val stack = mutableStateListOf<Route>(Route.Home)
    val current: Route get() = stack.last()
    fun go(r: Route) {
        stack.add(r)
    }
    fun tab(r: Route) {
        stack.clear()
        stack.add(r)
    }
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // כל האפליקציה בעברית, גם אם שפת המכשיר שונה
        val he = Locale.forLanguageTag("he-IL")
        Locale.setDefault(he)
        val cfg = Configuration(newBase.resources.configuration)
        cfg.setLocale(he)
        cfg.setLayoutDirection(he)
        super.attachBaseContext(newBase.createConfigurationContext(cfg))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        Reminders.ensureChannel(this)
        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

@Composable
fun AuraApp() {
    val ctx = LocalContext.current.applicationContext
    val store = remember { AuraStore(ctx) }
    val nav = remember { Nav() }

    BackHandler(enabled = nav.stack.size > 1) { nav.back() }

    AuraBackground {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (val r = nav.current) {
                    is Route.Home -> HomeScreen(store, nav)
                    is Route.Sessions -> SessionsScreen(store, nav)
                    is Route.Debts -> DebtsScreen(store, nav)
                    is Route.Payers -> PayersScreen(store, nav)
                    is Route.Calendar -> CalendarScreen(store, nav)
                    is Route.Settings -> SettingsScreen(store, nav)
                    is Route.Details -> DetailsScreen(store, nav, r.id)
                    is Route.Form -> FormScreen(store, nav, r.editId, r.date)
                    is Route.PayerSessions -> SessionsScreen(store, nav, r.payerId)
                }
            }
            if (nav.current !is Route.Form) {
                BottomBar(nav)
            }
        }
    }
}
