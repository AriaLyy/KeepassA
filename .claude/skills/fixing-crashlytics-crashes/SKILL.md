---
name: fixing-crashlytics-crashes
description: Use when fixing production crashes from Firebase Crashlytics for KeepassA — listing top crashes, diagnosing root cause, TDD-driven repair, or writing post-fix acceptance checklist to SiYuan. Triggers on: crashlytics 崩溃/firebase crash/线上崩溃/fatal ANR/生产环境异常/Crashlytics issue.
---

# 修复 Crashlytics 崩溃

## 概览

将 Firebase Crashlytics 报警 → 根因定位 → TDD 修复 → SiYuan 验收清单串成一个可重复执行的工作流。三阶段一次性走完，每阶段都有明确退出条件。

**核心原则**：
- 崩溃必须先用失败测试**复现**，再修复（RED 先于 GREEN）
- 修复完成前**不写验收清单**，避免虚假完成感
- SiYuan 路径严格遵守 `doc-ops` 约定（仅限 `/KeepassA/技术方案/崩溃修复/`）

## 前置条件（强制）

每次进入流程前，按顺序核对：

1. **Firebase MCP 已加载 Crashlytics 工具集**
   - 工具列表中必须能找到 `mcp__firebase__crashlytics_get_report`
   - 找不到时：确认 `~/.claude.json` 中 firebase MCP 的 `--dir` 指向 `D:/Dev/workspace/KeepassA`（Android 项目根），然后**重启 Claude Code** 让自动检测加载 Crashlytics 工具
   - 代理（如需）：`HTTP_PROXY/HTTPS_PROXY=http://127.0.0.1:7890`

2. **KeepassA 项目元数据**（已固化，无需用户再提供）：
   - `projectId`: `keepassa-c7022`
   - `appId`: `1:227935972650:android:a3019e5dcbf1351598c253`
   - 包名: `com.lyy.keepassa`

3. **必需子 skill**（开始前先调用 Skill 工具加载）：
   - `tdd` — 修复阶段的 RED-GREEN-REFACTOR
   - `doc-ops` — SiYuan 文档读写
   - `build-and-install-apk` — 验证阶段构建

## 阶段 1：发现崩溃

### 1.1 查询 Top Issues

调用 `mcp__firebase__crashlytics_get_report`：

```
projectId: keepassa-c7022
appId:    1:227935972650:android:a3019e5dcbf1351598c253
reportType: topIssues
issueErrorTypes: [FATAL]      # 默认；ANR 改 [ANR]，非致命改 [NON_FATAL]
# 时间范围默认 7 天；用户指定时同时填 intervalStartTime / intervalEndTime（毫秒时间戳，90 天内）
```

### 1.2 展示 Top 5

按官方格式输出（不改写、不省略 issue id）：

```
N. Issue <full issue id>
   - <issue title>
   - <issue subtitle>
   - **Description:** <事件数 / 影响用户数 / 涉及版本>
```

排序优先级（用户未指定时）：
1. 涉及最新 app 版本的 issue
2. 影响用户数最多
3. 事件数最多

### 1.3 辅助报告（可选）

用户问"哪些版本/机型受影响"时：
- `topVersions` — 按版本聚合
- `topAndroidDevices` — 按设备聚合
- 以上报告均可在 filter 中传 `issueId` 缩窄到具体 issue

## 阶段 2：TDD 修复

### 2.1 用户选定

**必须等用户明确指定**要修复哪个 issue（issue id 或序号），禁止自动选择 top 1 开始改。

### 2.2 取样定位

1. 调用 `mcp__firebase__crashlytics_get_report` 取 `topVariants`，filter 中带 `issueId`，从响应中拿到 `sampleEventUri`
2. 调用 `mcp__firebase__crashlytics_batch_get_events` 拉取样本事件，提取：
   - 堆栈顶帧（自定义异常类 + 方法 + 行号）
   - 设备/OS/版本/变体
   - 自定义日志（`logs` 字段，经常含崩溃前最后状态）
3. 用 Grep / Read 在 `app/src/main/java/com/lyy/keepassa/` 定位崩溃源

### 2.3 加载 TDD skill

```
Skill({ skill: "tdd" })
```

