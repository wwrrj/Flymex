# Flymex 组件 ↔ Flyme 资源对照

每个组件的尺寸、颜色和动效都能在 Flyme 12.6 的解包资源中找到出处。下表列出主要依据；
代码里每个类的 KDoc 也写明了来源。

## FlymeSwitch — `com.meizu.common.widget.Switch`

在魅族 21 Pro（Flyme 12.6，520dpi）的设置页上逐像素核对过。

| 项 | Flyme 资源 | 值 |
|---|---|---|
| 轨道形状 | `drawable/mc_switch_anim_track.xml` | 高 22dp，圆角 11dp |
| 最小宽度 | `dimen/mcSwitchMinWidth` | 40dp |
| 滑块 | `mz_switch_thumb_to_width/height` | 14dp 白色圆（开/关都是圆点） |
| 滑块内缩 | `mz_switch_thumb_padding_left/right` | 4dp |
| 轨道色（开） | `color/mz_switch_track_on_color_state` | `?colorBrand`，禁用 25% |
| 轨道色（关） | `color/mz_switch_track_off_color_state` | `?colorSurfaceContainerHigh`（白底上为 #d2d2d2），禁用 25% |
| 滑块色 | `color/mz_switch_thumb_on/off_color` | `?colorOnBrand` |
| 深色面板变体 | `mz_switch_track_dark_*`, `mz_switch_thumb_dark_*` | 开 #1966c9，关 #404040 |
| 动效 | `interpolator/mc_btn_checked_single_interpolator` | cubic-bezier(0.2, 0.3, 0.21, 0.97)，200ms |

`mz_switch_thumb_from_*`（9×4dp）在真机上并不作为关闭态滑块出现，库里不再使用。

## FlymeButton — `MzButton`

| 变体 | 背景 drawable | 文字色 |
|---|---|---|
| 主要（Widget.Flymex.Button） | `mz_btn_corner_default`（`?colorPrimary`，圆角 `smooth_large` 20dp；禁用 `surface_container_low`） | `mz_button_corner_text_color_default`（`?colorOnPrimary`） |
| Tonal | `mz_btn_corner_light_bg_state_drawable`（`?colorPrimaryContainer`，圆角 `smooth_regular` 12dp） | `mz_button_corner_text_color_light`（`?colorPrimary`） |
| 描边 | `mz_btn_corner_normal_stroke`（1dp `?colorPrimary`） | 同上 |
| 文字 | `mz_item_background_borderless`（ripple #08000000） | `mz_button_positive_text_default`（`?colorBrand`） |
| Light | `mz_btn_corner_bg_gray_smooth_regular`（`?colorSurfaceContainerLow`，按下 `?colorPrimaryContainer`） | `?colorOnSurface` |
| 大按钮 48dp | `mz_btn_corner_default_state_color`（圆角 `mz_cir_btn_radius_normal` 50dp） | — |

字号 `mz_corner_button_text_size` 17sp，`sans-serif-medium`，水平内边距 10dp（`Widget.DeviceDefault.Light.Button.Color`）。

## FlymeCheckBox / FlymeRadioButton

- 多选：`Widget.Flyme.Light.CompoundButton.CheckBox` → `drawable/mz_btn_check_multiple`
- 单选：`listChoiceIndicatorSingle` → `drawable/mz_btn_check_single` → `mz_vec_btn_checked_*`（24dp，viewport 72，描边 6.9，`?colorPrimary` 着色；`animator/mc_anim_btn_checked_fade_in` 200ms 描画动画；禁用 alpha 0.25）

## FlymeSeekBar — `Widget.Flyme.Light.SeekBar` / `MzSeekBar`

真机使用 `com.meizu.common.widget.MzSeekBar`，在"显示和亮度"页的亮度滑杆上逐像素核对（520dpi）。
外观完全由三个 drawable 决定，FlymeSeekBar 用代码按同样的层次绘制：

