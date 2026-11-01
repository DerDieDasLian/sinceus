# Contributing to Since Us

Thanks for wanting to help! There are several ways to contribute, and most of them need no programming at all.

## Before you suggest an idea

Please read [What Since Us is (and what it isn't)](SCOPE.md) first. Since Us is an app for couples (poly ones too), and ideas that fit this focus have the best chance.

## Ways to help

- **Report a bug:** right in the app (Settings, About the app, Report a bug), or open an [issue](https://github.com/DerDieDasLian/sinceus/issues/new/choose) with the bug template. Your Android version, phone model and app version help a lot.
- **Suggest an idea:** use the idea template and tell us why it would help.
- **Accessibility:** if something is hard to use with TalkBack, large text or other settings, there is a template just for that.
- **Translate:** Since Us is translated on [Weblate](https://hosted.weblate.org/engage/since-us/). Please don't send translations as pull requests, Weblate takes care of that.
- **Test on Google Play:** see [Help test on Google Play](README.md#help-test-on-google-play).

## Pull requests

Small fixes are welcome. For bigger changes, please open an issue first, so we can talk about it before you put time into it.

When you change the app:

- Add new texts to both `app/src/main/res/values/strings.xml` (English) and `values-de/strings.xml` (German). Other languages come through Weblate.
- Add a short line about the change to the changelog for the next version in `fastlane/metadata/android/en-US/changelogs/` and `de-DE/changelogs/` (at most 500 characters per file).
- Run the tests: `./gradlew testGithubDebugUnitTest`
- Keep the app offline: no new internet access, no tracking, no new permissions without a good reason.
- No emojis in the app. Icons are drawn as vectors.

## Code of conduct

Everyone taking part is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
