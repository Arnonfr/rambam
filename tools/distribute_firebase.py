"""Distribute a reviewed APK and release notes through Firebase CLI.

Usage: rtk proxy python3 tools/distribute_firebase.py /path/build.apk /path/notes.txt
Uses the existing Firebase login. No browser or new credentials are needed.
"""
import argparse
import subprocess
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description="הפצת APK לבודקי Firebase ללא דפדפן")
    parser.add_argument("apk", type=Path)
    parser.add_argument("notes", type=Path)
    parser.add_argument("--app", default="1:703198161750:android:1e1d23970312aa68bbc4f7")
    parser.add_argument("--testers", default="arnon7700@gmail.com")
    args = parser.parse_args()
    if not args.apk.is_file() or not args.notes.is_file():
        parser.error("Both the APK and the release-notes file must exist")
    if args.apk.suffix.lower() != ".apk" or not args.notes.read_text().strip():
        parser.error("Expected an APK and nonempty release notes")
    command = ["rtk", "proxy", "firebase", "appdistribution:distribute", str(args.apk.resolve()),
        "--app", args.app, "--release-notes-file", str(args.notes.resolve()),
        "--testers", args.testers, "--non-interactive"]
    raise SystemExit(subprocess.call(command))


if __name__ == "__main__":
    main()
