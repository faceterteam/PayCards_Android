# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/alexey/android-sdk-linux/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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

-dontoptimize
-dontobfuscate

-keep class cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore { *; }
-dontnote cards.pay.paycardsrecognizer.sdk.ndk.RecognitionCore

-keep class cards.pay.paycardsrecognizer.sdk.* {
    public protected *;
}

-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-keep class cards.pay.paycardsrecognizer.sdk.ndk.* {
    public protected *;
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

#Library resources
-keep public class **.R {
  public *;
}
-keep public class **.R$* {
  public *;
}