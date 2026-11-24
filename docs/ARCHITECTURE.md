# المعمارية الهندسية — تطبيق «نور القرآن» (Noor Al-Quran)

## 1. النظرة العامة والأنماط المعمارية
يعتمد التطبيق معمارية **MVVM (Model-View-ViewModel)** النظيفة بلغة **Kotlin** وواجهة **Jetpack Compose (Material 3)** مع دعم كامل للغة العربية **RTL** والتصميم التكيفي (`NavigationBar` للهواتف و `NavigationRail` للأجهزة اللوحية).

## 2. الطبقات والمكونات الرئيسية
- **`com.example.core`**:
  - `PrefsDataStore`: تخزين التفضيلات المحلية (آخر موضع قراءة، إعدادات الخطوط والمظهر المستقل عن وضع الصفحات، المدينة وطريقة الحساب، إعدادات الصوت والسبحة).
  - `AppCheckInit`: تهيئة Firebase App Check قبل طلبات Firebase AI Logic و Firestore.
- **`com.example.data.local`**:
  - `NoorDatabase` (Room): جداول العلامات المرجعية (`bookmarks`)، خطط الحفظ بالتكرار المتباعد (`memorization_plans`)، محاولات التسميع (`recitation_attempts`)، خطط الختمة (`khatmah_plans`)، التنزيلات (`downloaded_items`)، سجل السبحة (`tasbeeh_logs`)، وتقدم الأذكار اليومي (`adhkar_progress`).
- **`com.example.quran`**:
  - `QuranRepository`: تحميل المصحف العثماني والتفسير والمفردات وأسباب النزول وفضائل السور ودعاء الختم والبحث السريع وتقرير سلامة النص (`verifyIntegrity()`).
  - `ArabicNormalizer`: تطبيع الحروف والتشكيل للبحث والتسميع دون تعديل النص العثماني الأصلي إطلاقًا.
  - `TajweedHighlighter`: تلوين مواضع التجويد في وضع القراءة المستمرة فقط.
  - `AyahCardRenderer`: توليد بطاقات مصورة عالية الدقة لمشاركة الآيات.
- **`com.example.audio`**:
  - `QuranPlaybackService` (Media3 `MediaSessionService`) + `QuranAudioController`: تشغيل الآيات والسور والإذاعات بالخلفية مع تحكم بالسرعة (0.5x–2.0x مع الحفاظ على طبقة الصوت)، والتكرار، والمزامنة اللحظية.
  - `DownloadRepository`: تنزيل السور والتفاسير مع دعم HTTP `Range` للاستئناف والتحقق ببصمة `SHA-256`.
- **`com.example.memorization`**:
  - `SpacedRepetitionEngine`: جدولة المراجعة المتباعدة وحساب الورد اليومي للختمة.
  - `RecitationAnalyzer`: محاذاة برمجة ديناميكية على مستوى الكلمة لاكتشاف الحذف والإضافة والاستبدال والتقديم والتأخير، مع حارس `"تعذر التأكد"`.
- **`com.example.islamic`**:
  - `PrayerTimesCalculator`: حساب فلكي لمواقيت الصلاة لـ 5 هيئات ومذهبي العصر.
  - `QiblaCalculator` + `HijriCalendarHelper` (تقويم أم القرى).
- **`com.example.ai` & `com.example.cloud`**:
  - `QuranAiAssistantRepository`: مساعد قرآني عبر **Firebase AI Logic (`gemini-3.8-flash`)** مقيد باسترجاع محلي (RAG) وضوابط شرعية صارمة.
  - `CloudSyncRepository` + `AuthHelper`: مصادقة Google الاختيارية عبر Jetpack `CredentialManager` ومزامنة Firestore المحمية بقواعد أمان صارمة، بالإضافة إلى التصدير والاستيراد المحلي بصيغة JSON.
- **`com.example.widget`**:
  - `PrayerTimesWidget` و `DailyAyahWidget` عبر **Jetpack Glance**.
