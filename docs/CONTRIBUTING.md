## Getting started

### Developer Setup & Building the Project

When cloning the repository on a fresh setup and running `./gradlew clean build`, Gradle builds both debug and release variants (including `:app:packageRelease` and `:app:validateSigningRelease`). This requires configuring your Android SDK path and signing credentials.

#### 1. Android SDK Location
Set your Android SDK path either via environment variable or in `local.properties`:

- **Environment variable:**
  ```bash
  # macOS / Linux
  export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk" # or export ANDROID_HOME="$HOME/Library/Android/sdk"
  ```
- **Or in `local.properties` (project root):**
  ```properties
  sdk.dir=/Users/<username>/Library/Android/sdk
  ```

#### 2. Signing Configuration for Local Development
The release build requires signing credentials. For local development, you can use Android's default `debug.keystore` (generated automatically by Android Studio in `~/.android/debug.keystore`).

You can configure this using either **`local.properties`** (recommended for local dev) or **environment variables** (used in CI):

**Option A: Using `local.properties` (Recommended for local dev)**
Add the following to `local.properties` in your repository root:
```properties
release.keystore.path=/Users/<username>/.android/debug.keystore
release.keystore.key.alias=androiddebugkey
release.keystore.key.password=<key-password>    # for debug.keystore: android
release.keystore.store.password=<store-password>  # for debug.keystore: android
```

> [!NOTE]
> If no release keystore is configured, `./gradlew build` automatically falls back to debug signing so the project builds out of the box.

**Option B: Using Environment Variables**
Alternatively, export the following variables in your terminal session or shell profile (`~/.zshrc`, `~/.bashrc`):
```bash
export KEYSTORE_PATH="$HOME/.android/debug.keystore"
export SIGNING_KEY_ALIAS="androiddebugkey"
export SIGNING_KEY_PASSWORD="android"
export SIGNING_STORE_PASSWORD="android"
```

Once configured, verify by running:
```bash
./gradlew :app:validateSigningRelease
```

---

### Make Changes

### Commit Messages

We are using the [Conventionalcommits standard](https://www.conventionalcommits.org/en/v1.0.0/). 

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

Example: 
```
fix(TestApp): Fix crash on startup without permissions

If the user declines the location permissions popup after a fresh 
install then the initial user location was null. This was never 
validated.

- Implemented an early return for this case

Closes: #123
```

The following types are supported:
```
    "types": [
      {"type": "feature", "section": "Features"},
      {"type": "fix", "section": "Bug Fixes"},
      {"type": "docs", "section": "Documentation"},
      {"type": "perf", "section": "Performance"},
      {"type": "refactor", "section": "Refactoring"},
      {"type": "revert", "section": "Reverts"},
      {"type": "test", "section": "Tests"},
      {"type": "chore", "hidden": true},
      {"type": "style", "hidden": true},
      {"type": "revert", "hidden": true}
    ]
```
This means that every commit which does not use the types: "chore" or "style" will generate a changelog entry. It is upon the developer to decide which commits should be visible here. Most of the time you will want to generate one entry for every issue. But there is no strict rule. Orientate yourself to the previous commit history to get a clear picture. 

If you want to ensure that your pushed commits messages are in the correct format, before the CI build is rejecting your PR, you can enable the automatic [commit linter](https://commitlint.js.org/#/) locally. Execute the following commands:


```
// Dependencies
brew install yarn
brew install node

yarn install --frozen-lockfile
```

### Pull Requests

The following branch naming convention should be used:

- feature-1
- bugfix-2
- task-3

Every PR should be linked to at least one issue. If at least one commit message footer has the `Close #1` information then this is automatically done.

### Coding Conventions

The project uses some common code style and code quality standards. Use `./gradlew check` to run these manually. Every PR validation build will check for new warnings and will reject the PR if new issues would be introduced. Pre-Push hooks are also automatically configured via Yarn. 

#### [Detekt](https://detekt.dev) 

Used for detecting static code quality issues. It is recommended to install the [Android Studio Plugin](https://plugins.jetbrains.com/plugin/10761-detekt) for editor support.

#### [ktlint](https://pinterest.github.io/ktlint)

Used for detecting static code style issues. 

### Fail-Early-Builds

We have multiple Fail-Early-Builds which run different versions of the KUKSA Android SDK against the KUKSA Databroker. 
Our goal is to have an early indication which allows us more easily to find breaking or behavioral changes when running our SDK on a specific version of the Databroker. 

When one of these builds fail a short validity check should be done:
- Were the correct versions of the SDK and Databroker used? 
- Is it a sporadically failing test? Does a re-run fixes it? 
- Is apparent if the issue is inside the SDK (e.g. wrongly written test, bug) or inside the Databroker (e.g. behavioral change, bug)?

Using different versions of the SDK and Databroker give different indications with varying importance. See the following list:

**SDK:latestRelease -> Databroker:latestRelease**
This means that issues exist between the latest released version of the SDK and the latest released version of the Databroker.

If this build fails it should be considered as a potential bigger issue  
=> A hotfix or new release needs to be done

**SDK:latestRelease -> Databroker:main**
This means, that the latest released version of the SDK is not compatible with the currently developed version of the Databroker.

If this build fails it should be considered as a warning
=> Required fixes should be part of the next SDK release

**SDK:main -> Databroker:main**

[![SDK:main <-> Databroker:master](https://github.com/eclipse-kuksa/kuksa-android-sdk/actions/workflows/daily_integration_main.yaml/badge.svg)](https://github.com/eclipse-kuksa/kuksa-android-sdk/actions/workflows/daily_integration_main-master.yaml?query=branch%3Amain)

This means both the SDK and Databroker are running in a kind of "bleeding edge" state in their currently developed version.

If this build fails, it should be considered as a warning. 
=> It is okay for the pipeline to fail for a short period of time. Longer periods should be avoided.
=> No explicit release / hotfix required, both components should be compatible before a release

