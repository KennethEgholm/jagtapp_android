# JagtFokus for Android

Danish hunting seasons by date and place. The iOS app in `../jagtapp` is the behaviour north star.

This repo is the first slice: project setup and the Jagtbart screen. Kotlin, Jetpack Compose, Material 3. No account. No tracking.

```sh
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export ANDROID_HOME="$HOME/Library/Android/sdk"   # or the command-line SDK root
./gradlew :core:test :app:assembleDebug
```

See `AGENTS.md` for the rules.