**铁律**：
- 先写**能复现该崩溃**的失败测试（RED），不许直接改代码
- 复现不出来的崩溃 — 先用 `diagnosing-bugs` skill 深挖根因，禁止凭堆栈猜修复
- 单元测试无法复现的（例如 IME 服务、Android 框架回调）— 写 instrumented test (`app/src/androidTest/`) 或 Robolectric 测试，理由写进验收清单

### 2.4 TDD 循环

按 `tdd` skill 的纵向切片：
1. **RED**：一个测试 → 复现崩溃 → 测试失败
2. **GREEN**：最小修改让测试通过 → 不引入额外抽象
3. **REFACTOR**：清理但不改行为

涉及多个崩溃变体时，每个变体一轮 RED-GREEN，不许批量写测试再批量改代码。

## 阶段 3：验收清单

### 3.1 触发条件（任一不满足则禁止进入此阶段）

- [ ] 阶段 2 的所有测试已通过
- [ ] 用 `build-and-install-apk` skill 跑过 `devDebug` 构建
- [ ] 设备/模拟器上手动复现原崩溃路径，确认不再 crash
- [ ] git diff 已 review（无残留调试代码、无无关改动）

### 3.2 加载 doc-ops skill

```
Skill({ skill: "doc-ops" })
```

按 doc-ops 的路径规则与模板约束操作。

### 3.3 路径模板

```
/KeepassA/技术方案/崩溃修复/{YYYY-MM-DD}_{issueId末8位}_{短描述}/
  ├─ 验收清单.md       ← 主文档
  └─ 堆栈与样本.md     ← 原始证据（可选）
```

示例：`/KeepassA/技术方案/崩溃修复/2026-06-29_a1b2c3d4_IME空指针/验收清单.md`

### 3.4 写入清单

读取本目录的 `acceptance-checklist.md` 模板，替换占位符后用：

```
mcp__siyuan-sisyphus__fs({
  action: "write",
  path:   "/KeepassA/技术方案/崩溃修复/{...}/验收清单",
  markdown: <填充后的模板>
})
```

创建后**主动设置文档图标**（用户自定义规则要求）：

```
mcp__siyuan-sisyphus__document({
  action: "set_attr",
  id:     <上一步返回的文档 id>,
  attrs:  { icon: "1f6e0" }   # 🛠 工具图标；崩溃修复用 1f527 🔧 也可
})
```

### 3.5 错误处理

SiYuan 调用失败时遵守 `doc-ops` 的错误处理段：立即停止、提示用户、不静默回退本地文件、不重试超 2 次。

## 快速参考

| 阶段 | 主工具 / skill | 退出条件 |
|------|----------------|----------|
| 1 发现 | `crashlytics_get_report` (topIssues) | 用户选定 1 个 issue |
| 2 修复 | `tdd` + `crashlytics_batch_get_events` | 测试全绿 + devDebug 构建通过 |
| 3 验收 | `doc-ops` + `fs` write | SiYuan 文档创建成功并设图标 |

## 红旗 — 立即停下

- ⚠️ Crashlytics 工具未加载就直接拿堆栈猜修复 → 重启会话
- ⚠️ 测试还没红就开始改实现代码 → 删实现，回到 RED
- ⚠️ 修复跑通但 devDebug 没构建就写验收清单 → 先构建
- ⚠️ SiYuan 路径不在 `/KeepassA/技术方案/崩溃修复/` 下 → 路径错误，按 doc-ops 修正
- ⚠️ 多个崩溃变体一次性写完所有测试再改代码 → 违反纵向切片，回滚

## 常见错误

| 症状 | 原因 | 处理 |
|------|------|------|
| `crashlytics_get_report not found` | MCP 未加载 Crashlytics 工具集 | 检查 `--dir` 配置 + 重启 Claude Code |
| `7 PERMISSION_DENIED` | 未认证或项目错 | `firebase_login`，再 `firebase_update_environment({active_project: "keepassa-c7022"})` |
| 测试无法复现崩溃 | 崩溃依赖 Android 框架 | 改用 instrumented test / Robolectric，或先用 `diagnosing-bugs` |
| SiYuan 写入 404 | 工程笔记本未初始化 | 按 `doc-ops` 初始化三一级目录 |
