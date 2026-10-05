# بناء APK أونلاين بدون Android Studio

هذا الإصدار يحتوي على GitHub Actions جاهز لبناء APK تلقائياً.

## الطريقة
1. أنشئ مستودعاً جديداً على GitHub.
2. ارفع **محتويات مجلد IstirahaWebView** إلى جذر المستودع (وليس ملف ZIP كملف واحد).
   يجب أن ترى في الجذر: `app` و `.github` و `build.gradle.kts` و `settings.gradle.kts`.
3. افتح تبويب **Actions** في المستودع.
4. اختر **Build Istiraha APK** ثم **Run workflow**.
5. انتظر حتى تظهر علامة النجاح الخضراء.
6. افتح نتيجة التشغيل وانزل إلى **Artifacts**.
7. حمّل `Istiraha-debug-apk` ثم فك ضغطه لتحصل على `app-debug.apk`.

## ملاحظة
هذه نسخة Debug مناسبة للتجربة والتثبيت المباشر. للنشر الرسمي في Google Play يلزم إنشاء نسخة Release موقعة بمفتاح توقيع خاص بك.
