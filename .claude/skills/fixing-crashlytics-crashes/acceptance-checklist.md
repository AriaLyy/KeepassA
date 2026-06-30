# 验收清单 — {崩溃短描述}

> 本文档由 `fixing-crashlytics-crashes` skill 自动生成，禁止手工新建此类型文档。

## 一、Issue 元数据

| 字段 | 值 |
|------|-----|
| Crashlytics Issue ID | `{ISSUE_ID}` |
| 错误类型 | `{ERROR_TYPE}` (FATAL / NON_FATAL / ANR) |
| 首次出现 | `{FIRST_SEEN}` |
| 末次出现 | `{LAST_SEEN}` |
| 影响用户数 | `{IMPACTED_USERS}` |
| 事件总数 | `{EVENTS_COUNT}` |
| 涉及版本 | `{AFFECTED_VERSIONS}` |
| Top 设备 / OS | `{TOP_DEVICES_OS}` |
| 报告时间窗口 | `{WINDOW_START}` → `{WINDOW_END}` |

## 二、崩溃现象

### 堆栈顶帧

```
{STACK_TRACE_TOP}
```

### 完整堆栈

详见同目录 `堆栈与样本.md`（如未单独保存，则贴在下方）。

```
{FULL_STACK_TRACE}
```

### Crashlytics 自定义日志（崩溃前）

```
{CUSTOM_LOGS}
```

## 三、根因分析

- **触发条件**：{什么操作 / 什么状态下触发}
- **直接原因**：{NPE / ClassCast / 资源未释放 / 并发 / 框架回调时序 等}
- **深层原因**：{设计缺陷 / 边界未覆盖 / 缺少前置校验 等}
- **影响面**：{仅特定路径 / 全局 / 配置相关}

## 四、修复方案

### 4.1 复现测试（RED）

| 测试路径 | 复现的崩溃变体 |
|----------|----------------|
| `{TEST_FILE_PATH_1}` | `{VARIANT_DESC_1}` |
| `{TEST_FILE_PATH_2}` | `{VARIANT_DESC_2}` |

### 4.2 实现修改（GREEN）

| 改动文件 | 关键变更 | Commit |
|----------|----------|--------|
| `{CHANGED_FILE_1}` | `{CHANGE_DESC_1}` | `{COMMIT_HASH_1}` |
| `{CHANGED_FILE_2}` | `{CHANGE_DESC_2}` | `{COMMIT_HASH_2}` |

### 4.3 测试结果

- [ ] 单元测试（如有）：`./gradlew testDevDebugUnitTest` 全绿
- [ ] Instrumented 测试（如有）：`./gradlew connectedDevDebugAndroidTest` 全绿
- [ ] TDD 纵向切片：每个变体一轮 RED→GREEN（未批量写测试再批量改代码）

## 五、构建验证

- [ ] `./gradlew assembleDevDebug` 成功
- [ ] APK 已安装到设备 / 模拟器
- [ ] 手动复现原崩溃路径，**不再 crash**
- [ ] 原正常路径未回归

## 六、回归矩阵

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| {场景 1 — 复现路径} | ❌ 崩溃 | ✅ 正常 |
| {场景 2 — 相关路径} | ✅ 正常 | ✅ 正常 |
| {场景 3 — 边界} | — | ✅ 已覆盖 |

## 七、上线观察

- [ ] 修复版本号：`{VERSION_NAME} ({VERSION_CODE})`
- [ ] Crashlytics 监控窗口：发布后 7 天
- [ ] 验收阈值：该 issue 事件数下降 ≥ {THRESHOLD}%，且无新增相关 issue
- [ ] 责任人：{OWNER}
- [ ] 兜底回滚方案：{ROLLBACK_PLAN}

## 八、参考链接

- Crashlytics 控制台：https://console.firebase.google.com/u/0/project/keepassa-c7022/crashlytics/app/android:com.lyy.keepassa/issues/{ISSUE_ID}
- 修复 PR：{PR_URL}
- 关联 issue（如有）：{ISSUE_TRACKER_URL}
