# RikkaHub 手表端（Wear M3）移植说明

本文档说明手表端变体的构建方式、UI 结构、方/圆形态判定、v7a native 来源与验收步骤。

## 变体与产物

`app` 模块新增 `target` flavor 维度：`phone` / `wear`。手机变体保持原有行为不变。

```bash
# 手机（原命令仍是别名，指向 phone 变体）
./gradlew assembleGithubRelease        # → app/build/outputs/apk/github/release/
# 手表：默认 armeabi-v7a + arm64-v8a（按 abi split，另附 universal）
./gradlew assembleGithubWearRelease    # → app/build/outputs/apk/githubWear/release/
./gradlew assembleWearRelease          # 同上
# 模拟器调试（临时把 x86_64 加进 wear 变体，release 不会包含）
./gradlew assembleGithubWearDebug -PwearEmulatorAbi=true
```

- wear 变体的 `ndk.abiFilters` 在 `app/build.gradle.kts` 的 `wear` flavor 内：`armeabi-v7a` + `arm64-v8a`。
- 手机变体同时也补上了 `armeabi-v7a`：`splits.abi` 是全局配置，不分 ABI 会生成一个没有 native 的 v7a 手机包；现在 quickjs / sqlite / libsimple / PRoot 全部有 v7a 产物，v7a 手机包是完整的，不会出现“能装但 JNI 必崩”的产物。
- ABI split 由全局 `splits.abi` 生成；`splits.abi.include` 覆盖所有变体，所以本地构建 wear 时也会生成一个无 native 的 x86_64 split。发布脚本只取 v7a / arm64 / universal，不发布 x86_64。
- 手表 UI 代码位于 `app/src/wear/java/me/rerere/rikkahub/wear/`（flavor source set），设计系统在独立模块 `:wear`，只有 wear 变体依赖它，因此手机 APK 不包含 Wear Compose。
- 手表清单覆盖 `app/src/wear/AndroidManifest.xml`：移除手机入口 Activity（`RouteActivity`、`ShortcutHandlerActivity`、`SafeModeActivity`）与 Phone Use 的 `PACKAGE_USAGE_STATS` 权限，仅保留 `WearRouteActivity` 作为 launcher；不依赖任何 `androidx.wear.*` 的服务/Provider 类。

## 方 / 圆形态判定

`wear` 模块 `me.rerere.rikkahub.wear.shape.ScreenShapeController`：

1. 手动三态（自动 / 强制方 / 强制圆）优先，写入 `rikkahub.preferences` 的 `watch_screen_shape_mode`，立即生效并持久化。
2. 自动模式优先读系统 `Configuration.isScreenRound`（WearOS 与多数 AOSP 手表会置位）。
3. 非 WearOS 取不到标志时启发式：`FEATURE_WATCH` 且宽高比 ≈1 且短边 ≤240dp 判为圆屏，否则方屏。
4. 判定结果（含 `SYSTEM` / `HEURISTIC` / `FORCED` 来源、宽高dp、是否手表）放进全局 `StateFlow`，经 `LocalScreenShape*` 下发；Activity 在 `onConfigurationChanged` / `onStart` 刷新。

布局分支在组件库内部完成，页面代码只有一套：

- 圆屏：`WearScreen` 使用 Wear M3 `ScreenScaffold` + `ScalingLazyColumn`，自动带 TimeText 与 ScrollIndicator（M3 的 PositionIndicator 等价物）。
- 方屏：同一个 `WearScreen` 使用紧凑的顶部标题栏 + `LazyColumn` 与 ScrollIndicator。

设置页 `Settings → Display → Screen shape` 内直接提供三态开关与判定诊断。

## 手表 UI 页面

导航在 `wear/WearApp.kt`（Navigation 3，单 Activity）。已迁移页面：

