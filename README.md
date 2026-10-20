# JagtFokus for Android

Danish hunting seasons by date and place. The iOS app in `../jagtapp` is the behaviour north star.

All v1 screens are built: Jagtbart, Journal, Alle arter, Indstillinger, Sol og vejr, species detail, the Schweiss register and the welcome screen. Kotlin, Jetpack Compose, Material 3. Danish and English. No account. No tracking.

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="$HOME/Library/Android/sdk"   # or the command-line SDK root
./gradlew :core:test :app:assembleDebug :app:lintDebug
```

See `AGENTS.md` for the rules.
