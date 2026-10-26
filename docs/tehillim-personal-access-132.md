# Personal Psalm access — 132

The blue shape in the daily Tehillim reader was the personal-Psalm shortcut. It used DeepNavy as its Surface while rendering the light reader’s dark foreground text. It is now an outlined button with an explicit light/sepia/dark surface and matching text, labelled “הפרקים שלי”. Its action remains available with an empty personal list, opening the add/manage screen.

Home RTL order is now personal Psalms, resume (when applicable), daily study. Personal remains conditional on at least one configured entry. Resume visibility still follows the existing saved/unfinished-bookmark logic.

Firebase uses com.aistudio.dailyrambam.kzjvw.debug; Google Play uses com.aistudio.dailyrambam.kzjvw. Android app storage, including personal_tehillim DataStore, is separate. Entries created in the Firebase app therefore do not automatically appear in the Play app. No personal information is bundled, exported, or transferred by this UI fix.

The same targeted UI checks are run against debug and release code. Release Robolectric tests register only their in-memory Compose host activity; no test activity is added to the release app. AGP 9 requires -Pandroid.onlyEnableUnitTestForTheTestedBuildType=false to expose release unit tests ([official release notes](https://developer.android.com/build/releases/agp-9-0-0-release-notes)).

Validation: 9 focused debug checks and 13 release checks passed, including personal entry creation and continuous reading in release. White/dark/sepia shortcut screenshots were reviewed; light background is white and sepia uses its matching paper color. Debug APK built as 132.
