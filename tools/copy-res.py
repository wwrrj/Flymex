"""Copy Flyme file-based resources (drawables, colors, anims, animators, interpolators, layouts)
from apktool-decoded packages into the Flymex library, following @references transitively.

Usage: python -I tools/copy-res.py <out-res-dir> <src-res-dir>[;<src-res-dir>...] <name> [<name> ...]
       names may also be given as @file (one name per line, # comments allowed)

The first source dir that contains a name wins; every qualifier variant of that name
(drawable-xhdpi, drawable-night, ...) is copied from that source. Framework-res ids such as
@id/mask are rewritten to @android:id/mask. Referenced values resources (@color/@dimen/@string/
@integer) are reported so they can be checked against the generated values files.
"""
import os, re, shutil, sys

FILE_TYPES = ('drawable', 'color', 'anim', 'animator', 'interpolator', 'layout', 'mipmap', 'xml')
REF = re.compile(r'[@?](?:android:)?(drawable|color|anim|animator|interpolator|layout|mipmap|dimen|string|integer|style|array|id|attr|bool|fraction)/([A-Za-z0-9_.$]+)')
ANDROID_ID = re.compile(r'"@id/(mask|background|progress|secondaryProgress|icon|text1|text2|title|summary|button1|button2|button3|message|content|empty|list|checkbox|edit|hint|widget_frame|icon_frame)"')
PRIVATE = re.compile(r'\?\^attr-private/[A-Za-z0-9_]+|\?android:attr/[A-Za-z0-9_]+')


def find_variants(src_dirs, kind_hint, name):
    """-> list of (src_dir, qualifier_dir, filename)"""
    for src in src_dirs:
        hits = []
        for d in sorted(os.listdir(src)):
            base = d.split('-')[0]
            if base not in FILE_TYPES:
                continue
            if kind_hint and base != kind_hint:
                continue
            full = os.path.join(src, d)
            if not os.path.isdir(full):
                continue
            for fn in os.listdir(full):
                stem = fn.split('.')[0]
                if stem == name or stem == '$' + name:
                    hits.append((src, d, fn))
        if hits:
            return hits
    return []


def main(out_dir, src_spec, names):
    src_dirs = [s for s in src_spec.split(';') if s]
    queue, seen, copied, values_needed, warnings = [], set(), 0, {}, []
    for n in names:
        if n.startswith('@'):
            for line in open(n[1:], encoding='utf-8'):
                line = line.split('#', 1)[0].strip()
                if line:
                    queue.append((None, line))
        else:
            queue.append((None, n))
    while queue:
        kind, name = queue.pop()
        key = (kind, name)
        if key in seen:
            continue
        seen.add(key)
        hits = find_variants(src_dirs, kind, name)
        if not hits:
            warnings.append(f'missing file resource: {kind or "*"}/{name}')
            continue
        for src, qual, fn in hits:
            sp = os.path.join(src, qual, fn)
            dp = os.path.join(out_dir, qual, fn.lstrip('$'))   # apktool inline drawables: '$name__0' -> 'name__0'
            os.makedirs(os.path.dirname(dp), exist_ok=True)
            if fn.endswith('.xml'):
                text = open(sp, encoding='utf-8').read()
                text = text.replace('@drawable/$', '@drawable/')
                if os.path.basename(os.path.dirname(src)) == 'framework-res' or 'framework-res' in src:
                    text = ANDROID_ID.sub(lambda m: f'"@android:id/{m.group(1)}"', text)
                for m in PRIVATE.finditer(text):
                    warnings.append(f'{qual}/{fn}: private/framework attr {m.group(0)}')
                for m in REF.finditer(text):
                    rkind, rname = m.group(1), m.group(2)
                    if m.group(0).startswith('@android:') or m.group(0).startswith('?android:'):
                        continue
                    if rkind in FILE_TYPES:
                        queue.append((rkind, rname))
                    elif rkind in ('dimen', 'string', 'integer', 'array', 'bool', 'fraction', 'style'):
                        values_needed.setdefault(rkind, set()).add(rname)
                    elif rkind == 'color' and m.group(0).startswith('@'):
                        # could be a values colour or a res/color selector file: try file first
                        queue.append(('color', rname))
                        values_needed.setdefault('color', set()).add(rname)
                with open(dp, 'w', encoding='utf-8', newline='\n') as f:
                    f.write(text)
            else:
                shutil.copyfile(sp, dp)
            copied += 1
    print(f'copied {copied} files for {len(seen)} resources')
    for k, v in values_needed.items():
        print(f'values referenced ({k}): {len(v)}')
    for w in sorted(set(warnings)):
        print('  [warn]', w)
    with open(os.path.join(out_dir, '..', 'copy-res.report.txt'), 'w', encoding='utf-8') as f:
        for k, v in values_needed.items():
            for n in sorted(v):
                f.write(f'{k} {n}\n')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3:])
