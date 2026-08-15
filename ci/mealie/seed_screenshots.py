#!/usr/bin/env python3
"""Create a small, obviously fake Mealie account and recipe set for screenshots.

This talks only to the disposable Mealie container started by screenshots CI. It never reads
credentials from the repository or from a user's Mealie instance.
"""

import argparse
import json
import struct
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib


def request(base_url, path, method="GET", payload=None, token=None, form=False):
    url = f"{base_url.rstrip('/')}/{path.lstrip('/')}"
    if payload is None:
        data = None
    elif form:
        data = urllib.parse.urlencode(payload).encode("utf-8")
    else:
        data = json.dumps(payload).encode("utf-8")
    headers = {"Accept": "application/json"}
    if data is not None:
        headers["Content-Type"] = (
            "application/x-www-form-urlencoded" if form else "application/json"
        )
    if token:
        headers["Authorization"] = f"Bearer {token}"
    request = urllib.request.Request(url, data=data, headers=headers, method=method)
    with urllib.request.urlopen(request, timeout=30) as response:
        body = response.read()
        if not body:
            return None
        content_type = response.headers.get("Content-Type", "")
        return json.loads(body) if "json" in content_type else body.decode("utf-8")


def multipart_request(base_url, path, token, fields, file_name, file_body, content_type):
    boundary = "----SyncwichScreenshotFixture"
    parts = []
    for name, value in fields.items():
        parts.append(
            f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n".encode()
        )
    parts.append(
        (
            f'--{boundary}\r\nContent-Disposition: form-data; name="image"; '
            f'filename="{file_name}"\r\nContent-Type: {content_type}\r\n\r\n'
        ).encode()
        + file_body
        + b"\r\n"
    )
    parts.append(f"--{boundary}--\r\n".encode())
    request = urllib.request.Request(
        f"{base_url.rstrip('/')}/{path.lstrip('/')}",
        data=b"".join(parts),
        headers={
            "Accept": "application/json",
            "Authorization": f"Bearer {token}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        method="PUT",
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def cover_png(index):
    """Return a small deterministic food-like PNG without external image dependencies."""
    width, height = 640, 400
    colors = ((226, 109, 92), (54, 132, 108), (224, 163, 67))
    accent = colors[index % len(colors)]
    pixels = bytearray()
    for y in range(height):
        pixels.append(0)
        for x in range(width):
            mix = y / height
            pixels.extend(
                (
                    int(accent[0] * (1 - mix) + 245 * mix),
                    int(accent[1] * (1 - mix) + 210 * mix),
                    int(accent[2] * (1 - mix) + 165 * mix),
                )
            )

    def paint_ellipse(cx, cy, rx, ry, color):
        for y in range(max(0, cy - ry), min(height, cy + ry)):
            for x in range(max(0, cx - rx), min(width, cx + rx)):
                if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1:
                    offset = (y * (width * 3 + 1)) + 1 + x * 3
                    pixels[offset : offset + 3] = bytes(color)

    paint_ellipse(320, 218, 210, 125, (248, 242, 226))
    paint_ellipse(320, 218, 185, 100, tuple(max(0, c - 20) for c in accent))
    for offset, color in enumerate(((245, 198, 73), (207, 75, 64), (79, 145, 91))):
        paint_ellipse(230 + offset * 90, 190 + (offset % 2) * 42, 42, 30, color)
    paint_ellipse(318, 255, 54, 26, (239, 222, 166))

    def chunk(kind, data):
        return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)

    raw = zlib.compress(bytes(pixels), level=9)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0)) + chunk(b"IDAT", raw) + chunk(b"IEND", b"")


def wait_for_mealie(base_url):
    deadline = time.monotonic() + 300
    while time.monotonic() < deadline:
        try:
            request(base_url, "login")
            return
        except (OSError, urllib.error.URLError, urllib.error.HTTPError):
            time.sleep(2)
    raise RuntimeError("Timed out waiting for the disposable Mealie fixture")


def seed(base_url, username, password):
    wait_for_mealie(base_url)
    request(
        base_url,
        "api/users/register",
        method="POST",
        payload={
            "email": "syncwich-ci@example.invalid",
            "username": username,
            "fullName": "Syncwich CI",
            "password": password,
            "passwordConfirm": password,
            "group": "Syncwich CI",
            "household": "Syncwich Kitchen",
            "seedData": False,
        },
    )
    login = request(
        base_url,
        "api/auth/token",
        method="POST",
        payload={"username": username, "password": password},
        form=True,
    )
    token = login["access_token"]

    for index, name in enumerate(
        (
        "Gochujang Tofu Bowls",
        "Crispy Potato Breakfast Hash",
        "Lemon Herb Roast Vegetables",
        )
    ):
        request(base_url, "api/recipes", method="POST", payload={"name": name}, token=token)
        recipes = request(
            base_url, "api/recipes?page=1&perPage=50", token=token
        )["items"]
        recipe = next(item for item in recipes if item["name"] == name)
        multipart_request(
            base_url,
            f"api/recipes/{recipe['slug']}/image",
            token,
            {"extension": "png"},
            f"fixture-{index}.png",
            cover_png(index),
            "image/png",
        )

    return token


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--username", required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--github-env", required=True)
    args = parser.parse_args()
    token = seed(args.base_url, args.username, args.password)
    with open(args.github_env, "a", encoding="utf-8") as env_file:
        env_file.write(f"E2E_TOKEN={token}\n")


if __name__ == "__main__":
    main()
