"""Write only the stable tester link to the run summary, never download tokens."""
import json
import os
from pathlib import Path


def tester_links(value):
    if isinstance(value, dict):
        for child in value.values():
            yield from tester_links(child)
    elif isinstance(value, list):
        for child in value:
            yield from tester_links(child)
    elif isinstance(value, str) and value.startswith("https://appdistribution.firebase.google.com/testerapps/"):
        yield value.split("?")[0]


def main():
    result = json.loads(Path("build/firebase-distribution.json").read_text())
    if result.get("status") != "success":
        raise RuntimeError("Firebase did not confirm a successful distribution")
    url = next(tester_links(result))
    text = f"Firebase distribution completed.\n\n[Install the update]({url})\n"
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
        stream.write(text)
    print(text)


if __name__ == "__main__":
    main()
