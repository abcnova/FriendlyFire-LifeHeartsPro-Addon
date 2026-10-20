# API compatibility matrix

This add-on uses the Bukkit API only and does not link Minecraft internals. `plugin.yml` declares 1.21 as the minimum API. The release JAR is built against Spigot API 1.21. The matrix compile-checks every published Paper API artifact and exact Spigot APIs for 1.21.2 and the 26.x lines. Paper has no API artifact for 1.21.2, so its Spigot API is checked directly. Paper 26.x checks use pinned latest stable builds to avoid resolving prerelease history.

Release 1.1.3 was rebuilt and passed the matrix on 2026-10-20.

| Minecraft API line | Result |
| --- | --- |
| Paper API artifacts for 1.21, 1.21.1, 1.21.3–1.21.11 | PASS |
| Spigot API 1.21.2 | PASS |
| Paper 26.1–26.1.2 via 26.1.2.build.74-stable | PASS |
| Paper 26.2 via 26.2.build.134-stable | PASS |
| Paper 26.3 via 26.3.build.207-stable | PASS |
| Spigot API 26.1, 26.1.1, 26.1.2, 26.2, 26.3 | PASS |
| Spigot API 1.21 (release artifact) | PASS |

These are API compile checks, not full live-server or multi-plugin gameplay smoke tests. Paper 1.21.x runs on Java 21; Paper 26.1+ requires Java 25.