- 主页（会话列表 / 新建 / 收藏 / 设置入口）
- 聊天（流式、Markdown/代码高亮/LaTeX/Mermaid 复用 `MarkdownBlock`、消息分支、重生成/停止、复制/朗读/编辑/删除、Fork、语音输入、快捷消息）
- 会话历史、设置中心、Quick start、Provider 列表与详情（API key / Base URL / 启停 / 手动加模型）、模型选择、助手切换
- 助手详情（提示词、模型、记忆、MCP、本地工具、注入）、Extensions（提示词注入 / 快捷消息 / 技能）
- Search / MCP / Speech(TTS/ASR) / Media / Web server / Backup（二维码导出与手动导入）
- Workspace（创建/删除、rootfs 安装与进度、兼容模式、PRoot 控制台）、Translator、图像生成、媒体创作、WebView、统计、日志、Debug、Favorites、关于

### 各页覆盖与取舍

手表屏幕小，页面都做成「全宽单行 + 弹窗编辑」；数据仍走手机端同一套模型与持久化。

- **Search**：Provider 列表/启用/删除/新增，API key、endpoint、Tavily/BoCha/LinkUp/SearXNG/Jina/Grok/Perplexity 等常用字段，结果数量；自定义 JS 脚本编辑留给手机。
- **MCP**：SSE / Streamable HTTP 新增、URL、headers(JSON)、启停、删除；助手级 MCP 勾选在助手详情里。
- **Speech**：TTS/ASR 列表与启用、名称/API key/WebSocket 地址、按类型新增；音色/模型等长表单留给手机。
- **Media**：媒体生成 Provider 增删改（含阿里云 workspaceId）、图像模型选择；每家的进阶参数留给手机。
- **Web server**：状态/URL、启停（同一前台 Service）、端口、访问密码、JWT、仅本机；权限申请与手机一致。
- **Backup**：本地/Chatbox/Cherry/DeepSeek 导入导出、WebDAV/S3 配置+测试+立即备份、二维码导出（设置子集）与手动粘贴导入；远程文件恢复留给手机。
- **Assistant**：名称、聊天模型（含全局默认）、系统提示词（含流式开关/模板/会话级提示）、记忆（开关/整理/文件增删改）、MCP 勾选、本地工具开关、注入（mode/lorebook/quick message/skills）、快捷消息与技能管理；正则/预设消息/头像/采样参数留给手机。
- **Translator**：目标语言 + 翻译 + 复制（源语言由模型自动识别，手机 API 也没有源语言参数）。
- **ImageGen**：prompt/数量/尺寸、生成与历史；模型与参考图选择留给手机。
- **Media creation**：会话列表/新建、prompt、草稿附件展示与移除、生成、时间线资产；从相册添加附件留给手机。
- **Favorites/Stats/Logs/Debug/WebView**：列表跳转、统计数字、日志过滤/复制/清空、数据库与索引诊断、内置 WebView。
- **Phone Use**：手表端彻底移除（无障碍服务与入口都不打包）。

## v7a native 来源

## v7a native 来源