| 项 | 来源 | 值 |
|---|---|---|
| 轨道 | `mz_seekbar_background_enable` 第一层（作为 background 绘制，占满整个 View 宽度） | 22dp 高（`mz_seekbar_height`），圆角 `mz_seekbar_radius` 15dp > 半高 ⇒ 两端为完整半圆（药丸形）；颜色 `mz_seekbar_background_state_color` = `?colorSurfaceContainerLow` |
| 左端帽 | 同一层叠列表第二层 | 12dp 宽（禁用 11dp）、左侧两角药丸圆角的 `?colorBrand` 色块，垫在填充下，保证填充起点永远是圆的 |
| 填充 | `mz_seekbar_rectangle_progress` | 方角 `?colorBrand` 矩形，从滑块零位（11dp，`mz_seekbar_background_padding_start`）画到滑块圆心；方角被端帽和滑块盖住 |
| 滑块 | `mz_seekbar_thumb` | 22dp `?colorBrand` 圆盘 + 14dp `?colorOnPrimary` 圆点；禁用时只画右半圆盘（避免半透明叠加） |
| 禁用 | `mz_seekbar_thumb_state_color` | 品牌色部分降为 20% 透明度，轨道颜色不变 |
| 内边距 | `Widget.Flyme.Light.SeekBar` | 左右 11dp、上下 4dp ⇒ 滑块圆心与触摸映射都在 11dp ～ 宽度−11dp 之间移动 |
| 按压放大 | `MzSeekBar.mAnimHelper` → `SeekBarAnimHelper.handleTouchScale` | ACTION_DOWN 时整个 View 以 `SpringAnimation` 弹到 scaleX/scaleY **1.05**，UP/CANCEL 弹回 1.0；`SpringForce` stiffness **250**、dampingRatio **0.89**，默认中心为缩放锚点。`touchScaleEnabled` / `touchScale` 可关可调 |
| 数值气泡 | `mcLargeCircleRadis` 13dp，`mcTextNumberSize` 14sp | 拖动时悬浮在轨道上方 2dp（`mcDistanceToCircle`） |
| 分段 | `StepSeekBar`，`mc_enhance_seekbar_spot_radius` | 3dp 圆点标记每一档 |
| 标签行 | `layout/flyme_seek_bar_preference.xml` | 13sp medium `?colorOnSurfaceVariant`，18dp 图标，距滑杆 10dp |

## FlymeLoadingView / FlymeCircleProgressBar

- `Widget.MeizuCommon.LoadingView`：`mcLoadingRadius` 9.5dp，`mcRingWidth` 3.5dp，`mc_loading_view_background_light` #19000000，文字 `mc_loading_view_text_light` #66000000 14sp
- `MeizuCommon.CircleProgressBar`：`mz_circle_progressbar_layout_width` 33dp，`mz_circle_progressbar_width` 1.75dp，`mz_circle_progressbar_rim_color_blue` #26000000

## FlymeCircularProgressButton

`mc_cir_progress_button_min_width/min_height` 56×26dp，`corner_radius` 48dp，`stroke_width` 3dp，`text_size` 12sp，`mc_cir_progress_button_normal_text_color` 白；指示器 `?colorPrimary` / `?colorSurfaceContainerHigh`。

## FlymeCell / FlymeCellGroup — `mz_preference.xml`

真机（Flyme 12 设置）使用沉浸式列表 `layout/mz_preference_immersive.xml` + `com.meizu.common.animator.ImmersiveListItemDrawable`，在魅族 21 Pro 上实测：

