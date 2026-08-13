# Signal Android version

| Field | Value |
| --- | --- |
| Version name | **8.23.0** |
| Canonical version code | 1734 |
| Upstream tag | [v8.23.0](https://github.com/signalapp/Signal-Android/releases/tag/v8.23.0) |
| Previous project version | 8.22.2 (canonical 1733) |

## Highlights since 8.22.2

- Redesigned media editor for tablets and foldable devices
- Remove the audio track from a video before sending
- Emoji handling extracted into `:lib:emoji`
- libsignal-client 0.99.2 → 0.100.0
- Conversation settings split into group / individual / shared modules
- Recipient table migration `V325_AddBlockedAtToRecipientTable`

This tree matches official Signal Android **v8.23.0**, plus the project’s existing APK-size, resource-shrink, and Gradle IDE-sync customizations.

## Android Studio sync: kotlin-dsl 6.4.2 verification error

If sync fails with:

`Dependency verification failed ... org.gradle.kotlin.kotlin-dsl.gradle.plugin-6.4.2.pom`

1. Use the **Gradle wrapper** (9.4.1), not a manually selected 9.3.x.
2. Pull the latest `gradle/verification-metadata.xml` — it trusts official `org.gradle.kotlin` plugin artifacts so IDE sync is not blocked.
3. File → Invalidate Caches / Sync Project with Gradle Files.
