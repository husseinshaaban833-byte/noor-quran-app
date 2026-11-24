# دليل البناء والتشغيل — تطبيق «نور القرآن»

## المتطلبات
- **Android Gradle Plugin:** `9.1.1`
- **Kotlin / Compose BOM:** `2.4.21` / `2024.09.00`
- **Compile / Target SDK:** `36` (`minSdk = 24`)
- **Java Toolchain:** JDK `11+`

## الأوامر الأساسية
1. **فحص الترجمة البرمجية (Compile Debug Kotlin):**
   ```bash
   gradle :app:compileDebugKotlin
   ```
2. **تشغيل الاختبارات المحلية واختبارات Robolectric:**
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **بناء ملف APK للتجربة (Debug APK):**
   ```bash
   gradle :app:assembleDebug
   ```
   يُخرج الملف في المسار: `app/build/outputs/apk/debug/app-debug.apk`.
4. **بناء حزمة الإصدار (Release APK / AAB):**
   ```bash
   gradle :app:assembleRelease
   gradle :app:bundleRelease
   ```
   *(يتطلب توفير متغيرات التوقيع `KEYSTORE_PATH` و `STORE_PASSWORD` و `KEY_PASSWORD` في بيئة البناء).*

## إعداد خدمات Firebase (اختياري للمزامنة والمساعد الذكي)
- يعمل التطبيق بالكامل دون إنترنت أو حساب مستخدم لجميع ميزات المصحف، التفسير، المواقيت، القبلة، الحفظ، والأذكار.
- لتفعيل المزامنة السحابية والمساعد القرآني الذكي: تأكد من وجود ملف `app/google-services.json` ونشر قواعد `firestore.rules` على مشروع Firebase المرتبط.
