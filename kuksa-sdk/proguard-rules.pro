# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn java.lang.invoke.StringConcatFactory

# don't shrink/remove classes; even if unused
# don't shrink/remove public methods and fields; even if unused
# don't rename class and public/protected methods / field names
# but rename private methods and fields
-keep class org.eclipse.kuksa.DataBrokerConnection {
    public protected *;
}

-keep class org.eclipse.kuksa.DataBrokerConnector {
    public protected *;
}

-keep class com.etas.kuksa.sdk.model.** {
    public protected *;
}

-keep class org.eclipse.kuksa.TimeoutConfig { *; }
-keep interface org.eclipse.kuksa.DataBrokerException { *; }
-keep interface org.eclipse.kuksa.PropertyObserver { *; }

# used for java compatibility
-keep class org.eclipse.kuksa.CoroutineCallback { *; }
