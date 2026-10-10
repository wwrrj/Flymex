"""Generate Flymex design tokens (TS + CSS + docs) from the extracted Flyme token database.

Usage: python -I tools/gen-tokens.py <flyme-tokens.json> <pkg-src-dir> <docs-dir>
"""
import json, sys, os

ACCENTS = ['blue', 'green', 'orange', 'purple', 'red', 'yellow', 'cyan', 'polestar']
DEFAULT_ACCENT = 'blue'   # Base.Theme.Flyme.AppCompat.Light: colorPrimary -> fd_sys_color_primary_blue


def android_color_to_css(v):
    v = v.strip()
    if not v.startswith('#'):
        return None
    h = v[1:]
    if len(h) == 3:
        h = ''.join(c * 2 for c in h); a = 'ff'
    elif len(h) == 4:
        a = h[0] * 2; h = ''.join(c * 2 for c in h[1:])
    elif len(h) == 6:
        a = 'ff'
    elif len(h) == 8:
        a, h = h[:2], h[2:]
    else:
        return None
    return f'#{h}' if a.lower() == 'ff' else f'#{h}{a}'


class DB:
    def __init__(self, path):
        raw = json.load(open(path, encoding='utf-8'))
        self.tokens = raw['tokens']

    def resolve_color(self, name, mode, depth=0):
        t = self.tokens.get(name)
        if not t or depth > 6:
            return None, None
        e = t.get(mode) or t.get('light')
        if not e:
            return None, None
        v = e['value']
        if v.startswith('@color/') or v.startswith('@android:color/'):
            ref = v.split('/')[-1]
            c, src = self.resolve_color(ref, mode, depth + 1)
            return c, src or ref
        if v.startswith('?'):
            return None, v
        return android_color_to_css(v), name

    def dimen(self, name, mode='light'):
        t = self.tokens.get(name)
        if not t:
            return None
        e = t.get(mode) or t.get('light')
        if not e or 'number' not in e:
            return None
        return e['number'], e.get('unit')

    def string(self, name):
        t = self.tokens.get(name)
        if not t:
            return None
        e = t.get('light') or t.get('night')
        return e['value'] if e else None

    def integer(self, name):
        t = self.tokens.get(name)
        if not t:
            return None
        e = t.get('light')
        try:
            return int(float(e['value']))
        except Exception:
            return None


def kebab(s):
    return s.replace('_', '-')


LEGACY_COLORS = {
    '--fx-color-divider': 'mz_divider_color',
    '--fx-color-divider-block': 'mz_divider_line_block',
    '--fx-color-list-press': 'mz_dialog_alert_listitem_pressed',
    '--fx-color-list-checked': 'mz_dialog_alert_listitem_checked',
    '--fx-color-dialog-button-bg': 'mz_alert_dialog_button1_bg_color',
    '--fx-color-dialog-delete': 'mz_alert_dialog_delete_button_color',
    '--fx-color-search-bg': 'action_bar_search_layout_bg_color',
    '--fx-color-search-bg-pressed': 'action_bar_search_layout_bg_pressed_color',
    '--fx-color-search-hint': 'mz_action_bar_search_text_color_hint_white',
    '--fx-color-switch-track-on': 'mz_switch_track_on_enabled_color',
    '--fx-color-switch-track-on-disabled': 'mz_switch_track_on_disabled_color',
    '--fx-color-switch-track-off': 'mz_switch_track_off_enabled_color',
    '--fx-color-switch-track-off-disabled': 'mz_switch_track_off_disabled_color',
    '--fx-color-switch-track-on-dark': 'mz_switch_track_dark_on_enabled_color',
    '--fx-color-switch-track-on-dark-disabled': 'mz_switch_track_dark_on_disabled_color',
    '--fx-color-switch-track-off-dark': 'mz_switch_track_dark_off_enabled_color',
    '--fx-color-switch-track-off-dark-disabled': 'mz_switch_track_dark_off_disabled_color',
    '--fx-color-switch-thumb-dark-off': 'mz_switch_thumb_dark_off_enable_color',
    '--fx-color-switch-thumb-dark-on': 'mz_switch_thumb_dark_on_enable_color',
    '--fx-color-toast-complete-bg': 'mz_complete_toast_bg',
    '--fx-color-toast-text-normal': 'mc_content_toast_text_color_normal',
    '--fx-color-toast-text-error': 'mc_content_toast_text_color_error',
    '--fx-color-snackbar-action-bg': 'mz_snackbar_toast_action_bg',
    '--fx-color-loading-bg': 'mc_loading_view_background_light',
    '--fx-color-loading-bg-dark': 'mc_loading_view_background_dark',
    '--fx-color-loading-text': 'mc_loading_view_text_light',
    '--fx-color-loading-text-dark': 'mc_loading_view_text_dark',
    '--fx-color-pager-dot': 'mc_pager_indicator_fill_color',
    '--fx-color-pager-dot-active': 'mc_pager_indicator_highlight_color',
    '--fx-color-text-primary-legacy': 'mz_text_primary',
    '--fx-color-text-tips': 'mz_text_tips_desc',
    '--fx-color-mask-8e': 'mz_8e_alpha_black_color',
    '--fx-color-ripple-light': 'mz_ripple_light',
    '--fx-color-edge-effect': 'mz_edge_effect_color',
    '--fx-color-cir-btn-blank': 'mc_cir_progress_button_blank',
    '--fx-color-cir-btn-grey': 'mc_cir_progress_button_grey',
    '--fx-color-cir-btn-green': 'mc_cir_progress_button_green',
    '--fx-color-cir-btn-red': 'mc_cir_progress_button_red',
    '--fx-color-tab-dot': 'mz_tab_view_dot_color',
    '--fx-color-tab-indicator-default': 'mz_action_bar_tab_indicator_default_color',
    '--fx-color-mz-theme': 'mz_theme_color_default',
    '--fx-color-mz-theme-blue': 'mz_theme_color_blue',
}

