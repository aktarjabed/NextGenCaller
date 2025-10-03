# ProGuard rules for NextGenCaller - Enhanced for Security

# --- General Optimizations & Obfuscation ---
-optimizationpasses 5
-overloadaggressively
-repackageclasses ''
-allowaccessmodification
-dontusemixedcaseclassnames

# Obfuscation Dictionaries
-obfuscationdictionary obfuscationdictionary.txt
-classobfuscationdictionary classobfuscationdictionary.txt
-packageobfuscationdictionary classobfuscationdictionary.txt

# --- Stripping ---
-stripdebug
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Assumptions for Cleaner Code ---
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# --- Keep Rules for Core Android Components ---
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.multidex.MultiDexApplication

# Keep native methods and their classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepnames class * implements android.os.Parcelable

# --- Library-Specific Keep Rules ---

# WebRTC
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**

# Dagger/Hilt
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep class com.nextgencaller.Hilt_MainApplication { *; }
-keep class * implements dagger.hilt.internal.GeneratedComponent { *; }
-keep class * implements dagger.hilt.internal.GeneratedEntryPoint { *; }
-keepclassmembers class * {
    @dagger.hilt.android.AndroidEntryPoint *;
    @javax.inject.Inject <init>(...);
}

# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** DATABASE_NAME;
}
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Firebase & GMS
-keepnames class * implements com.google.android.gms.common.api.Result
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.common.**

# Retrofit, OkHttp, Gson
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep class com.google.gson.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn com.google.gson.**

# Kotlin Coroutines & Serialization
-keepclassmembers class kotlinx.coroutines.internal.MainDispatcherFactory {
    private static final <fields>;
}
-keepclassmembers class kotlinx.coroutines.flow.** {
    *;
}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler
-keepclassmembernames class kotlin.coroutines.jvm.internal.BaseContinuationImpl {
    private <fields>;
}
-dontwarn kotlinx.coroutines.**

# Compose
-keepclassmembernames class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keepclassmembernames class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
-keepclassmembernames class * {
    @androidx.compose.runtime.Composable <methods>;
}
-dontwarn androidx.compose.runtime.internal.ComposableLambda

# --- Application-Specific Keep Rules ---

# Keep data and domain models
-keep class com.nextgencaller.data.model.** { *; }
-keep class com.nextgencaller.domain.model.** { *; }

# Keep ViewModels and their constructors
-keep public class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep public class * extends androidx.lifecycle.AndroidViewModel {
    <init>(...);
}

# Keep custom Application class
-keep public class com.nextgencaller.MainApplication

# Keep native library loader
-keep class org.webrtc.NativeLibraryLoader {
    public static boolean isLoaded();
    public static void initialize(Context context);
}