| 库 | 来源 | 说明 |
|---|---|---|
| `libquickjs.so` | Maven `quickjs-kt-android` AAR | AAR 自带 armeabi-v7a |
| `libsqlite3x.so` | JitPack `com.github.rikkahub:sqlite-android` AAR | AAR 自带 armeabi-v7a |
| `libtermux.so`、`libworkspace.so` | 仓库内 C/C++ 源码（`:terminal`、`:workspace` CMake） | 交叉编译 |
| `libsimple.so` | 上游 [wangfenjin/simple](https://github.com/wangfenjin/simple) v0.7.1 源码 | 官方 Release 不含 v7a，用 `tools/native/build-libsimple.sh` 从源码编（jieba 关闭，与 arm64 产物一致） |
| `libproot_exec.so` / `libproot_loader.so` | Termux 官方 `proot_5.1.107.102_arm.deb` | 仓库内只有 arm64/x86_64 自包含产物；v7a 用官方 arm 包 |
| `libtalloc.so` | Termux 官方 `libtalloc_2.5.0_arm.deb` | arm proot 的动态依赖（SONAME `libtalloc.so.2`） |
| `libandroid_shmem.so` | [termux/libandroid-shmem](https://github.com/termux/libandroid-shmem) 源码 | arm proot 的另一动态依赖，源码编译并带 SONAME |

复现：

```bash
bash tools/native/build-libsimple.sh    # 需要 NDK + SDK CMake 3.22.1 + python
bash tools/native/fetch-proot-v7a.sh    # 需要 NDK + python + curl
```

运行时依赖解析：Android 只把 `lib*.so` 解压进 `nativeLibraryDir`，而 arm proot 的 `DT_NEEDED` 是
`libtalloc.so.2` / `libandroid-shmem.so`。`ProotShellRunner` 会：

1. 在 `filesDir/.proot-libs/` 复制出带 soname 的文件名；
2. 给 proot 子进程设置 `LD_LIBRARY_PATH` 与 `LD_PRELOAD`（两个库都带正确 SONAME，预加载后直接命中）。

arm64 的 PRoot 是自包含的，这两个库不存在时这段逻辑自动跳过。

### SQLite 扩展优雅回退

- `SQLiteConfiguration`：`libsimple.so` 不存在时不注册扩展，数据库照常打开。
- `MessageFtsManager`：探测 `simple_query()` 是否可用；不可用时 FTS 索引改用内置 `unicode61` 分词、查询用普通前缀匹配、snippet 用内置 `snippet()`；分词器变化时重建索引（搜索页有重建入口）。因此缺少 v7a 分词扩展也不会崩溃，只是搜索改走内置分词。

## PRoot / workspace

rootfs 不打包进 APK：手表安装页默认按当前进程 ABI 选择 **Alpine minirootfs**（armv7 / aarch64 / x86 / x86_64，约 3-4MB，来自 Alpine 官方 CDN），运行时下载解压到工作区目录；也可以填任意 tar/tar.gz/tar.xz 地址。Alpine 没有 bash，`WorkspaceManager.rootfsShell` 已回退到 `/bin/sh`，`RootfsPatcher` 也处理了 busybox 软链；需要 bash 时可在 Rootfs 内 `apk add bash`。手机端页面的默认镜像仍是 Ubuntu base（约 27MB）。离线或下载失败不影响其余功能，只是 workspace 不可用。

## 验收步骤

### 模拟器（Wear OS 圆屏）

```bash
# 用 SDK 里的 Wear 系统镜像手工建 AVD（没有 cmdline-tools 时）：
# ~/.android/avd/rikkahub_watch.avd/config.ini 中 image.sysdir.1 指向 android-37.0/android-wear-signed/x86_64
emulator -avd rikkahub_watch -no-window -gpu swiftshader_indirect
./gradlew :app:assembleGithubWearDebug -PwearEmulatorAbi=true
adb install -r app/build/outputs/apk/githubWear/debug/*x86_64*.apk
adb shell am start -n me.rerere.rikkahub.debug/me.rerere.rikkahub.wear.WearRouteActivity
```

### 真机（v7a、圆屏）

已用 Xiaomi Watch 2 Pro（armeabi-v7a 用户空间 / Android 14 / 466x466 圆屏）验证：

- 安装 v7a debug APK 可启动，`libsqlite3x.so` 从 `/lib/arm/` 加载，无 `UnsatisfiedLinkError`；
- 形态判定为 `round · system`；
- 手动强制方屏立即生效，重启后保持；
- 本地 mock 流式对话（见下）可完整渲染流式回复。

### 流式对话（不依赖真实 API Key）

```bash
python tools/wear-automation/mock-openai-server.py &          # 监听 :8099
adb reverse tcp:8099 tcp:8099                 # 手表访问 127.0.0.1:8099
# 手表上：Settings → Providers → OpenAI → Base URL=http://127.0.0.1:8099/v1、API key 任意、Add model=mock-gpt
# Settings → Model 选 mock-gpt，然后 New chat 发消息
```

### PRoot（可选特性）

```bash
# 离线测试：本地提供 Alpine minirootfs（约 3.5MB）
curl -LO https://dl-cdn.alpinelinux.org/alpine/v3.22/releases/armv7/alpine-minirootfs-3.22.8-armv7.tar.gz
mkdir -p build/rootfs && mv alpine-minirootfs-*.tar.gz build/rootfs/
python -m http.server 8098 --directory build/rootfs
adb reverse tcp:8098 tcp:8098
# 手表：Settings → Workspace → New workspace → Install rootfs（URL 改成 http://127.0.0.1:8098/alpine-minirootfs-3.22.8-armv7.tar.gz；直接点 OK 则用官方 CDN 的默认地址）
# 安装完成后 Open console，执行 uname -a / cat /etc/os-release
```

## 调试与自动化（仅 debug 构建）

手表上 adb `input` 与 IME 组合很不稳定（输入法全屏、光标位置、合成文本），因此加了一个 **debug-only** 的无障碍桥：

- 代码：`app/src/debug/java/me/rerere/rikkahub/debug/WearAutomationService.kt`，清单/权限在 `app/src/debug/`，release 变体不会打包。
- 能力：`GET /tree`（读屏：文字/描述/bounds/可点击/可编辑/可滚动）、`POST /click?text=`、`POST /settext?value=`（直接 ACTION_SET_TEXT，绕开 IME）、`POST /scroll?dir=`、`POST /back`、`POST /tap?x=&y=`。
- 启用：

```bash
adb shell settings put secure enabled_accessibility_services \
  me.rerere.rikkahub.debug/me.rerere.rikkahub.debug.WearAutomationService
adb shell settings put secure accessibility_enabled 1
adb forward tcp:8096 tcp:8096
curl http://127.0.0.1:8096/ping
```

- 宿主脚本：`tools/wear-automation/wear-ui.sh`（tree/click/settext/scroll/back/start/screenshot）与 `tools/wear-automation/wear-nav.sh`（滚动查找并打开页面），仅用于本地验收。

已用它在 Xiaomi Watch 2 Pro 上完成：设置页全量冒烟（Search/MCP/Speech/Media/Web server/Backup/Appearance/Extensions/Statistics/Logs/Debug/Translator/ImageGen/MediaCreation/WebView/About/Assistants 及其子页）、方/圆强制切换、Alpine rootfs 安装与 PRoot 命令执行、mock 流式对话。

## 验证记录（2026-11-21）

真机：Xiaomi Watch 2 Pro（armeabi-v7a 用户空间、Android 14/API 34、466x466 圆屏、WearOS）
模拟器：Wear OS 7.0 x86_64（454x454，手动 AVD，未配置圆屏 skin 标志）

已确认：

- **v7a release 包**安装并启动（`me.rerere.rikkahub`，minSdk 26 / target 37，launcher 仅 `WearRouteActivity`）；logcat 中 `libsqlite3x.so` 从 `.../lib/arm/` 加载，无 `UnsatisfiedLinkError`。
- **v7a libsimple** 生效：debug 与 release 都打印 `MessageFtsManager: message_fts tokenizer=simple (simple extension=true)`。
- **PRoot + Alpine**：手表上从 `adb reverse` 提供的 `alpine-minirootfs-3.22.8-armv7.tar.gz` 安装 rootfs，进度→ready；控制台执行 `uname -a && cat /etc/os-release` 返回 `Linux localhost 5.15.185-android14-11 ... armv8l Linux` / `NAME="Alpine Linux" ID=alpine`、`[exit 0]`。PRoot 版本 5.1.107.102，`process_vm`/`seccomp_filter` 均可用。
- **流式对话**：`tools/wear-automation/mock-openai-server.py` + `adb reverse`，完整流式回复渲染；自动滚动停在回复底部。
- **消息分支**：对助手消息 Regenerate 后出现 `Previous branch` / `Next branch` 控件。
- **形态**：真机 `Detected: round · system`；`Force square` 立即切到方屏布局（顶部栏 + 紧凑列表），`Auto` 恢复；模拟器上（系统无 ROUND 标志）走启发式 `round · heuristic`，强制方屏后重启仍保持。
- **设置页冒烟**（无障碍桥导航）：Search、MCP、Speech、Media、Web server、Backup、Appearance、Extensions、Statistics、Logs、Debug、Translator、Image generation、Media creation、Web browser、About、Assistants 及助手详情/提示词/记忆/MCP 子页均能渲染，无崩溃。
- **修复的运行时问题**：`About` 路由漏注册导致 `Unknown screen About` 崩溃；列表重复 key（两条同名会话）导致 Compose 崩溃 → 现在每个 slot 的 key 是「显式 key + 自增序号」。

- **构建链**：`spotlessCheck` 通过；Play AAB（`bundlePlayPhoneRelease`）构建通过（`app/build/outputs/bundle/playPhoneRelease/app-play-phone-release.aab`，约 38MB）。
- 最终 release 包重新安装到真机复验：圆屏启动、`message_fts tokenizer=simple (simple extension=true)`、无 FATAL / UnsatisfiedLinkError。

最终 APK（`app/build/outputs/apk/githubWear/release/`）：

| 文件 | SHA-256 |
|---|---|
| `app-github-wear-armeabi-v7a-release.apk` | `37be9e04d51f83459ebd32ce861bbe822af18697181a2775812b247bb25cc966` |
| `app-github-wear-arm64-v8a-release.apk` | `72e29c3c311b8b1b8fa4a208a5fadf5235bff98e34a291ba2e7f0783d08abd87` |


- **主题默认值**：手表变体首次启动写入 `colorMode=DARK` + `amoledDark=true`，深色模式不再被系统「未开启深色」误导；之后完全尊重用户选择（外观设置里可改回跟随系统/浅色）。
- **圆屏复测（2026-11-21 夜，模拟器 454×454）**：修复聊天页标题栏与底部按钮被圆形裁切的问题（圆屏标题栏内缩、语音/重生成移入标题栏与菜单、底部只保留居中的 Reply/Stop）；所有页面返回按钮由整行大按钮改为圆形小图标；`infoItem` 长值改为 40/60 分栏并右对齐换行，避免 "Device" 被挤成竖排单字。


- **中文界面**：手表页面已接入 830 条 `wear_*` 字符串（`app/src/wear/res/values/strings_wear_pages.xml` + `values-zh-rCN` + `values-zh`），共享组件（返回/取消/关闭/重试/错误）另有 `wear/src/main/res/values*` 中文；模拟器切换 `zh-CN` 后主页/聊天/设置/关于/形态设置均已中文显示。保留英文的仅为技术内容（品牌名、URL、ABI、形状检测枚举如 round/heuristic、剪贴板标签等）。

- **手机回归**：`assembleGithubPhoneDebug` / `assembleGithubPhoneRelease` 通过，launcher 仍是 `RouteActivity`，手机 UI（`ChatPage` 等）保留；手机变体同时支持 armeabi-v7a（所有 native 依赖都有 v7a 产物）。

发布 APK 由 CI 用仓库配置的 release keystore 签名；本地验证包使用生成的测试 keystore（`app/app.key`，gitignored）。

> 说明：R8 默认保留 `Activity`/`Service` 子类，因此 wear release 的 dex 里仍能看到 `RouteActivity`、`PhoneUseAccessibilityService` 等**类壳**；它们的清单节点、入口和 Compose 页面已全部移除（`ChatPage` 等手机 UI 类在 release dex 中不存在），手表端无法进入手机 UI。若要求连类壳也彻底消失，需要把手机 UI/Phone Use 源码迁到 `src/phone`，属于后续可选重构。

## 已知限制


- 手机 UI 源码仍在 `main` source set 中，wear 变体不注册其入口、Release 经 R8/资源收缩剥离；如需源码级彻底移除，下一步可把手机 UI 迁到 `src/phone`。
- 本地 wear 构建会额外生成一个无 native 的 x86_64 split（`splits.abi` 是全局配置）；发布脚本只选择 v7a/arm64/universal。
- 手表端控制台是「命令 + 输出」形式（圆表上全屏终端可读性差），PRoot 能力等价；需要完整 TUI 时可后续把 `TerminalView` 嵌进方屏布局。
- Wear 端未接入手表的账号登录（ChatGPT/Grok OAuth 需要浏览器），请先在手机端登录后导入配置。