COMPONENT_DIMENS = {
    # list / cell
    '--fx-list-item-height': 'mz_list_item_height',
    '--fx-list-item-height-small': 'mz_list_item_height_small',
    '--fx-list-item-height-single': 'mz_list_item_height_single_content',
    '--fx-list-item-height-double': 'mz_list_item_height_double',
    '--fx-list-item-height-large': 'mz_list_item_height_large',
    '--fx-list-item-pad-v-1': 'mz_list_item_content_vertical_padding_level1',
    '--fx-list-item-pad-v-2': 'mz_list_item_content_vertical_padding_level2',
    '--fx-list-item-pad-v-3': 'mz_list_item_content_vertical_padding_level3',
    '--fx-list-item-pad-v-4': 'mz_list_item_content_vertical_padding_level4',
    '--fx-list-item-pad-v-5': 'mz_list_item_content_vertical_padding_level5',
    '--fx-list-item-pad-h': 'mz_list_item_content_padding_left',
    '--fx-list-summary-gap-1': 'mz_list_summary_item_text_padding_top_level1',
    '--fx-list-summary-gap-2': 'mz_list_summary_item_text_padding_top_level2',
    '--fx-list-summary-gap-3': 'mz_list_summary_item_text_padding_top_level3',
    '--fx-list-desc-line-extra': 'mz_list_desc_line_spacing_extra',
    '--fx-list-divider-height': 'mz_list_item_divider_height',
    '--fx-list-divider-inset-start': 'mz_list_item_divider_margin_start',
    '--fx-list-divider-inset-end': 'mz_list_item_divider_margin_end',
    '--fx-list-vdivider-width': 'mz_list_item_vertical_divider_width',
    '--fx-list-vdivider-height': 'mz_list_item_vertical_divider_height',
    '--fx-list-icon-small': 'mz_list_icon_small_width',
    '--fx-list-icon-normal': 'mz_list_icon_normal_width',
    '--fx-list-icon-medium': 'mz_list_icon_medium_width',
    '--fx-list-icon-large': 'mz_list_icon_large_width',
    '--fx-list-icon-radius': 'mz_list_icon_item_icon_corner_radius',
    '--fx-list-icon-gap': 'mz_preference_icon_margin_horizontal',
    '--fx-list-widget-min-width': 'preference_widget_width',
    '--fx-list-singleline-icon': 'mz_preference_singleline_image_size',
    '--fx-list-title-size': 'mz_list_item_title_text_size',
    '--fx-list-summary-size': 'mz_list_item_summary_text_size',
    '--fx-list-text3-size': 'mz_list_item_text_3_size',
    '--fx-category-min-height': 'mz_preference_category_mini_height',
    '--fx-category-notitle-height': 'mz_preference_category_no_title_height',
    '--fx-card-title-height': 'mz_card_title_height',
    '--fx-card-title-gap': 'mz_card_title_margin_bottom',
    # buttons
    '--fx-button-min-height': 'mz_button_minHeight',
    '--fx-button-large-height': 'mz_bottom_bar_corner_button_height',
    '--fx-button-text-size': 'mz_corner_button_text_size',
    '--fx-button-stroke': 'mz_btn_stroke_width',
    '--fx-cir-btn-min-width': 'mc_cir_progress_button_min_width',
    '--fx-cir-btn-min-height': 'mc_cir_progress_button_min_height',
    '--fx-cir-btn-radius': 'mc_cir_progress_button_corner_radius',
    '--fx-cir-btn-stroke': 'mc_cir_progress_button_stroke_width',
    '--fx-cir-btn-text-size': 'mc_cir_progress_button_text_size',
    # dialog
    '--fx-dialog-width': 'mz_alert_dialog_width',
    '--fx-dialog-max-height': 'mz_alert_dialog_max_height',
    '--fx-dialog-button-bar-height': 'mz_alert_dialog_button_bar_height',
    '--fx-dialog-button-h-height': 'mz_alert_dialog_horizontal_button_height',
    '--fx-dialog-button-v-height': 'mz_alert_dialog_vertical_button_height',
    '--fx-dialog-button-text-size': 'mz_alert_dialog_button_text_size',
    '--fx-dialog-button-margin-start': 'mz_alert_dialog_button_margin_start',
    '--fx-dialog-button-min-padding': 'mz_alert_dialog_btn_min_padding',
    '--fx-dialog-title-top': 'mz_alertDialog_title_margin_top',
    '--fx-dialog-title-min-height': 'mz_alert_dialog_title_min_height',
    '--fx-dialog-message-pad-h': 'mz_alert_dialog_message_padding_left',
    '--fx-dialog-message-pad-bottom': 'mz_alert_dialog_message_padding_bottom',
    '--fx-dialog-list-item-height': 'mz_alertDialog_list_item_height',
    '--fx-dialog-bottom-margin': 'mz_alert_dialog_margin_bottom_to_screen',
    '--fx-dialog-min-height': 'mz_alert_dialog_panel_min_height',
    '--fx-dialog-edittext-height': 'mz_alert_dialog_edittext_height',
    '--fx-dialog-edittext-text-size': 'mz_alert_dialog_edittext_text_size',
    '--fx-dialog-top-panel-max-height': 'mz_alert_dialog_top_panel_max_height',
    '--fx-dialog-title-icon': 'mz_alert_dialog_title_icon_width',
    # popup menu
    '--fx-popup-item-height': 'mz_popup_menu_item_height',
    '--fx-popup-min-width': 'mz_popup_menu_item_min_width',
    '--fx-popup-max-width': 'mz_popup_menu_width_max',
    '--fx-popup-item-pad-h': 'mz_popup_list_item_content_padding_left',
    '--fx-popup-item-radius': 'mz_popup_item_radius',
    '--fx-popup-item-pad-ext': 'mz_popup_menu_item_padding_ext',
    # action bar / title bar
    '--fx-appbar-height': 'mz_action_bar_default_height_appcompat',
    '--fx-appbar-height-expanded': 'mz_action_bar_default_height_appcompat_expanded',
    '--fx-appbar-back-width': 'mz_action_bar_back_button_width',
    '--fx-appbar-icon-pad-h': 'mz_action_bar_icon_horizontal_padding',
    '--fx-appbar-action-min-width': 'mz_action_button_min_width',
    '--fx-appbar-action-min-height': 'mz_action_button_min_height',
    '--fx-appbar-progress-height': 'mz_action_bar_progress_height',
    '--fx-appbar-search-height': 'mz_action_bar_search_layout_height',
    '--fx-appbar-search-button-width': 'mz_action_bar_search_button_width',
    # tabs
    '--fx-tab-height': 'mz_tab_height',
    '--fx-tab-bar-height-nested': 'mz_tab_bar_height_nested_scroll',
    '--fx-tab-indicator-pad-bottom': 'mz_action_bar_tab_indicator_padding_bottom',
    '--fx-tab-indicator-height': 'mz_action_bar_tab_indicator_height',
    '--fx-tab-indicator-width': 'mz_action_bar_tab_indicator_width',
    '--fx-tab-indicator-width-second': 'mz_action_bar_tab_indicator_width_second',
    '--fx-tab-top-height': 'mz_action_bar_top_tab_height',
    '--fx-tab-capsule-height': 'mz_action_bar_tab_capsule_style_height',
    '--fx-tab-view-pad-h': 'mz_tool_bar_tab_view_padding_left_small',
    '--fx-tab-view-pad-h-large': 'mz_tool_bar_tab_view_padding_left_large',
    '--fx-tab-view-min-width': 'mz_action_bar_tab_view_mini_width',
    '--fx-tab-dot-size': 'mz_tool_bar_tab_view_dot_width_height',
    # toast
    '--fx-toast-pad-h': 'mz_toast_padding',
    '--fx-toast-pad-top': 'mz_toast_padding_top',
    '--fx-toast-y-offset': 'mz_toast_y_offset',
    '--fx-content-toast-radius': 'mc_content_toast_bg_corner_radius',
    '--fx-content-toast-min-height': 'mc_content_toast_min_height',
    '--fx-content-toast-pad-h': 'mc_content_toast_content_horizontal_padding',
    '--fx-content-toast-width': 'mc_content_toast_content_width_default',
    '--fx-content-toast-action-radius': 'mc_content_toast_action_text_corner_radius',
    '--fx-content-toast-action-pad-h': 'mc_content_toast_action_text_padding_horizontal',
    '--fx-content-toast-action-pad-v': 'mc_content_toast_action_text_padding_vertical',
    '--fx-content-toast-margin-bottom': 'mc_content_toast_content_margin_bottom_default',
    '--fx-content-toast-line-spacing': 'mc_content_toast_line_spacing',
    '--fx-complete-toast-width': 'mz_complete_toast_width',
    '--fx-complete-toast-height': 'mz_complete_toast_height',
    '--fx-complete-toast-icon-gap': 'mz_complete_toast_icon_margin_bottom',
    '--fx-complete-toast-pad': 'mz_complete_toast_background_padding',
    '--fx-complete-toast-pad-h': 'mz_complete_toast_text_margin_horizontal',
    # slider / seekbar
    '--fx-slider-track-height': 'mc_enhance_seekbar_stroke_width',
    '--fx-slider-enhance-pad': 'mc_enhance_seekbar_padding',
    '--fx-slider-enhance-distance': 'mc_enhance_seekbar_distance',
    '--fx-slider-spot-radius': 'mc_enhance_seekbar_spot_radius',
    '--fx-slider-pad-v': 'mc_seekbar_vertical_padding_top',
    # loading / progress
    '--fx-loading-radius': 'mcLoadingRadius',
    '--fx-loading-ring-width': 'mcRingWidth',
    '--fx-circle-progress-size': 'mz_circle_progressbar_layout_width',
    '--fx-circle-progress-stroke': 'mz_circle_progressbar_width',
    # page indicator
    '--fx-pager-dot-radius': 'mc_pager_indicator_radius',
    '--fx-pager-dot-radius-active': 'mc_pager_indicator_enlarge_radius',
    '--fx-pager-dot-gap': 'mc_pager_indicator_distance',
    # switch (MeizuCommon Switch; thumb morphs from a 9x4 pill (off) to a 14x14 circle (on))
    '--fx-switch-min-width': 'mcSwitchMinWidth',
    '--fx-switch-thumb-off-width': 'mz_switch_thumb_from_width',
    '--fx-switch-thumb-off-height': 'mz_switch_thumb_from_height',
    '--fx-switch-thumb-on-size': 'mz_switch_thumb_to_width',
    '--fx-switch-thumb-pad': 'mz_switch_thumb_padding_left',
    # misc
    '--fx-disabled-alpha': 'mz_disabled_alpha_light',
    '--fx-banner-radius': 'mz_banner_view_corner_radius',
    '--fx-qs-radius': 'qs_corner_radius',
    '--fx-qs-tile-size': 'qs_quick_tile_size',
    '--fx-notification-radius': 'notification_corner_radius',
    '--fx-notification-bg-radius': 'notification_background_radius',
    '--fx-search-bar-radius': 'search_bar_corner_radius',
}

