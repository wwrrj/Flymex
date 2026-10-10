# Flymex

**Flymex** 是一个 Android（View + Kotlin）组件库，目标是 **1:1 复刻 Flyme（魅族）的系统组件**，定位类似 miuix 之于 MIUI。

所有颜色、尺寸、圆角、字阶、阴影、动效曲线，以及开关、勾选、滑杆、对话框、Toast、弹出菜单等组件的位图/矢量资源，均直接提取自 **Flyme 12.6.0.0A（魅族 21 Pro，Android 16）** 的系统包（`framework-res.apk`、`Settings.apk`、`SystemUI.apk`），资源名与 Flyme 保持一致（`fd_sys_color_*`、`mz_*`、`mc_*`、`Blue_*`），并保留了 Flyme 自己的主题属性体系（`?colorPrimary`、`?colorBrand`、`?colorStatePressed`、`?cornerMini` …）。

> 提取过程与每个令牌的来源见 [docs/TOKENS.md](docs/TOKENS.md)，组件与 Flyme 资源的对应关系见 [docs/COMPONENTS.md](docs/COMPONENTS.md)。

## 接入

```kotlin
// settings.gradle.kts
include(":flymex")
// app/build.gradle.kts
implementation(project(":flymex"))
```

在 `AndroidManifest.xml` 里使用 Flyme 主题：

```xml
<application android:theme="@style/Theme.Flymex">
```

`Theme.Flymex` 继承 `Theme.AppCompat.DayNight.NoActionBar`，深色模式自动切换到 Flyme 夜间配色（`values-night`）。

### 主题色

Flyme 的 8 种主题色都已内置：`Theme.Flymex.Blue`（默认）、`.Green`、`.Orange`、`.Purple`、`.Red`、`.Yellow`、`.Cyan`、`.Polestar`。

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    FlymeTheme.apply(this, FlymeAccent.GREEN)   // 在 super.onCreate() 之前
    super.onCreate(savedInstanceState)
}
```

也可以只给某个子树换色：`android:theme="@style/ThemeOverlay.Flymex.Purple"`，或 `FlymeTheme.withAccent(context, FlymeAccent.RED)`。

## 组件

| 组件 | 类 | Flyme 原型 |
|---|---|---|
| 开关 | `FlymeSwitch` | `com.meizu.common.widget.Switch`（40×22dp 轨道，14dp 圆点滑块，真机核对） |
| 按钮 | `FlymeButton` + `Widget.Flymex.Button.*` | `MzButton`：主要 / Tonal / 描边 / 文字 / Light / 48dp 胶囊 |
| 多选 / 单选 | `FlymeCheckBox` / `FlymeRadioButton` | `mz_btn_check_multiple` / `mz_btn_check_single`（Flyme 的单选是动画勾） |
| 滑杆 | `FlymeSeekBar`（`.Step` 样式） | `MzSeekBar` / `StepSeekBar`：22dp 轨道、白色圆点滑块、拖动数值气泡 |
| 加载环 / 环形进度 | `FlymeLoadingView` / `FlymeCircleProgressBar` | `LoadingView` / `CircleProgressBar` |
| 下载按钮 | `FlymeCircularProgressButton` | `CircularProgressButton`（56×26 胶囊 ↔ 26dp 进度环） |
| 列表单元 / 分组卡片 | `FlymeCell` / `FlymeCellGroup` | 沉浸式列表 `mz_preference_immersive.xml` + `ImmersiveListItemDrawable`（10dp 圆角卡片、无分隔线） |
| 圆角卡片 | `FlymeCardView` | `SmoothCornerView` + `Widget.Flyme.Shadow.*` |
| 对话框 | `FlymeDialog.Builder` | `FlymeAlertDialogLayout`（340dp，16dp 圆角，48dp 圆角按钮，Flyme 进出场曲线） |
| 底部操作面板 | `FlymeActionSheet.Builder` | `Flyme.AlertDialog…ShowAtBottom`（304ms 滑入） |
| Toast | `FlymeToast.show()` / `.complete()` | `toast_frame` / `mc_toast_layout` |
| 弹出菜单 | `FlymePopupMenu` | `Widget.Flyme.Light.ListPopupWindow`（`mz_popup_bg_light`，`mz_overflow_popup_enter`） |
| 标签栏 | `FlymeTabBar`（下划线 / 胶囊） | `TabScrollView`，`mz_action_bar_tab_indicator_*` |
| 标题栏 | `FlymeAppBar` | Flyme ActionBar（64dp / 展开 128dp，大标题 32sp） |
| 搜索栏 | `FlymeSearchBar` | `mz_titlebar_search_view_edittext_bg` |
| 空状态 | `FlymeEmptyView` | `mc_empty_view.xml` |
| 页码指示器 | `FlymePageIndicator` | `PagerIndicator` |
| 角标 / 红点 | `FlymeBadge` | `TipDrawable` / `mz_tab_view_dot_color` |

文字外观：`TextAppearance.Flymex.Headline/Body/Label.{Large,Medium,Small,Mini}`（对应 `fd_sys_typescale_*`），以及旧版 `H6…H12/B6/B7`。

### 示例

```xml
<com.flymex.widget.FlymeCellGroup
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:mzGroupTitle="通用">

    <com.flymex.widget.FlymeCell
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:title="深色模式"
        android:summary="跟随系统"
        app:mzWidget="switch" />

    <com.flymex.widget.FlymeCell
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:title="更多设置"
        app:mzWidget="arrow" />
</com.flymex.widget.FlymeCellGroup>
```

```kotlin
FlymeDialog.Builder(this)
    .setTitle("清除应用数据？")
    .setMessage("此操作不可撤销。")
    .setNegativeButton("取消")
    .setPositiveButton("清除") { /* … */ }
    .setDestructive(true)
    .show()

FlymeToast.show(this, "已保存")
```

`sample/` 模块是一个完整的演示 App（主题色切换、夜间模式、全部组件）。

## 仓库结构

```
flymex/      Android library（com.flymex）
sample/      演示 App
tools/       资源提取管线：apktool 解包 → extract-tokens.py → gen-android-res.py / copy-res.py
docs/        令牌溯源表、组件对照
```

重新生成资源（需要已解包的 Flyme 镜像）：

```bash
python -I tools/extract-tokens.py work/flyme-tokens.json settings=work/settings/res systemui=work/systemui/res framework-res=work/framework-res/res
python -I tools/gen-android-res.py work/flyme-tokens.json flymex/src/main/res
python -I tools/copy-res.py flymex/src/main/res "work/settings/res;work/framework-res/res;work/systemui/res" @tools/flyme-res-roots.txt
```

## 构建

需要 JDK 17 与 Android SDK（compileSdk 36，minSdk 26）：

```bash
./gradlew :sample:assembleDebug
```

## 说明

- 资源来自魅族 Flyme 系统包，仅供学习与适配参考；Flyme Sans / Flyme Number 字体未随库分发，文字外观使用 `sans-serif` / `sans-serif-medium`（在 Flyme 设备上会自动落到系统字体）。
- 没有在 Flyme 资源中找到精确定义的少数值（如大标题模式下的标题底边距、`label_small` 字号）在代码注释中标明为推导值。
