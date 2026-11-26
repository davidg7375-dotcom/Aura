package com.aura.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** עדכון מתוך האפליקציה: בודק את ה-Release האחרון בריפו, מוריד את ה-APK ומציע להתקין. */
object Updater {
    private const val LATEST = "https://api.github.com/repos/davidg7375-dotcom/Aura/releases/latest"

    enum class Phase { Idle, Checking, UpToDate, Available, Downloading, Error }

    var phase by mutableStateOf(Phase.Idle)
        private set
    var message by mutableStateOf("")
        private set
    private var apkUrl: String? = null
    private var checkedOnce = false

    fun currentVersion(ctx: Context): Long =
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).longVersionCode

    /** בדיקה שקטה אחת בכל הפעלה של האפליקציה (לכרטיס "יש עדכון" בבית). */
    fun checkOnce(ctx: Context) {
        if (checkedOnce) return
        checkedOnce = true
        check(ctx)
    }

    fun check(ctx: Context) {
        if (phase == Phase.Checking || phase == Phase.Downloading) return
        phase = Phase.Checking
        val app = ctx.applicationContext
        Thread {
            try {
                val c = URL(LATEST).openConnection() as HttpURLConnection
                c.connectTimeout = 10_000
                c.readTimeout = 15_000
                c.setRequestProperty("Accept", "application/vnd.github+json")
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                val o = JSONObject(c.inputStream.bufferedReader().readText())
                val latest = o.getString("tag_name").removePrefix("v").toLongOrNull() ?: 0L
                val assets = o.getJSONArray("assets")
                var url: String? = null
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.getString("name").endsWith(".apk")) url = a.getString("browser_download_url")
                }
                if (latest > currentVersion(app) && url != null) {
                    apkUrl = url
                    message = "גרסה חדשה זמינה (${latest})"
                    phase = Phase.Available
                } else {
                    message = "האפליקציה מעודכנת"
                    phase = Phase.UpToDate
                }
            } catch (e: Exception) {
                message = "לא הצלחתי לבדוק עדכון. בדקו חיבור לאינטרנט."
                phase = Phase.Error
            }
        }.start()
    }

    /** מוריד את ה-APK ומפעיל את מתקין המערכת. */
    fun downloadAndInstall(ctx: Context) {
        val url = apkUrl ?: return
        if (phase == Phase.Downloading) return
        val app = ctx.applicationContext
        // נדרש אישור חד-פעמי להתקנה ממקור זה
        if (!app.packageManager.canRequestPackageInstalls()) {
            val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(i)
            message = "אשרו התקנה מהאפליקציה ואז לחצו שוב"
            return
        }
        phase = Phase.Downloading
        message = "מוריד…"
        Thread {
            try {
                val dir = File(app.cacheDir, "updates").also { it.mkdirs() }
                val f = File(dir, "Breathe.apk")
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 30_000
                c.instanceFollowRedirects = true
                if (c.responseCode != 200) throw IllegalStateException("HTTP ${c.responseCode}")
                c.inputStream.use { inp -> f.outputStream().use { out -> inp.copyTo(out) } }
                val uri = FileProvider.getUriForFile(app, "${app.packageName}.updates", f)
                val i = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                app.startActivity(i)
                message = "ההתקנה נפתחה"
                phase = Phase.Available
            } catch (e: Exception) {
                message = "ההורדה נכשלה. נסו שוב."
                phase = Phase.Error
            }
        }.start()
    }
}