MOTION = {
    'duration-short': (200, 'framework config_shortAnimTime'),
    'duration-medium': (400, 'framework config_mediumAnimTime'),
    'duration-long': (500, 'framework config_longAnimTime'),
    'duration-press': (64, 'framework config_pressedStateDurationMillis'),
    'duration-tooltip': (150, 'config_tooltipAnimTime'),
    'duration-fragment': (300, 'config_fragmentAnimTime'),
    'duration-dialog-in': (250, 'anim/mz_dialog_alert_anim_enter'),
    'duration-dialog-out': (200, 'anim/mz_dialog_alert_anim_exit'),
    'duration-sheet': (304, 'integer/bottom_dialog_anim_duration'),
    'duration-popup-alpha': (100, 'anim/mz_overflow_popup_enter'),
    'duration-popup-scale': (150, 'anim/mz_overflow_popup_enter'),
    'duration-snackbar-scale': (200, 'anim/mz_snackbar_enter_anim'),
    'duration-snackbar-alpha': (150, 'anim/mz_snackbar_enter_anim'),
    'duration-check': (200, 'animator/mc_anim_btn_checked_fade_in'),
    'duration-dialog-show-sysui': (300, 'systemui config_dialogShowAnimationDurationMs'),
    'duration-dialog-hide-sysui': (250, 'systemui config_dialogHideAnimationDurationMs'),
}

