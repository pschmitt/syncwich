#!/usr/bin/env python3
"""Create a small, obviously fake Mealie account and recipe set for screenshots.

This talks only to the disposable Mealie container started by screenshots CI. It never reads
credentials from the repository or from a user's Mealie instance.
"""

import argparse
import json
import time
import urllib.error
import urllib.parse
import urllib.request


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

    for name in (
        "Gochujang Tofu Bowls",
        "Crispy Potato Breakfast Hash",
        "Lemon Herb Roast Vegetables",
    ):
        request(base_url, "api/recipes", method="POST", payload={"name": name}, token=token)

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
