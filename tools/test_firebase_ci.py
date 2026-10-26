import unittest
from firebase_ci_version import next_version
from firebase_ci_summary import tester_links, published_link


class FirebaseCiTest(unittest.TestCase):
    def test_new_push_increments_the_highest_published_version(self):
        self.assertEqual(149, next_version(147, [{"buildVersion": "146"}, {"buildVersion": "148"}]))

    def test_source_version_is_the_floor(self):
        self.assertEqual(150, next_version(150, [{"buildVersion": "146"}]))
        self.assertEqual(147, next_version(147, []))

    def test_non_numeric_release_metadata_does_not_reset_the_counter(self):
        self.assertEqual(149, next_version(147, [{"buildVersion": "preview"}, {"buildVersion": "148"}]))

    def test_cli_without_a_release_object_resolves_the_exact_published_build(self):
        old = "https://appdistribution.firebase.google.com/testerapps/app/releases/old"
        new = "https://appdistribution.firebase.google.com/testerapps/app/releases/new"
        releases = [{"buildVersion": "148", "testingUri": old}, {"buildVersion": "147", "testingUri": new}]
        self.assertEqual(new, published_link(releases, "147"))
        with self.assertRaises(StopIteration):
            published_link(releases, "149")

    def test_summary_exposes_only_the_stable_tester_link(self):
        link = "https://appdistribution.firebase.google.com/testerapps/app/releases/release"
        result = {"result": {"release": {"testingUri": link + "?utm_source=cli",
                    "binaryDownloadUri": "https://example.com/private?token=secret"}}}
        self.assertEqual([link], list(tester_links(result)))


if __name__ == "__main__":
    unittest.main()
