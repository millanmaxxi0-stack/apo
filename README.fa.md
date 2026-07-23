# ساخت اپلیکیشن واقعی اندروید برای PeyamApp (با نوتیفیکیشن و تماس مثل واتساپ)

## این پکیج چیه؟

دو تا کار جدا اینجا انجام شده:

1. **server-changes/** → تغییراتی که باید در همون پروژه‌ای که روی Railway هاست کردی اعمال کنی (اضافه‌شدن پشتیبانی از Firebase Cloud Messaging کنار Web Push فعلی).
2. **android-app-scaffold/** → یک پروژه‌ی جدید و جدا (یک ریپازیتوری گیت‌هاب جدید) که اپ واقعی اندروید رو می‌سازه؛ این اپ فقط یک "پوسته‌ی نیتیو" دور سایتت هست که آدرس Railway رو داخل خودش لود می‌کنه، پس هر تغییری که بعداً روی سرور Railway بدی، خودکار توی اپ هم دیده می‌شه — نیازی به ساخت مجدد اپ نیست، مگر این‌که خود کد نیتیو (نوتیفیکیشن/تماس) رو عوض کنی.

### چرا Firebase لازمه؟
وب‌پوش (Web Push) که الان داری فقط وقتی کار می‌کنه که مرورگر باز باشه یا Service Worker مرورگر زنده باشه. برای این‌که اپ **حتی وقتی کاملاً بسته (killed) هست** بتونه مثل واتساپ زنگ تماس بزنه و صفحه‌ی تماس رو روی صفحه بیاره، باید از سیستم پوش‌نوتیفیکیشن واقعی گوگل (FCM) استفاده کنیم — این تنها راه استانداردیه که همه‌ی اپ‌های اندرویدی مثل واتساپ/تلگرام هم همینو استفاده می‌کنن.

---

## مرحله ۱ — ساخت پروژه‌ی Firebase (رایگان، ۵ دقیقه)

1. برو به https://console.firebase.google.com و با همون جیمیلت وارد شو.
2. **Add project** بزن، اسمش رو مثلاً `peyam-app` بذار، Google Analytics رو می‌تونی خاموش کنی (لازم نیست).
3. بعد از ساخته‌شدن پروژه، وسط صفحه‌ی داشبورد روی آیکون **Android** بزن تا یک اپ اندروید به پروژه اضافه کنی:
   - **Android package name**: دقیقاً بنویس `com.peyam.app`
   - App nickname: هرچی دوست داری
   - بقیه رو خالی بذار و Register app بزن
4. فایل `google-services.json` رو دانلود کن — این فایل رو یک جای امن نگه دار، بعداً لازمش داری.
5. برگرد به داشبورد پروژه، روی آیکون چرخ‌دنده (⚙️) کنار "Project Overview" بزن → **Project settings**.
6. برو به تب **Service accounts** → دکمه‌ی **Generate new private key** رو بزن. یک فایل JSON دانلود می‌شه — این فایل خیلی حساسه (مثل رمز عبور سرورته)، جایی commit نکن.

الان دو تا فایل داری:
- `google-services.json` → برای اپ اندروید
- یک فایل JSON دیگه (چیزی شبیه `peyam-app-firebase-adminsdk-xxxxx.json`) → برای سرور Railway

---

## مرحله ۲ — بروزرسانی سرور روی Railway

1. فایل‌های داخل پوشه‌ی `server-changes/` رو با فایل‌های متناظرشون توی ریپازیتوری فعلی سرورت جایگزین کن:
   - `server.js`
   - `package.json`
   - `public/index.html`
   - `public/peyam-native.js` (فایل جدید — این یکی رو فقط اضافه کن)
2. توی ریشه‌ی همون ریپو دستور بزن (یا بذار Railway خودش موقع دیپلوی نصب کنه):
   ```
   npm install
   ```
3. محتوای فایل JSON مرحله‌ی قبل (همون service account) رو باز کن، کل متنش رو کپی کن و **یک خط** کن (بدون فاصله‌ی اضافه یا line break — راحت‌ترین راه اینه که با یک ابزار آنلاین JSON minify کنی، یا با این دستور توی ترمینال):
   ```
   node -e "console.log(JSON.stringify(require('./peyam-app-firebase-adminsdk-xxxxx.json')))"
   ```
4. وارد پنل Railway پروژه‌ات شو → تب **Variables** → یک متغیر جدید اضافه کن:
   - Name: `FIREBASE_SERVICE_ACCOUNT`
   - Value: همون رشته‌ی JSON یک‌خطی که ساختی
5. کد رو به گیت‌هاب push کن؛ Railway خودش دوباره دیپلوی می‌کنه. توی لاگ‌های Railway باید این خط رو ببینی:
   ```
   ✅ Firebase Cloud Messaging is ready (native Android push enabled).
   ```

---

## مرحله ۳ — ساخت پروژه‌ی اپ اندروید (Capacitor)

این کارها رو روی کامپیوتر خودت (یا Termux) با Node.js نصب‌شده انجام بده:

```bash
mkdir peyam-app && cd peyam-app
```

حالا تمام فایل‌های داخل پوشه‌ی `android-app-scaffold/` این پکیج (`capacitor.config.json`, `package.json`, `www/`, `android-custom/`, `.github/`) رو داخل همین پوشه‌ی `peyam-app` کپی کن.

```bash
npm install
npx cap add android
```

این دستور یک پوشه‌ی `android/` کامل می‌سازه (پروژه‌ی گرادل واقعی). حالا فایل‌های سفارشی رو داخلش کپی می‌کنیم:

```bash
cp -r android-custom/app/src/main/java/com/peyam/app/*.java  android/app/src/main/java/com/peyam/app/
cp android-custom/app/src/main/res/layout/activity_call.xml   android/app/src/main/res/layout/
cp android-custom/app/src/main/res/drawable/ic_notification.xml android/app/src/main/res/drawable/
```

نکته: مسیر پکیج `com/peyam/app/` رو `npx cap add android` خودش طبق `appId` توی `capacitor.config.json` می‌سازه؛ اگه از قبل یک `MainActivity.java` خالی اونجا بود، همون رو با فایل ما (که توی `android-custom` هست) **جایگزین** کن (overwrite).

فایل `google-services.json` (مرحله‌ی ۱) رو بذار توی:
```
android/app/google-services.json
```

---

## مرحله ۴ — ویرایش دستی دو فایل (فقط کپی-پیست)

سه فایل راهنما توی `android-custom/` هست که دقیقاً بهت می‌گه چی رو کجا اضافه کنی:

- `MERGE_INTO_AndroidManifest.xml.txt` → توی `android/app/src/main/AndroidManifest.xml`
- `MERGE_INTO_root_build.gradle.txt` → توی `android/build.gradle`
- `MERGE_INTO_app_build.gradle.txt` → توی `android/app/build.gradle`

هر سه فایل رو باز کن و دقیقاً طبق توضیحش عمل کن (فقط چندتا خط اضافه کردنه، چیزی رو پاک نمی‌کنی).

---

## مرحله ۵ — بیلد گرفتن روی گیت‌هاب (بدون نیاز به Android Studio)

1. یک ریپازیتوری جدید توی گیت‌هاب بساز (مثلاً `peyam-android`) و کل پوشه‌ی `peyam-app` (شامل `android/`, `.github/`, `capacitor.config.json`, و بقیه) رو push کن.

   ⚠️ فایل `android/app/google-services.json` رو **commit نکن** اگه ریپو پابلیکه. به‌جاش:

2. فایل `google-services.json` رو به base64 تبدیل کن:
   ```bash
   base64 -w0 android/app/google-services.json > gs.b64
   cat gs.b64
   ```
3. توی گیت‌هاب: Settings → Secrets and variables → Actions → **New repository secret**
   - Name: `GOOGLE_SERVICES_JSON_BASE64`
   - Value: همون متن base64
4. حالا فایل واقعی `google-services.json` رو از گیت اضافه کن به `.gitignore` که push نشه:
   ```
   echo "android/app/google-services.json" >> .gitignore
   ```
5. یک push دیگه بزن. برو به تب **Actions** توی گیت‌هاب — یک workflow به اسم "Build PeyamApp APK" خودکار اجرا می‌شه و در چند دقیقه یک فایل APK می‌سازه.
6. وقتی سبز شد، روی همون run کلیک کن → پایین صفحه بخش **Artifacts** → `peyam-debug-apk` رو دانلود کن، فایل zip رو باز کن، `app-debug.apk` رو بگیر و روی گوشیت نصب کن (باید نصب از منابع ناشناس رو فعال کنی).

---

## تست کردن

1. اپ رو نصب و باز کن، با جیمیلت لاگین کن (دقیقاً مثل نسخه‌ی وب).
2. از یک مرورگر یا گوشی دوم با یک اکانت دیگه، برای خودت پیام بفرست → باید نوتیفیکیشن بیاد حتی اگه اپ رو کامل ببندی.
3. تماس صوتی/تصویری بگیر → روی گوشی گیرنده، حتی اگه اپ بسته یا قفل گوشی روشن باشه، باید صفحه‌ی تماس تمام‌صفحه (سبز، با دکمه‌ی قبول/رد) ظاهر بشه.

---

## محدودیت‌های شناخته‌شده‌ی این نسخه‌ی اول (قابل رفع در تکرار بعدی)

- فقط اندروید ساخته شده (نه iOS).
- رینگتون تماس ورودی، رینگتون پیش‌فرض گوشیه (سفارشی نیست).
- آیکون نوتیفیکیشن و آیکون اپ ساده/پیش‌فرض هستن — اگه لوگوی اختصاصی داری بگو جایگزین کنم.
- وقتی از خود نوتیفیکیشن دکمه‌ی «Decline» رو بدون باز کردن صفحه‌ی تماس بزنی، اپ برای یک لحظه به‌طور مخفی باز می‌شه تا رد تماس رو به سرور اطلاع بده (چون توکن ورود فقط داخل اپه). قابل بهینه‌سازی هست.

هر کدوم از این‌ها یا هر رفتار دیگه‌ای رو که موقع تست دیدی نیاز به تغییر داره، دقیقاً بگو کدوم قسمت (کدوم فایل/کدوم رفتار) — روی همین ساختار ادامه می‌دیم.
