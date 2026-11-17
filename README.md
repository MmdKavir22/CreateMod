# Test Mod

A minimal Fabric test mod for **Minecraft 26.2**: one custom item (`testmod:test_item`, in the *Ingredients* creative tab), a `/testmod` command (prints mod + Minecraft version) and a startup log line.

## Requirements
- Minecraft Java Edition **26.2**
- Fabric Loader **0.20.1+**
- Fabric API **0.161.0+26.2** (or newer for 26.2)
- Java **25** (required by Minecraft 26.x)

## Installation
1. Install Fabric Loader for 26.2 from <https://fabricmc.net/use/>.
2. Put Fabric API and `testmod-<version>.jar` into `.minecraft/mods/`.
3. Launch the Fabric profile, run `/testmod`, find *Test Item* in the Ingredients tab.

## Build locally
```bash
./gradlew build        # JDK 25 required
```
The mod jar is `build/libs/testmod-<version>.jar` (ignore `-sources.jar`).

## GitHub Actions
`.github/workflows/build.yml` runs on every push to `main` and on manual **Run workflow**:
1. `build` job: JDK 25, Gradle cache, `./gradlew build`, uploads the jar as an artifact.
2. `release` job: creates tag + release `v<version>` titled `Test Mod v<version>` with the jar and auto-generated notes.

Permissions: `build` has `contents: read`; only `release` has `contents: write`, using the built-in `GITHUB_TOKEN` (no custom secrets).

## Releases
The version comes from `version=` in `gradle.properties`. If release `v<version>` already exists, the workflow **fails with a clear error** and never overwrites it.

Latest download: `https://github.com/<user>/<repo>/releases/latest`

## Publish a new version
1. Edit `version=1.0.1` in `gradle.properties`.
2. Commit and push to `main`. Tag `v1.0.1` and the release are created automatically.
