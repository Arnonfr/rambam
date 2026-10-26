"""Choose an increasing Firebase version and prepare CI release notes.

Credentials arrive only through an ephemeral environment variable from GitHub OIDC.
A failed lookup aborts the build instead of publishing a stale version number.
"""
import json
import os
from pathlib import Path
import re
from urllib.request import Request, urlopen

ROOT = Path(__file__).resolve().parents[1]


def next_version(base, releases):
    versions = [int(release["buildVersion"]) for release in releases
                if str(release.get("buildVersion", "")).isdigit()]
    return max(base, max(versions, default=0) + 1)


def main():
    app = os.environ["FIREBASE_APP_ID"]
    project = app.split(":")[1]
    request = Request(
        f"https://firebaseappdistribution.googleapis.com/v1/projects/{project}/apps/{app}/releases?pageSize=25",
        headers={"Authorization": "Bearer " + os.environ["GOOGLE_ACCESS_TOKEN"]})
    with urlopen(request, timeout=60) as response:
        releases = json.load(response).get("releases", [])
    gradle = ROOT / "app/build.gradle.kts"
    source = gradle.read_text()
    base = int(re.search(r"versionCode = (\d+)", source).group(1))
    version = next_version(base, releases)
    gradle.write_text(re.sub(r"versionCode = \d+", f"versionCode = {version}", source, count=1))
    notes = max((ROOT / "docs").glob("release-notes-*.txt"),
                key=lambda path: int(path.stem.rsplit("-", 1)[1]))
    output = ROOT / "build/firebase-release-notes.txt"
    output.parent.mkdir(exist_ok=True)
    output.write_text(f"גרסה {version}\n" + notes.read_text() +
                      f"\nGit: {os.environ.get('GITHUB_SHA', '')[:12]}\n")
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        stream.write(f"version={version}\n")
    print(f"Firebase build version: {version}")


if __name__ == "__main__":
    main()