EASING = {
    'ease-dialog-in': ('cubic-bezier(0.25, 0.24, 0.19, 0.99)', 'interpolator/mz_dialog_alert_short_translate_enter_interpolator'),
    'ease-dialog-out': ('cubic-bezier(0.33, 0, 0.67, 1)', 'interpolator/mz_dialog_alert_short_translate_exit_interpolator'),
    'ease-sheet-in': ('cubic-bezier(0.2, 0.5, 0.05, 1)', 'interpolator/mz_showat_bottom_dialog_in_interpolator_'),
    'ease-sheet-out': ('cubic-bezier(0.29, 0.5, 0.16, 1)', 'interpolator/mz_showat_bottom_dialog_out_interpolator'),
    'ease-popup-scale': ('cubic-bezier(0.24, 0, 0.26, 1)', 'interpolator/mz_overflow_popup_scale_enter_anim_interpolator'),
    'ease-popup-alpha': ('cubic-bezier(0.33, 0, 0.67, 1)', 'interpolator/mz_overflow_popup_alpha_anim_interpolator'),
    'ease-snackbar-scale': ('cubic-bezier(0.18, 0.7, 0.05, 1)', 'interpolator/mz_snackbar_scale_blowup_interpolator'),
    'ease-snackbar-alpha': ('cubic-bezier(0.65, 0, 0.35, 1)', 'interpolator/mz_snackbar_alpha_interpolator'),
    'ease-check': ('cubic-bezier(0.2, 0.3, 0.21, 0.97)', 'interpolator/mc_btn_checked_single_interpolator'),
    'ease-standard': ('cubic-bezier(0.33, 0, 0.67, 1)', 'Flyme symmetric ease shared by exit/alpha curves'),
}

