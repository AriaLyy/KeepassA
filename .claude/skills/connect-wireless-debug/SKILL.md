---
name: connect-wireless-debug
description: Use when setting up or troubleshooting Android Studio / ADB wireless debugging — adb connect failures, empty device list, daemon stuck state, switching from USB to Wi-Fi, or pairing Android 11+ devices over TCP
---

# Connect Wireless Debug

## Overview

通过 Wi-Fi(而非 USB)连接 Android 设备进行 ADB 调试。KeepassA 项目跑在真机上时,无线调试可以避免 USB 线物理束缚,尤其在测试自动填充 / 锁屏 / 通知 / 后台启动等场景时更顺手。

ADB 是 client-server 架构:**后台常驻的 `adb.exe` daemon(监听本机 tcp:5037)负责设备通信,命令行 client 转发请求给它**。绝大多数"连不上"的根因不是网络、不是手机,而是 **daemon 状态卡死**——daemon 进程活着,但内部 socket / mDNS 探测通道僵死,拒绝把新连接登记进设备列表。重启 daemon 即可。

## Prerequisites

1. **手机 Android 11(API 30)及以上**——才有原生的"无线调试"。11 以下只能用旧式 `adb tcpip 5555`(需要先用 USB 切换,稳定性差,本 skill 不覆盖)。
2. **ADB ≥ 30**(`platform-tools 30+`)。验证:`adb version`。本项目环境 `D:\Dev\sdk\platform-tools\adb.exe`。
3. **同网段**:电脑和手机连同一个 Wi-Fi,且该 Wi-Fi 不开启 AP 隔离 / 客户端隔离(路由器设置)。
4. **手机端已开启无线调试**:设置 → 系统 → 开发者选项 → 无线调试(开关 + 详情页都要进)。

## Two Ports (Android 11+ 关键概念)

无线调试详情页有**两个不同的端口**,极易混淆:

| 位置 | 用途 | 命令 |
|------|------|------|
| 详情页主页显示的 `IP:Port`(如 `192.168.50.205:45499`) | **连接端口**——已配对过的设备直连 | `adb connect <IP:Port>` |
| 点击"使用配对码配对设备"后显示的 `IP:Port` + 6 位配对码 | **配对端口**——首次配对 / 重置过 | `adb pair <IP:PairPort>` 然后输入配对码 |

**两个端口不一样**,且每次关闭再开启无线调试、手机重启后都会变。**配对关系保留,无需重新扫码**;端口会换,需要重新看屏幕报。

## Quick Reference

```bash
# 0. 万能起手式——daemon 卡死时必备
adb kill-server && adb start-server

# 1. 查看当前设备(USB + 无线全部列出)
adb devices -l

# 2. 已配对过,直连(端口看手机屏幕)
adb connect 192.168.50.205:45499

# 3. 首次配对(进手机"使用配对码配对设备"页,记下配对端口和 6 位码)
adb pair 192.168.50.205:配对端口
# 按提示输入配对码,成功后回到主页用连接端口 adb connect

# 4. 断开单个设备
adb disconnect 192.168.50.205:45499

# 5. 断开所有
adb disconnect
```

## Typical Workflow

### 首次配对(新设备 / 重置过)

```bash
# 1. 确认 ADB 版本 ≥ 30
adb version

# 2. daemon 清状态(永远从这一步开始)
adb kill-server && adb start-server

# 3. 手机进入"使用配对码配对设备"页面,记下:
#    - 配对 IP:端口(例如 192.168.50.205:37xxx)
#    - 6 位配对码

# 4. 配对
adb pair 192.168.50.205:37215
# 输入配对码,期望输出:Successfully paired to ...

# 5. 切到主页,用主页显示的连接端口(不是配对端口!)
adb connect 192.168.50.205:45499

# 6. 验证
adb devices -l
# 期望看到:model/transport_id 字段,状态为 device(不是 offline / unauthorized)
```

### 日常复连(已配对过)

```bash
# 端口会变,每次以手机屏幕当前显示为准
adb kill-server && adb start-server
adb connect 192.168.50.205:<当前端口>
adb devices -l
```

### 在 Android Studio 中使用

`adb connect` 成功后,Android Studio 的设备选择器(Running Devices 面板)会自动列出该 TCP 设备,与 USB 设备一视同仁,直接 Run / Debug 即可。

## Common Mistakes

| 症状 | 原因 | 解决 |
|------|------|------|
| `adb devices` 列表为空,但 `adb connect` 命令本身能成功 | **daemon 状态卡死**(Windows 极常见,尤其有 Hyper-V/WSL 虚拟网卡时) | `adb kill-server && adb start-server`,然后重连 |
| `adb connect` 提示 `connected`,但列表里显示 `offline` | daemon 没完全启动就连接,或手机端无线调试刚关又开 | kill-server 重来,等手机屏幕显示端口稳定后再连 |
| `unable to connect to ...: cannot connect to ...: No connection could be made` | 不同 Wi-Fi / 防火墙 / AP 隔离 / 端口输错了 | `ping <手机IP>` 验证;关闭电脑防火墙试一次;检查路由器 AP 隔离 |
| `failed to authenticate ...` 或一直 `unauthorized` | 手机端有未响应的"允许 USB 调试"弹窗 / 之前授权过期 | 下拉通知栏看调试授权弹窗;或撤销 USB 调试授权后重新 pair |
| `adb pair` 提示 `cannot connect to ... at ...: timeout` | 配对端口输错了(把连接端口当配对端口用) | 仔细看手机"使用配对码配对设备"页面顶部显示的端口,跟主页不同 |
| 配对成功但 `adb connect` 立刻 `device offline` | 端口输错了(把配对端口当连接端口用) | 用主页显示的连接端口,不是配对页的端口 |
| 重启手机后连不上 | 无线调试关闭了 / 端口变了 | 进设置确认开关仍开着,看新端口号重连 |
| 电脑有 WSL2 / Hyper-V vEthernet 适配器 | 路由表混乱,ADB 走错网卡 | 控制面板 → 网络 → 适配器设置,临时禁用虚拟网卡验证 |
| `adb server version doesn't match this client` | 多个 platform-tools 版本互相冲突(Android Studio 自带的 vs 独立安装的) | `where adb` 找出所有副本,确保 PATH 里只有一个 |
| 连接 1 秒后掉线 / 间歇性断开 | Wi-Fi 信号弱 / 路由器节能策略 / 手机省电模式 | 关手机省电;路由器关 AP 隔离;换 5GHz 频段 |

## Verification

连接成功的标志:

1. `adb devices -l` 输出包含该 TCP 端口的设备,状态为 `device`(不是 `offline` / `unauthorized`)
2. 输出包含 `model:` 字段(例如 `model:aurorapro`),证明 ADB 协议握手完成
3. `adb shell getprop ro.product.model` 能返回型号字符串
4. Android Studio → Running Devices 面板能看到设备,Run App 下拉菜单可选

```bash
# 一行验证
adb -s 192.168.50.205:45499 shell getprop ro.product.model
```

## Out of Scope

- **Android 10 及以下旧式无线调试**(`adb tcpip 5555` + USB 切换流程)——稳定性差,推荐升级设备或换 USB
- **通过中继服务器 / 云ADB 跨网调试**——需要额外端口转发或 frp 配置,不在本 skill 范围
- **无线调试自身被检测 / 反调试规避**——KeepassA 不涉及,看反逆向相关资料
- **ADB over Bluetooth**——实验性功能,本 skill 不覆盖
- **iOS / 鸿蒙设备的无线调试**——仅 Android
