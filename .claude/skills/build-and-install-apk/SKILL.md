---
name: build-and-install-apk
description: Use when compiling, building, packaging, or installing the KeepassA Android APK — gradle assemble/install/bundle tasks, build variants (devDebug/devRelease), signing setup, device deployment, or build failure troubleshooting
---

# Build and Install APK

## Overview

KeepassA 是基于 Gradle 的 Android 原生项目,包含 CMake/NDK 原生代码。构建前必须确保签名配置就绪——**即使是 debug 变体也使用 release 签名**(见 `app/build.gradle` 的 `signingConfigs.release` 引用)。

## Prerequisites

构建前必须满足:

1. **`local.properties` 已配置签名**(根目录,不会进 git):
   ```properties
   storeFile=<keystore 绝对路径>
   storePassword=<keystore 密码>
   keyAlias=<key 别名>
   keyPassword=<key 密码>
   msalKey=<OneDrive MSAL 签名 hash>
   ```
   缺任何一项,`devDebug` 都会构建失败。

2. **`app/google-services.json` 已下载放置**(Firebase 控制台 → 项目设置 → 下载配置文件)。文件在 .gitignore 中,仓库里没有;缺失会触发 `processDevDebugGoogleServices` 任务失败。

3. **JDK 17**(项目强制 `sourceCompatibility`/`targetCompatibility = 17`,`jvmTarget = "17"`)。JDK 17-21 均可,低于 17 会报 `Unsupported class file major version`。

3. **Android SDK + NDK 21.4.7075529 + CMake**(原生模块 `src/main/jni/CMakeLists.txt` 依赖)。

4. **设备/模拟器已连接**(用 `adb devices` 确认),否则只能 `assemble`,不能 `install`。

## Build Variants

只有 `dev` 一个 flavor,组合两个 build type:

| Variant | 用途 | ABI | 路径 |
|---------|------|-----|------|
| `devDebug` | 日常开发调试 | x86 / x86_64 / armeabi-v7a / arm64-v8a | `app/build/outputs/apk/dev/debug/app-dev-debug.apk` |
| `devRelease` | 正式发布包 | arm64-v8a only | `app/build/outputs/apk/dev/release/app-dev-release.apk` |

## Quick Reference

| 任务 | 命令 |
|------|------|
| 编译 debug APK | `./gradlew assembleDevDebug` |
| 编译 release APK | `./gradlew assembleDevRelease` |
| 编译 AAB (上架) | `./gradlew bundleDevRelease` |
| 编译并安装 debug | `./gradlew installDevDebug` |
| 编译并安装 release | `./gradlew installDevRelease` |
| 清理构建 | `./gradlew clean` |
| 列出已连接设备 | `adb devices` |
| 手动安装 APK | `adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk` |
| 指定设备安装 | `adb -s <serial> install -r <apk 路径>` |
| 卸载 | `adb uninstall com.lyy.keepassa` |

Windows 环境 shell 用 `./gradlew`(git bash)或 `gradlew.bat`(cmd/powershell)。

## Typical Workflow

```bash
# 1. 确认设备在线
adb devices

# 2. 编译并安装(增量,日常用)
./gradlew installDevDebug

# 3. 启动主 Activity
adb shell am start -n com.lyy.keepassa/.view.main.MainActivity
```

如需彻底重编(改了原生代码或资源混淆):
```bash
./gradlew clean installDevDebug
```

## Common Mistakes

| 症状 | 原因 | 解决 |
|------|------|------|
| `Keystore file ... not found` | `local.properties` 缺 `storeFile` | 补齐签名配置 |
| `Signing config "release" not found` | 同上,gradle 解析失败 | 同上 |
| `processDevDebugGoogleServices FAILED ... File google-services.json is missing` | `app/google-services.json` 未放置 | 从 Firebase 控制台下载,放到 `app/` 根目录 |
| `Unsupported class file major version` | JDK 版本 < 17 | 切到 JDK 17-21;AS 自带 JBR 21 可用(`D:\Program Files\Android\Android Studio\jbr`) |
| `installDevDebug` 失败 `no connected devices` | 设备未连接/未授权 | `adb devices` 查看;USB 调试授权弹窗确认 |
| 多设备时装错 | `installDevDebug` 找不到唯一目标 | 改用 `adb -s <serial> install -r <apk>` |
| 首次编译超过 5 分钟 | CMake/NDK 编译原生代码 | 正常,后续增量会快 |
| `Unsupported class file major version` | JDK 版本不对 | 切到 JDK 17(`java -version` 验证) |
| 资源/代码改动看不到 | 未重新安装 | `./gradlew installDevDebug` 或 `adb install -r` |
| release 包比 debug 大很多 | release 含 R8/混淆但未开启 minify? 实际是 debug 多 ABI | 正常现象,见上表 ABI 列 |

## Native Build Notes

- `app/src/main/jni/CMakeLists.txt` 是 KeepassLib 原生部分
- 修改 `.c`/`.cpp`/`CMakeLists.txt` 后必须 `./gradlew clean` 再编,增量有时不重链
- NDK 版本固定 `21.4.7075529`,SDK Manager 里没有就单独装

## Verification

构建成功后:
1. APK 文件存在于上表路径(`ls app/build/outputs/apk/dev/debug/`)
2. `adb shell pm list packages | grep keepassa` 能看到 `com.lyy.keepassa`
3. 启动后不立即 crash(检查 `adb logcat | grep -i "keepassa\|FATAL"`)

## Out of Scope

- 签名密钥生成(找 Android 文档)
- 渠道包配置(项目当前只有 `dev` flavor)
- CI/CD 自动化(无现成配置)
- Firebase/Crashlytics 上传 mapping 文件(由 `firebaseCrashlytics` 插件处理,正常构建即可)