| 项 | 资源 | 值 |
|---|---|---|
| 卡片 | `ImmersiveListItemDrawable`：`immersiveItemPadding`、`radius` | 距屏幕 16dp，圆角 10dp（`fd_sys_radius_corner_smooth_small`），`?colorSurfaceContainerLowest` |
| 行内边距 | `fd_sys_margin_extra_heavy` 32dp（含卡片内缩 16dp） | 文字距卡片边 16dp |
| 垂直内边距 | `mz_list_item_content_vertical_padding_level4` | 16dp；首行上 / 末行下再加 8dp（`immersive_mz_preference_head/foot`） |
| 图标 | `mz_list_icon_small/normal/medium/large_width` | 32 / 40 / 46 / 60dp，圆角 `mz_list_icon_item_icon_corner_radius` 7dp，间距 16dp |
| 标题 | `TextAppearance.Flyme.SingleList.Title` | 17sp medium，`mz_title_text_light` |
| 摘要 | `TextAppearance.Flyme.Body.Mini` | 14sp，`?colorOnSurfaceVariant`，行距 +4dp |
| 右侧控件区 | `preference_item_padding_inner`，`preference_widget_width` | 距文字 14dp，最小宽 43dp |
| 右箭头 | `mz_arrow_next_right` | 24dp |
| 分隔线 | 无（沉浸式）；`mzShowDivider="true"` 启用旧版 `mz_list_item_divider_height` 0.67dp | |
| 分组标题 | `mz_preference_category_immersive.xml` | 13sp medium `?colorOnSurfaceVariant`，上 18dp 下 8dp，距卡片边 16dp |
| 组间距 | `immersive_preference_divider` | 16dp |
| 按压 | `ImmersiveListItemDrawable` tint `?colorStatePressed` | |

## FlymeCardView — `SmoothCornerView` + `Widget.Flyme.Shadow.*`

默认 `?cornerLarge` 20dp、`?colorSurfaceContainerLowest`；阴影 Medium = `fd_sys_shadow_elevation_medium` 40dp + `fd_sys_color_shadow_spot_medium` #78000000；Large = 60dp + #c8000000（`outlineSpotShadowColor` / `outlineAmbientShadowColor`）。

## FlymeDialog — `FlymeAlertDialogLayout` / `mz_alert_dialog_appcompat.xml`

| 项 | 资源 | 值 |
|---|---|---|
| 宽度 | `mz_alert_dialog_width` | 340dp |
| 背景 | `mz_dialog_background_drawable` | 16dp 圆角，`?colorSurfaceContainerLowest` |
| 标题 | `DialogWindowTitle.Flyme.Light` | 19sp medium，`?colorOnSurface`，行距 +6dp |
| 内容 | `TextAppearance.Flyme.Light.Content` | 15sp，`?colorOnSurfaceVariant`，行距 +5dp |
| 内边距 | `dialogPreferredPadding` = `mz_alert_dialog_normal_margin_start` | 24dp |
| 按钮 | `mz_alert_dialog_button_bg_radius`（8dp 圆角，`mz_alert_dialog_button1_bg_color` + 按压 `?colorStatePressed`） | 横排 48dp（`mz_alert_dialog_horizontal_button_height`），竖排 58dp，间距 12dp（`mz_alert_dialog_button_margin_start`），上距 28dp |
| 按钮文字 | `mz_alert_dialog_button_bar.xml` | 17sp medium；确定 `mz_button_positive_text_default`（`?colorBrand`），删除 `mz_alert_dialog_delete_button_color` #ff0000 |
| 列表项 | `mz_select_dialog_singlechoice.xml` | 48dp，左右 21dp，`mz_alert_dialog_listview_item_bg_radius`（6dp，选中 `?colorOnSelected`） |
| 进出场 | `Animation.Flyme.Dialog.Alert` | 进 250ms cubic-bezier(.25,.24,.19,.99) 自下 100% 平移；出 200ms (.33,0,.67,1) 至 150% |
| 底部变体 | `Animation.Flyme.Dialog.ShowAtBottom` | `bottom_dialog_anim_duration` 304ms，(.2,.5,.05,1) / (.29,.5,.16,1)，底边距 `mz_alert_dialog_margin_bottom_to_screen` 18dp |

## FlymeToast

