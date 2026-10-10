# Domino AI Pro — Android prototype

This is an offline manual-input domino strategy helper. It does not connect to or automate Higgs Games Island.

## Build with GitHub Actions
The workflow in `.github/workflows/android.yml` builds a debug APK and publishes it as a downloadable Actions artifact.

## Build locally
Requires Android Studio, JDK 17, and Android SDK 35.

This first version supports manually entering a domino hand and the two open ends, recommends a legal tile using a simple heuristic, and tracks win/loss counts for the current app session. It is not a trained AI and does not guarantee wins.
