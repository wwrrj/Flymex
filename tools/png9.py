"""Decode 9-patch PNG geometry: stored transparent border, content padding, colors.

Usage: python -I tools/png9.py <file.9.png> [more.9.png ...]
"""
import struct, sys, zlib

def read_png(path):
    data = open(path, 'rb').read()
    assert data[:8] == b'\x89PNG\r\n\x1a\n', 'not a png'
    pos, idat, hdr = 8, b'', None
    while pos < len(data):
        (length,) = struct.unpack('>I', data[pos:pos+4])
        ctype = data[pos+4:pos+8]
        chunk = data[pos+8:pos+8+length]
        if ctype == b'IHDR':
            w, h, depth, color, comp, filt, inter = struct.unpack('>IIBBBBB', chunk)
            hdr = dict(w=w, h=h, depth=depth, color=color, interlace=inter)
        elif ctype == b'IDAT':
            idat += chunk
        elif ctype == b'IEND':
            break
        pos += 12 + length
    raw = zlib.decompress(idat)
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[hdr['color']]
    stride = hdr['w'] * nch
    rows, prev, p = [], bytearray(stride), 0
    for _ in range(hdr['h']):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p+stride]); p += stride
        if f == 1:
            for i in range(nch, stride): line[i] = (line[i] + line[i-nch]) & 0xFF
        elif f == 2:
            for i in range(stride): line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0
                b = prev[i]; c = prev[i-nch] if i >= nch else 0
                pa, pb, pc = abs(b-c), abs(a-c), abs(a+b-2*c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        rows.append(bytes(line)); prev = line
    return hdr, nch, rows

def px(row, nch, x):
    o = x * nch
    if nch == 4: return row[o], row[o+1], row[o+2], row[o+3]
    if nch == 3: return row[o], row[o+1], row[o+2], 255
    if nch == 1: return row[o], row[o], row[o], 255
    return 0, 0, 0, 255

def black_runs(seq):
    runs, start = [], None
    for i, v in enumerate(seq):
        if v and start is None: start = i
        elif not v and start is not None:
            runs.append((start, i - 1)); start = None
    if start is not None: runs.append((start, len(seq) - 1))
    return runs

for path in sys.argv[1:]:
    try:
        hdr, nch, rows = read_png(path)
    except Exception as e:
        print(path, 'ERR', e); continue
    w, h = hdr['w'], hdr['h']
    top = [px(rows[0], nch, x)[3] < 128 for x in range(w)]
    left = [px(rows[y], nch, 0)[3] < 128 for y in range(h)]
    # 9-patch borders are fully opaque black/transparent markers; treat non-opaque as patch edge
    inner = (px(rows[2], nch, 2), px(rows[h//2], nch, w//2), px(rows[h-3], nch, w-3))
    print(f'{path}')
    print(f'  size={w}x{h}px ({w/4:g}x{h/4:g}dp @xxxhdpi)  channels={nch}')
    print(f'  top-marks(black runs, x)=  {black_runs(top)}')
    print(f'  left-marks(black runs, y)= {black_runs(left)}')
    print(f'  pixels rgb/corner(2,2)={inner[0]} center={inner[1]} br={inner[2]}')
