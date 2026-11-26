# Breathe — space to breathe

אפליקציית Android אישית לניהול פגישות עם מטפל ולמעקב אחר תשלומים.
ממשק בעברית מלאה וב־RTL, נתונים נשמרים מקומית במכשיר.

## איך מקבלים APK

### אפשרות 1 — GitHub (בלי להתקין כלום)
1. צרו ב־GitHub repository **פרטי** חדש והעלו אליו את כל תוכן התיקייה הזו (כולל `.github` ו־`keystore`).
2. היכנסו ללשונית **Actions** ← **Build Breathe APK** ← **Run workflow** (או פשוט בצעו push).
3. בסיום הריצה, תחת **Artifacts** הורידו `Breathe-release-apk` — בתוכו `app-release.apk`.
4. העבירו את הקובץ לטלפון ופתחו אותו (יש לאשר התקנה ממקור לא מוכר).

### אפשרות 2 — Android Studio
פתחו את התיקייה ב־Android Studio, חכו לסנכרון Gradle, ואז
**Build ← Build Bundle(s) / APK(s) ← Build APK(s)**, או משורת הפקודה:

```
gradle assembleRelease
```

הקובץ ייווצר ב־`app/build/outputs/apk/release/`.

## חתימה
ה־APK נחתם במפתח קבוע שנשמר ב־GitHub Secrets (`KEYSTORE_B64`), כדי שגרסאות עתידיות יותקנו מעל הקיימת בלי למחוק נתונים.
זו אפליקציה אישית; מומלץ להשאיר את ה־repository פרטי.

## מבנה
- `Data.kt` — מודלים, שמירה לקובץ JSON, חישובי חובות
- `Reminders.kt` — תזכורות והתראות
- `Theme.kt`, `Components.kt` — שפה עיצובית וכרטיסים משותפים
- `Screens.kt`, `FormScreen.kt`, `DebtsPayers.kt`, `CalendarSettings.kt` — המסכים
- `MainActivity.kt` — ניווט

## כללי חוב
חוב = פגישה שכבר התקיימה ולא סומנה כשולמה. פגישה עתידית שטרם שולמה מוצגת כ"קרוב" ואינה נספרת כחוב עד שמועד הפגישה עובר.
