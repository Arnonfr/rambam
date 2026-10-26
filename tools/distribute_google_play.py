#!/usr/bin/env python3
"""Build, verify and publish Baderech bundles to the existing Alpha testers."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

CODE = Path(__file__).resolve().parents[1]
WORKSPACE = CODE.parent
PRIVATE = WORKSPACE / "חתימה-פרטי"
PACKAGE = "com.aistudio.dailyrambam.kzjvw"
UPLOAD_SHA256 = "05:24:AA:BB:F9:10:AC:23:97:54:4F:41:9E:C2:3A:D6:A0:57:14:95:2E:D1:35:9F:5E:C8:F5:1F:91:50:31:5A"


def run(command, *, env=None, capture=False):
    return subprocess.run(command, cwd=CODE, env=env, check=True, text=True,
                          capture_output=capture)


def java_home():
    return Path(os.environ.get("JAVA_HOME", str(Path.home() / ".local/jdk21")))


def environment():
    env = os.environ.copy()
    env.update(FASTLANE_SKIP_UPDATE_CHECK="1", FASTLANE_HIDE_CHANGELOG="1")
    env.setdefault("JAVA_HOME", str(java_home()))
    env.setdefault("ANDROID_HOME", str(Path.home() / "Library/Android/sdk"))
    env.setdefault("GOOGLE_PLAY_JSON_KEY", str(PRIVATE / "google-play-service-account.json"))
    if not Path(env["GOOGLE_PLAY_JSON_KEY"]).is_file():
        raise ValueError("Google Play credential file is missing.")
    return env


def fastlane(lane, options, env):
    mise = shutil.which("mise") or str(Path.home() / ".local/bin/mise")
    run([mise, "exec", "--", "bundle", "exec", "fastlane", "android", lane, *options], env=env)


def build(env):
    signing = json.loads((PRIVATE / "signing-config.json").read_text())
    secret = run(["/usr/bin/security", "find-generic-password", "-s",
                  signing["keychain_service"], "-w"], capture=True).stdout.strip()
    env = dict(env, KEYSTORE_PATH=str(PRIVATE / signing["keystore"]),
               KEY_ALIAS=signing["key_alias"], STORE_PASSWORD=secret, KEY_PASSWORD=secret)
    gradle = shutil.which("gradle")
    if not gradle:
        candidates = sorted((Path.home() / ".gradle/wrapper/dists/gradle-9.3.1-bin").glob("*/gradle-9.3.1/bin/gradle"))
        if not candidates:
            raise ValueError("Gradle 9.3.1 is not installed.")
        gradle = str(candidates[0])
    run([gradle, "bundleRelease", "--no-configuration-cache", "--console=plain"], env=env)
    return CODE / "app/build/outputs/bundle/release/app-release.aab"


def verify(aab):
    java = java_home() / "bin"
    verified = run([str(java / "jarsigner"), "-verify", str(aab)], capture=True)
    if "jar verified." not in verified.stdout:
        raise ValueError("Bundle signature verification failed.")
    certificate = run([str(java / "keytool"), "-printcert", "-jarfile", str(aab)], capture=True).stdout
    if UPLOAD_SHA256 not in certificate.upper():
        raise ValueError("Bundle is not signed with the existing Google Play upload certificate.")
    bundletool = Path(os.environ.get("BUNDLETOOL_JAR", str(Path.home() / ".local/lib/bundletool.jar")))
    manifest = run([str(java / "java"), "-jar", str(bundletool), "dump", "manifest",
                    "--bundle=" + str(aab)], capture=True).stdout
    root = ET.fromstring(manifest)
    if root.attrib.get("package") != PACKAGE:
        raise ValueError("Bundle package does not match the Play app.")
    ns = "{http://schemas.android.com/apk/res/android}"
    app = root.find("application")
    if app is not None and app.attrib.get(ns + "debuggable") == "true":
        raise ValueError("Debuggable builds cannot be published.")
    run([str(java / "java"), "-jar", str(bundletool), "validate", "--bundle=" + str(aab)], capture=True)
    return {"package_name": PACKAGE, "version_code": int(root.attrib[ns + "versionCode"]),
            "version_name": root.attrib[ns + "versionName"], "upload_certificate_sha256": UPLOAD_SHA256,
            "sha256": hashlib.sha256(aab.read_bytes()).hexdigest(), "track": "alpha"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--status", action="store_true", help="Read Google Play tracks without publishing")
    parser.add_argument("--output", type=Path, help="Where to save track status")
    parser.add_argument("--aab", type=Path, help="Signed AAB; otherwise build using the existing macOS keychain credential")
    parser.add_argument("--notes", type=Path, help="UTF-8 release notes, up to 500 characters")
    parser.add_argument("--name", help="Release display name")
    parser.add_argument("--validate-only", action="store_true", help="Upload and validate an uncommitted Play edit")
    args = parser.parse_args()
    env = environment()
    if args.status:
        options = ["output:" + str(args.output.resolve())] if args.output else []
        fastlane("play_status", options, env)
        return
    if args.notes and not 1 <= len(args.notes.read_text(encoding="utf-8").strip()) <= 500:
        raise ValueError("Release notes must contain 1–500 characters.")
    aab = args.aab.resolve() if args.aab else build(env)
    info = verify(aab)
    if args.output:
        args.output.write_text(json.dumps(info, indent=2) + "\n")
    print(json.dumps(info, indent=2))
    options = ["aab:" + str(aab), "name:" + (args.name or f"{info['version_name']} ({info['version_code']})")]
    if args.notes:
        options.append("notes:" + str(args.notes.resolve()))
    if args.validate_only:
        options.append("validate_only:true")
    fastlane("play_testers", options, env)


if __name__ == "__main__":
    try:
        main()
    except (ValueError, subprocess.CalledProcessError, OSError) as error:
        print(f"Google Play distribution failed: {error}", file=sys.stderr)
        sys.exit(1)
