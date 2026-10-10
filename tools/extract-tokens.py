"""Extract Flyme design tokens from decompiled Flyme system resources.

Reads the apktool-decoded `res/values*/{colors,dimens,strings,integers}.xml` of the
Flyme packages and writes a single machine-readable token database (with provenance).

Usage: python -I tools/extract-tokens.py <out.json> <pkg=path> [<pkg=path> ...]
"""
import json, re, sys, os

ENTRY = re.compile(r'<(color|dimen|string|integer|item)\b([^>]*)>(.*?)</\1>', re.S)
ATTR = re.compile(r'(\w[\w:-]*)="([^"]*)"')

def parse_values_dir(d):
    """name -> {type, value, where}"""
    out = {}
    if not os.path.isdir(d):
        return out
    for fn in os.listdir(d):
        if not fn.endswith('.xml'):
            continue
        path = os.path.join(d, fn)
        text = open(path, encoding='utf-8').read()
        for m in ENTRY.finditer(text):
            tag, raw_attrs, body = m.group(1), m.group(2), m.group(3)
            a = dict(ATTR.findall(raw_attrs))
            name = a.get('name')
            if not name:
                continue
            typ = a.get('type', tag)
            out.setdefault(name, {'type': typ, 'value': body.strip(), 'where': f'{fn}'})
    return out

def classify_dimen(value):
    m = re.match(r'^(-?[\d.]+)\s*([a-z%]*)$', value)
    if not m:
        return None, value
    num, unit = float(m.group(1)), m.group(2)
    if unit in ('dip', 'dp', ''):
        return 'dp', num
    if unit == 'sp':
        return 'sp', num
    if unit == 'px':
        return 'px', num
    return unit, num

def main(out_path, pkgs):
    db = {'packages': {}, 'tokens': {}}
    for spec in pkgs:
        pkg, root = spec.split('=', 1)
        light = parse_values_dir(os.path.join(root, 'values'))
        night = parse_values_dir(os.path.join(root, 'values-night'))
        db['packages'][pkg] = {
            'root': root,
            'light': light,
            'night': night,
            'counts': {'light': len(light), 'night': len(night)},
        }
    # Build the flat resolution map for names we care about (flyme design + meizu style)
    keep = re.compile(r'^(fd_[a-z0-9_]+|mz_[A-Za-z0-9_]+|mc_[A-Za-z0-9_]+|mc[A-Z][A-Za-z0-9]+|meizu_[a-z0-9_]+|flyme_[a-z0-9_]+|'
                      r'Blue_[0-9]+|[A-Z][A-Za-z0-9]*_[0-9]+|theme_color|action_bar_search_layout_bg[a-z_]*|'
                      r'black_fourty_alpha|preference_widget_width|bottom_dialog_anim_duration|bottom_sheet_slide_duration|'
                      r'config_[a-zA-Z]*Anim[a-zA-Z]*|config_dialog[A-Za-z]*|config_tooltipAnimTime|config_pressedStateDurationMillis|'
                      r'qs_corner_radius|qs_quick_tile_size|qs_icon_size|notification_corner_radius|notification_background_radius|'
                      r'search_bar_corner_radius|error_alpha_text_color|white|black|transparent)$')
    order = ['settings', 'systemui', 'framework-res']
    for name in sorted({n for p in db['packages'].values() for n in list(p['light']) + list(p['night'])}):
        if not keep.match(name):
            continue
        entry = {'name': name}
        for mode in ('light', 'night'):
            found = None
            for pkg in order:
                p = db['packages'].get(pkg)
                if not p:
                    continue
                src = p[mode].get(name)
                if src:
                    found = {'pkg': pkg, **src}
                    break
            if found:
                v = found['value']
                if found['type'] == 'dimen' or (found['type'] == 'item' and re.match(r'^-?[\d.]', v)):
                    unit, num = classify_dimen(v)
                    found['unit'], found['number'] = unit, num
                entry[mode] = found
        if 'light' in entry or 'night' in entry:
            db['tokens'][name] = entry
    json.dump(db, open(out_path, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    print(f'wrote {out_path}: {len(db["tokens"])} tokens')
    for pkg, p in db['packages'].items():
        print(f'  {pkg}: light={p["counts"]["light"]} night={p["counts"]["night"]}')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2:])
