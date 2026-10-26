"""Write only the stable tester link to the run summary, never download tokens."""
import json
import os
from pathlib import Path
from urllib.request import Request, urlopen


def tester_links(value):
    if isinstance(value, dict):
        for child in value.values():
            yield from tester_links(child)
    elif isinstance(value, list):
        for child in value:
            yield from tester_links(child)
    elif isinstance(value, str) and value.startswith("https://appdistribution.firebase.google.com/testerapps/"):
        yield value.split("?")[0]


def published_link(releases, version):
    release = next(item for item in releases if str(item.get("buildVersion")) == str(version))
    return next(tester_links(release))


def main():
    result = json.loads(Path("build/firebase-distribution.json").read_text())
    if result.get("status") != "success":
        raise RuntimeError("Firebase did not confirm a successful distribution")
    url = next(tester_links(result), None)
    if url is None:
        # Firebase CLI 15 confirms success without returning the release object.
        # Fetch the exact published build instead of assuming JSON includes a URL.
        app = os.environ["FIREBASE_APP_ID"]
        project = app.split(":")[1]
        request = Request(
            f"https://firebaseappdistribution.googleapis.com/v1/projects/{project}/apps/{app}/releases?pageSize=25",
            headers={"Authorization": "Bearer " + os.environ["GOOGLE_ACCESS_TOKEN"]})
        with urlopen(request, timeout=60) as response:
            releases = json.load(response).get("releases", [])
        url = published_link(releases, os.environ["FIREBASE_BUILD_VERSION"])

    text = f"Firebase distribution completed.\n\n[Install the update]({url})\n"
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
        stream.write(text)
    print(text)


if __name__ == "__main__":
    main()
