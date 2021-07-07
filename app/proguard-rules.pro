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

################################### 混淆配置 start ###########################################
#指定代码的压缩级别
-optimizationpasses 5
#包明不混合大小写
-dontusemixedcaseclassnames
#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses
#优化  不优化输入的类文件
-dontoptimize
#预校验
-dontpreverify
#混淆时是否记录日志
-verbose
# 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
#忽略警告
#-ignorewarning
################################### 混淆配置 end ############################################


################## 记录生成的日志数据,gradle build时在本项目根目录输出         #################
####### 输出文件夹 build/outputs/mapping
#apk 包内所有 class 的内部结构
-dump build/outputs/mapping/class_files.txt
#未混淆的类和成员
-printseeds build/outputs/mapping/kpa_seeds.txt
#列出从 apk 中删除的代码
-printusage build/outputs/mapping/kpa_unused.txt
#混淆前后的映射
-printmapping build/outputs/mapping/kpa_mapping.txt
################## 记录生成的日志数据，gradle build时 在本项目根目录输出-end    #################

################## 常用属性配置-start  ##################
# 保护注解
-keepattributes *Annotation*
# 保护support v4 包
-dontwarn android.support.v4.app.**

# *	匹配任意长度字符，不包含包名分隔符 (.)
# **	匹配任意长度字符，包含包名分隔符 (.)
# ***	匹配任意参数类型
-keep class android.support.v4.app.**{ *; }
# 保护andorid x
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**
# 保护一些奇葩的问题
-dontwarn org.xmlpull.v1.XmlPullParser
-dontwarn org.xmlpull.v1.XmlSerializer
-keep class org.xmlpull.v1.* {*;}

# 保护JS接口
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
##保持 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}
##保持 Parcelable 不被混淆
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
##保持 Serializable 不被混淆
-keepnames class * implements java.io.Serializable
#
#保持 Serializable 不被混淆并且enum 类也不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
#避免混淆泛型 如果混淆报错建议关掉
#–keepattributes Signature
# webview + js
-keepattributes *JavascriptInterface*

################## 常用属性配置-end  ##################

################## kotlin-start  ##################
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}
################## kotlin-end  ##################

################# eventbus-start ##################
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# And if you use AsyncExecutor:
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
################# eventbus-end ##################

################# glide-start ##################
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
################# glide-end ##################

################# immersionbar-start ##################
-keep class com.gyf.immersionbar.* {*;}
-dontwarn com.gyf.immersionbar.**
################# immersionbar-end ##################

################# room-start ##################
-keep class * extends android.arch.persistence.room.RoomDatabase { *; } # suport
-keep class * extends androidx.room.RoomDatabase { *; } # androidx
################# room-end ##################

################# bugly-start ##################
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}
################# bugly-end ##################

################# sqlcipher-start ##################
-dontwarn net.sqlcipher.**
-keep class net.sqlcipher.** {*;}
################# sqlcipher-end ##################

################# wcdb-start ##################
# https://github.com/Tencent/wcdb/blob/master/android/wcdb/proguard-rules.pro
# Keep all native methods, their classes and any classes in their descriptors
-keepclasseswithmembers,includedescriptorclasses class com.tencent.wcdb.** {
    native <methods>;
}

# Keep all exception classes
-keep class com.tencent.wcdb.**.*Exception

# Keep classes referenced in JNI code
-keep class com.tencent.wcdb.database.WCDBInitializationProbe { <fields>; }
-keep,includedescriptorclasses class com.tencent.wcdb.database.SQLiteCustomFunction { *; }
-keep class com.tencent.wcdb.database.SQLiteDebug$* { *; }
-keep class com.tencent.wcdb.database.SQLiteCipherSpec { <fields>; }
-keep interface com.tencent.wcdb.support.Log$* { *; }

# Keep methods used as callbacks from JNI code
-keep class com.tencent.wcdb.repair.RepairKit { int onProgress(java.lang.String, int, long); }
-keep class com.tencent.wcdb.database.SQLiteConnection {
    void notifyCheckpoint(java.lang.String, int);
    void notifyChange(java.lang.String, java.lang.String, long[], long[], long[]);
}
################# wcdb-end ##################

################# webdav-start ##################
-dontwarn org.simpleframework.xml.stream.**
-keep class org.simpleframework.xml.**{ *; }
-keepclassmembers,allowobfuscation class * {
    @org.simpleframework.xml.* <fields>;
    @org.simpleframework.xml.* <init>(...);
}

## Sardine Android model classes: needed for XML serialization
-keep class com.thegrizzlylabs.sardineandroid.model.**{ *; }
## 防止某些奇葩情况下导致的崩溃问题
-keep class javax.xml.namespace.**{
    public <methods>;
}

## OkHTTP
-dontwarn okhttp3.internal.platform.ConscryptPlatform
################# webdav-end ##################

################# arouter-start ##################
-keep public class com.alibaba.android.arouter.routes.**{*;}
-keep public class com.alibaba.android.arouter.facade.**{*;}
-keep class * implements com.alibaba.android.arouter.facade.template.ISyringe{*;}

# 如果使用了 byType 的方式获取 Service，需添加下面规则，保护接口
-keep interface * implements com.alibaba.android.arouter.facade.template.IProvider

# 如果使用了 单类注入，即不定义接口实现 IProvider，需添加下面规则，保护实现
# -keep class * implements com.alibaba.android.arouter.facade.template.IProvider
################# arouter-start ##################

-keep class com.com.lyy.keepassa.baseapi.*{ *; }
-dontwarn com.com.lyy.keepassa.baseapi.**
-keep class * implements com.lyy.keepassa.baseapi.INotFreeLibService{ *; }
-keep class com.lyy.keepassa.view.setting.UISettingFragment
-keep class com.lyy.keepassa.service.autofill.AutofillService{ *; }