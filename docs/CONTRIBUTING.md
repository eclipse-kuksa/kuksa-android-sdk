## Getting started

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