- 文本 Toast：`framework-res drawable/toast_frame`（白，8dp 圆角，0.5dp #0d000000 描边）/ 夜间 `mz_toast_frame_dark`（#101010）；SystemUI `text_toast.xml`：最小 128×39dp，内边距 18×12dp，最大宽 284dp；`TextAppearance.Toast` 14sp medium；`mz_toast_y_offset` 30dp；`toast_elevation` 60dp，`outlineSpotShadowColor` #c8000000
- 完成 Toast：`mc_toast_layout.xml`：`Widget.Flyme.Shadow.Large`，最小宽 144dp（`mz_complete_toast_width`），内边距 24/16dp，图标 30dp `mz_btn_check_button_square_on_toast`，文字 14sp medium
- 动效：`mz_snackbar_enter_anim`（缩放 0.8→1 200ms (.18,.7,.05,1) + 透明 150ms (.65,0,.35,1)）

## FlymePopupMenu — `Widget.Flyme.Light.ListPopupWindow`

`mz_popup_bg_light`（`?colorSurfaceContainerLowest`，`smooth_medium` 16dp，0.5dp `?colorOutlineVariant` 描边）；行高 `mz_popup_menu_item_height` 48dp，内边距 `mz_popup_list_item_content_padding_left` 21dp，宽度 `mz_popup_menu_item_min_width` 232dp ~ `mz_popup_menu_width_max` 280dp；动画 `mz_overflow_popup_enter`（alpha 100ms + scale 0.965→1 150ms，枢轴 80%/0）。

## FlymeTabBar — `TabScrollView`

- 下划线：`mz_tab_height` 38dp；文字 `TextAppearance.Flyme.H8.Medium` 19sp，`mz_title_bar_tab_text_state_color`（选中 `?colorPrimary`，未选 `?colorOnSurface`，禁用 20%）；指示器 `mz_action_bar_tab_indicator_width/height` 14×3dp，`mz_action_bar_tab_indicator_padding_bottom` 7dp；tab 内边距 `mz_tool_bar_tab_view_padding_left_small` 10dp，最小宽 `mz_action_bar_tab_view_mini_width` 46dp
- 胶囊：`mz_action_bar_tab_capsule_style_height` 55dp，上下 `_top/_bottom` 8dp，`mz_action_bar_tab_indicator_color_blue`（`?colorPrimary`，圆角 20dp）
- 红点：`mz_tool_bar_tab_view_dot_width_height` 6dp，`mz_tab_view_dot_color` #ef2828

## FlymeAppBar

`mz_action_bar_default_height_appcompat` 64dp / `_expanded` 128dp；返回键 `mz_action_bar_back_button_width` 64dp、`mz_ic_sb_back`；标题 `TextAppearance.Flyme.HeadLine.Medium` 20sp；大标题 `…ActionBar.Title.Expanded` = `fd_sys_typescale_headline_large_size` 32sp；动作按钮 `mz_action_button_min_width/height` 34×44dp；右边距 `mz_action_bar_padding_right`。

## FlymeSearchBar

`mz_action_bar_search_layout_height` 64dp；输入框 `mz_titlebar_search_view_edittext_bg`（`action_bar_search_layout_bg_color` #f4f4f5 / 夜间 #222227，按下 #e7e7e9，`?cornerMini` 8dp）；提示色 `mz_action_bar_search_text_color_hint_white` #58000000；图标 `mz_titlebar_ic_search`、清除 `mz_btn_textfield_delete`。

## FlymeEmptyView / FlymePageIndicator / FlymeBadge

- `mc_empty_view.xml`：`TextAppearance.Small.EmptyView.Title/Summary` 14sp，`mc_empty_content_panel_max_width` 256dp，`mc_empty_tips_top_port` 12dp，`mc_empty_summary_margin_bottom` 4dp
- `Widget.MeizuCommon.PagerIndicator`：`mc_pager_indicator_radius` 2dp，`enlarge_radius` 2.5dp，`distance` 8dp，`fill_color` #80ffffff，`highlight_color` #ffffff
- 角标：`mz_tool_bar_tab_view_dot_width_height` 6dp，`mz_tab_view_dot_color`

## 令牌

颜色（含夜间）、圆角、间距、字阶、阴影、动效时长/曲线的完整来源见 [TOKENS.md](TOKENS.md)。
