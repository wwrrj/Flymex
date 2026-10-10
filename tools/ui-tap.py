"""Tiny adb UI driver: tap a node by its text (via uiautomator dump), swipe, screenshot.

Usage: python -I tools/ui-tap.py <serial> <out-dir> <script...>
  script tokens: tap:<text>  back  home  swipe:<x1>,<y1>,<x2>,<y2>  sleep:<sec>  shot:<name>  tapxy:<x>,<y>
"""
import io, re, subprocess, sys, time

serial, out = sys.argv[1], sys.argv[2]

def adb(*args, binary=False):
    r = subprocess.run(['adb', '-s', serial, *args], capture_output=True)
    return r.stdout if binary else r.stdout.decode('utf-8', 'replace')

def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/fx_ui.xml')
    return adb('exec-out', 'cat', '/sdcard/fx_ui.xml', binary=True).decode('utf-8', 'replace')

def tap_text(text):
    xml = dump()
    for m in re.finditer(r'<node[^>]*>', xml):
        node = m.group(0)
        t = re.search(r' text="([^"]*)"', node)
        d = re.search(r' content-desc="([^"]*)"', node)
        if (t and text in t.group(1)) or (d and text in d.group(1)):
            b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
            x1, y1, x2, y2 = map(int, b.groups())
            adb('shell', 'input', 'tap', str((x1 + x2) // 2), str((y1 + y2) // 2))
            print(f'tap "{text}" @ {(x1 + x2) // 2},{(y1 + y2) // 2}')
            return True
    print(f'NOT FOUND: {text}')
    return False

tokens = []
for a in sys.argv[3:]:
    if a.startswith('@'):
        tokens += [l.strip() for l in io.open(a[1:], encoding='utf-8') if l.strip() and not l.startswith('#')]
    else:
        tokens.append(a)
for tok in tokens:
    kind, _, arg = tok.partition(':')
    if kind == 'tap':
        tap_text(arg)
    elif kind == 'tapxy':
        x, y = arg.split(','); adb('shell', 'input', 'tap', x, y)
    elif kind == 'back':
        adb('shell', 'input', 'keyevent', '4')
    elif kind == 'stop':
        adb('shell', 'am', 'force-stop', 'com.flymex.sample')
    elif kind == 'start':
        adb('shell', 'am', 'start', '-n', 'com.flymex.sample/.MainActivity')
    elif kind == 'dump':
        open(f'{out}/{arg}.xml', 'w', encoding='utf-8').write(dump()); print('dumped', arg)
    elif kind == 'home':
        adb('shell', 'input', 'keyevent', '3')
    elif kind == 'swipe':
        x1, y1, x2, y2 = arg.split(','); adb('shell', 'input', 'swipe', x1, y1, x2, y2, '400')
    elif kind == 'sleep':
        time.sleep(float(arg))
    elif kind == 'shot':
        png = adb('exec-out', 'screencap', '-p', binary=True)
        open(f'{out}/{arg}.png', 'wb').write(png)
        print(f'shot {arg} {len(png)} bytes')