FONT_FAMILY = '"Flyme Sans", "FlymeSans", "PingFang SC", "HarmonyOS Sans SC", "MiSans", "Noto Sans SC", "Source Han Sans SC", -apple-system, "Segoe UI", Roboto, system-ui, sans-serif'
FONT_FAMILY_NUMBER = '"Flyme Number", "FlymeNumber", "SF Pro Display", var(--fx-font-family)'


def main(db_path, src_dir, docs_dir):
    db = DB(db_path)
    provenance = []

    # ---------- semantic colors (fd_sys_color_*) ----------
    roles = {}
    for name in db.tokens:
        if not name.startswith('fd_sys_color_'):
            continue
        rest = name[len('fd_sys_color_'):]
        accent = None
        for a in ACCENTS + ['hongqi', 'hongqi2', 'hongqi3']:
            if rest.endswith('_' + a):
                rest, accent = rest[:-(len(a) + 1)], a
                break
        if accent is None and rest.endswith('_default'):
            rest, accent = rest[:-len('_default')], 'default'
        roles.setdefault(rest, {})[accent] = name

    def color_pair(name):
        l, _ = db.resolve_color(name, 'light')
        d, _ = db.resolve_color(name, 'night')
        return l, (d or l)

    base_light, base_dark = {}, {}
    accent_vars = {a: ({}, {}) for a in ACCENTS}
    for role in sorted(roles):
        m = roles[role]
        var = f'--fx-color-{kebab(role)}'
        if ('default' in m or None in m) and not any(x in m for x in ACCENTS):
            name = m.get('default', m.get(None))
            l, d = color_pair(name)
            if l:
                base_light[var], base_dark[var] = l, d
                provenance.append((var, name, l, d))
        if any(a in m for a in ACCENTS):
            for a in ACCENTS:
                if a in m:
                    l, d = color_pair(m[a])
                    if l:
                        accent_vars[a][0][var], accent_vars[a][1][var] = l, d
                        provenance.append((f'{var} [accent={a}]', m[a], l, d))
            if DEFAULT_ACCENT in m:
                l, d = color_pair(m[DEFAULT_ACCENT])
                if l:
                    base_light.setdefault(var, l)
                    base_dark.setdefault(var, d)
    # accents that lack a role fall back to the base (blue) so every accent block is complete
    accent_roles = sorted({v for a in ACCENTS for v in accent_vars[a][0]})
    for a in ACCENTS:
        for var in accent_roles:
            accent_vars[a][0].setdefault(var, base_light.get(var))
            accent_vars[a][1].setdefault(var, base_dark.get(var))
        for mode in (0, 1):
            kept = {k: v for k, v in accent_vars[a][mode].items() if v and v != 'None'}
            accent_vars[a][mode].clear(); accent_vars[a][mode].update(kept)

    # ---------- legacy ramps ----------
    for i in range(1, 11):
        for src, var in ((f'Blue_{i}', f'--fx-blue-{i}'), (f'mz_theme_color_level{i}_default', f'--fx-level-{i}')):
            l, d = color_pair(src)
            if l:
                base_light[var], base_dark[var] = l, d
                provenance.append((var, src, l, d))
    for var, src in LEGACY_COLORS.items():
        l, d = color_pair(src)
        if l:
            base_light[var], base_dark[var] = l, d
            provenance.append((var, src, l, d))
        else:
            print('  [warn] missing color', src)

    # ---------- dimens ----------
    dims = {}

    def add_dim(var, src):
        r = db.dimen(src)
        if r is None:
            print('  [warn] missing dimen', src)
            return
        num, unit = r
        if unit == 'px':
            num = round(num / 4, 3)   # xxxhdpi assets -> dp
        dims[var] = (num, unit, src)

    for lvl in ['none', 'extra_mini', 'mini', 'small', 'regular', 'medium', 'large', 'heavy', 'extra_heavy']:
        src = 'fd_sys_radius_corner_none' if lvl == 'none' else f'fd_sys_radius_corner_smooth_{lvl}'
        add_dim(f'--fx-radius-{kebab(lvl)}', src)
    add_dim('--fx-stroke-width', 'fd_sys_radius_corner_stroke_width')
    for lvl in ['none', 'extra_mini', 'mini', 'small', 'regular', 'medium', 'large', 'heavy', 'extra_heavy']:
        add_dim(f'--fx-space-{kebab(lvl)}', f'fd_sys_margin_{lvl}')
    add_dim('--fx-space-bottom-default', 'fd_sys_margin_bottom_default')
    add_dim('--fx-space-bottom-max', 'fd_sys_margin_bottom_max')
    add_dim('--fx-space-page', 'mz_page_padding_default')
    add_dim('--fx-shadow-elevation-medium', 'fd_sys_shadow_elevation_medium')
    add_dim('--fx-shadow-elevation-large', 'fd_sys_shadow_elevation_large')
    add_dim('--fx-shadow-radius-small', 'fd_sys_shadow_radius_small')
    add_dim('--fx-shadow-padding-small', 'fd_sys_shadow_padding_small')
    for var, src in COMPONENT_DIMENS.items():
        add_dim(var, src)

    # ---------- typescale ----------
    type_scale = {}
    for group, lvls in (('headline', ['large', 'medium', 'small', 'mini']),
                        ('body', ['large', 'medium', 'small', 'mini']),
                        ('label', ['large', 'medium', 'small', 'mini'])):
        for lvl in lvls:
            size = db.dimen(f'fd_sys_typescale_{group}_{lvl}_size')
            font = db.string(f'fd_sys_typescale_{group}_{lvl}_font') or 'sans-serif'
            tracking = db.dimen(f'fd_sys_typescale_{group}_{lvl}_tracking')
            derived = False
            if size is None:
                size, derived = (12.0, 'sp'), True   # label_small has no size resource; Flyme uses 12sp (mz_text_size_b6)
            weight = 500 if 'medium' in font else (700 if 'bold' in font else 400)
            ratio = 1.3 if group == 'headline' else 1.4
            type_scale[f'{group}-{lvl}'] = dict(size=size[0], weight=weight, line=round(size[0] * ratio),
                                               tracking=(tracking[0] if tracking else 0.0), font=font, derived=derived)
    # legacy mz text sizes (H6..H12, B6, B7, normal/medium/large/small/little/mini)
    legacy_text = {}
    for key in ['h6', 'h8', 'h9', 'h10', 'h11', 'h12', 'b6', 'b7', 'normal', 'medium', 'large', 'small', 'little', 'little2', 'mini']:
        r = db.dimen(f'mz_text_size_{key}')
        if r:
            legacy_text[key] = r[0]

    # ---------- CSS ----------
    def block(selector, mapping):
        out = [f'{selector} {{']
        for k in sorted(mapping):
            out.append(f'  {k}: {mapping[k]};')
        out.append('}')
        return '\n'.join(out)

    def indent(s):
        return '\n'.join('  ' + l for l in s.split('\n'))

    css = ['/* AUTO-GENERATED by tools/gen-tokens.py from Flyme 12.6 (meizu 21 Pro) system resources. Do not edit. */']
    root = dict(base_light)
    for var, (num, unit, src) in dims.items():
        root[var] = f'{num:g}' if var.endswith('-alpha') else f'{num:g}px'
    for key, t in type_scale.items():
        root[f'--fx-type-{key}-size'] = f"{t['size']:g}px"
        root[f'--fx-type-{key}-line'] = f"{t['line']}px"
        root[f'--fx-type-{key}-weight'] = str(t['weight'])
        root[f'--fx-type-{key}-tracking'] = f"{t['tracking']:g}em"
    for key, size in legacy_text.items():
        root[f'--fx-text-{key}'] = f'{size:g}px'
    for k, (v, _) in MOTION.items():
        root[f'--fx-{k}'] = f'{v}ms'
    for k, (v, _) in EASING.items():
        root[f'--fx-{k}'] = v
    root['--fx-font-family'] = FONT_FAMILY
    root['--fx-font-family-number'] = FONT_FAMILY_NUMBER
    root['--fx-shadow-small'] = '0 8px 8px var(--fx-color-shadow-small)'
    root['--fx-shadow-medium'] = '0 20px 40px var(--fx-color-shadow-spot-medium)'
    root['--fx-shadow-large'] = '0 30px 60px var(--fx-color-shadow-spot-large)'
    css.append(block(':root, .fx-root, [data-fx-theme="light"]', root))
    css.append(block('.fx-dark, [data-fx-theme="dark"]', base_dark))
    css.append('@media (prefers-color-scheme: dark) {\n' + indent(block('[data-fx-theme="system"]', base_dark)) + '\n}')
    for a in ACCENTS:
        l, d = accent_vars[a]
        if not l:
            continue
        css.append(block(f'[data-fx-accent="{a}"]', l))
        css.append(block(f'.fx-dark[data-fx-accent="{a}"], [data-fx-theme="dark"][data-fx-accent="{a}"], '
                         f'.fx-dark [data-fx-accent="{a}"], [data-fx-theme="dark"] [data-fx-accent="{a}"]', d))
        css.append('@media (prefers-color-scheme: dark) {\n' + indent(block(
            f'[data-fx-theme="system"][data-fx-accent="{a}"], [data-fx-theme="system"] [data-fx-accent="{a}"]', d)) + '\n}')
    os.makedirs(os.path.join(src_dir, 'tokens'), exist_ok=True)
    with open(os.path.join(src_dir, 'tokens', 'tokens.gen.css'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n\n'.join(css) + '\n')

    # ---------- TS ----------
    def ts_obj(d, ind=2):
        pad = ' ' * ind
        return '{\n' + '\n'.join(f'{pad}{json.dumps(k)}: {json.dumps(v)},' for k, v in sorted(d.items())) + '\n' + ' ' * (ind - 2) + '}'

    ts = ['// AUTO-GENERATED by tools/gen-tokens.py from Flyme 12.6 system resources. Do not edit.', '',
          'export type FxAccent = ' + ' | '.join(json.dumps(a) for a in ACCENTS) + ';',
          f'export const fxAccents = [{", ".join(json.dumps(a) for a in ACCENTS)}] as const;',
          f'export const fxDefaultAccent: FxAccent = {json.dumps(DEFAULT_ACCENT)};', '',
          '/** Semantic + legacy colors, light scheme. Keys are CSS custom property names. */',
          'export const fxColorsLight: Record<string, string> = ' + ts_obj(base_light) + ';', '',
          '/** Semantic + legacy colors, dark (Flyme night) scheme. */',
          'export const fxColorsDark: Record<string, string> = ' + ts_obj(base_dark) + ';', '',
          '/** Accent overrides: accent -> { light, dark } CSS custom property maps. */',
          'export const fxAccentColors: Record<FxAccent, { light: Record<string, string>; dark: Record<string, string> }> = {']
    for a in ACCENTS:
        l, d = accent_vars[a]
        ts.append(f'  {json.dumps(a)}: {{ light: {ts_obj(l, 4)}, dark: {ts_obj(d, 4)} }},')
    ts += ['};', '',
           '/** Dimension tokens in dp (1dp == 1 CSS px). */',
           'export const fxDimens: Record<string, { value: number; unit: string | null; source: string }> = {']
    for var, (num, unit, src) in sorted(dims.items()):
        ts.append(f'  {json.dumps(var)}: {{ value: {num:g}, unit: {json.dumps(unit)}, source: {json.dumps(src)} }},')
    ts += ['};', '',
           'export interface FxTypeStyle { size: number; line: number; weight: number; tracking: number; font: string; derived: boolean }',
           'export const fxTypeScale: Record<string, FxTypeStyle> = {']
    for k, t in type_scale.items():
        ts.append(f'  {json.dumps(k)}: {json.dumps(t)},')
    ts += ['};', '', '/** Legacy Flyme text sizes (mz_text_size_*), in sp. */',
           'export const fxLegacyTextSizes: Record<string, number> = ' + ts_obj(legacy_text) + ';', '',
           'export const fxMotion: Record<string, { ms: number; source: string }> = {']
    for k, (v, s) in MOTION.items():
        ts.append(f'  {json.dumps(k)}: {{ ms: {v}, source: {json.dumps(s)} }},')
    ts += ['};', '', 'export const fxEasing: Record<string, { css: string; source: string }> = {']
    for k, (v, s) in EASING.items():
        ts.append(f'  {json.dumps(k)}: {{ css: {json.dumps(v)}, source: {json.dumps(s)} }},')
    ts += ['};', '']
    with open(os.path.join(src_dir, 'tokens', 'tokens.gen.ts'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(ts))

    # ---------- docs ----------
    os.makedirs(docs_dir, exist_ok=True)
    doc = ['# Flymex design tokens — provenance', '',
           'Every token below was read from the decompiled Flyme 12.6.0.0A (meizu 21 Pro, Android 16) resources of',
           '`framework-res.apk`, `Settings.apk` and `SystemUI.apk`. Values are copied verbatim; colors are converted from',
           'Android `#AARRGGBB` to CSS `#RRGGBBAA`.', '',
           '## Colors', '', '| CSS variable | Flyme resource | Light | Dark |', '|---|---|---|---|']
    for var, src, l, d in provenance:
        doc.append(f'| `{var}` | `{src}` | `{l}` | `{d}` |')
    doc += ['', '## Dimensions', '', '| CSS variable | Flyme resource | Value |', '|---|---|---|']
    for var, (num, unit, src) in sorted(dims.items()):
        doc.append(f'| `{var}` | `{src}` | {num:g}{unit or ""} |')
    doc += ['', '## Typescale (`fd_sys_typescale_*`)', '',
            '| Scale | Size | Weight | Font resource | Line height (derived: ×1.3 headline, ×1.4 body/label) |', '|---|---|---|---|---|']
    for k, t in type_scale.items():
        doc.append(f'| {k} | {t["size"]:g}sp{" (derived)" if t["derived"] else ""} | {t["weight"]} | `{t["font"]}` | {t["line"]}px |')
    doc += ['', '## Legacy text sizes (`mz_text_size_*`)', '', '| Key | Size |', '|---|---|']
    for k, v in legacy_text.items():
        doc.append(f'| {k} | {v:g}sp |')
    doc += ['', '## Motion', '', '| Token | Value | Flyme source |', '|---|---|---|']
    for k, (v, s) in MOTION.items():
        doc.append(f'| `--fx-{k}` | {v}ms | `{s}` |')
    for k, (v, s) in EASING.items():
        doc.append(f'| `--fx-{k}` | `{v}` | `{s}` |')
    with open(os.path.join(docs_dir, 'TOKENS.md'), 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(doc) + '\n')
    print(f'colors: light={len(base_light)} dark={len(base_dark)} accents={[a for a in ACCENTS if accent_vars[a][0]]}')
    print(f'dimens: {len(dims)}  typescale: {len(type_scale)}  legacy text: {len(legacy_text)}  provenance rows: {len(provenance)}')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
