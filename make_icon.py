#!/usr/bin/env python3
# 生成纯黑白 PNG 图标（适配墨水屏），仅用标准库，无 PIL 依赖。
# 图案：白色背景 + 黑色粗边框方块 + 四宫格应用块。
import struct, zlib, os

def make_png(size, path):
    W = H = size
    S = size / 108.0  # 以 108 画布为基准缩放

    def px(x, y):
        # 转回 108 坐标系
        X, Y = x / S, y / S
        # 外框：26..82，描边宽 7
        outer = 26.0 <= X <= 82.0 and 26.0 <= Y <= 82.0
        inner = 29.5 <= X <= 78.5 and 29.5 <= Y <= 78.5
        border = outer and not inner
        # 四宫格实心块
        blocks = (
            (38.0 <= X <= 50.0 and 38.0 <= Y <= 50.0) or
            (58.0 <= X <= 70.0 and 38.0 <= Y <= 50.0) or
            (38.0 <= X <= 50.0 and 58.0 <= Y <= 70.0) or
            (58.0 <= X <= 70.0 and 58.0 <= Y <= 70.0)
        )
        return 0 if (border or blocks) else 255  # 黑=0 白=255

    raw = bytearray()
    for y in range(H):
        raw.append(0)  # filter type 0
        for x in range(W):
            v = px(x, y)
            raw += bytes((v, v, v, 255))  # RGBA

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        c += struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
        return c

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', W, H, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)
    print(f'{path}  {W}x{H}  {len(png)} bytes')

if __name__ == '__main__':
    root = os.path.dirname(os.path.abspath(__file__))
    for dpi, size in [('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)]:
        d = os.path.join(root, 'res', f'mipmap-{dpi}')
        os.makedirs(d, exist_ok=True)
        make_png(size, os.path.join(d, 'ic_launcher.png'))
