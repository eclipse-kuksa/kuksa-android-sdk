## Releasing a version

Releasing a version is mostly automated. However since the CHANGELOG.md + version information are part of the git
history and need to be committed, some manual steps are still needed. These steps are also there to check for errors
which may have been made in the git history via
the [Conventionalcommits standard](https://www.conventionalcommits.org/en/v1.0.0/) format.

The NPM package [Commit-And-Tag](https://github.com/absolute-version/commit-and-tag-version) is used to streamline the
release process.

### Preparing a release

The following convenience NPM scripts are available for use here:

```
  "scripts": {
    "tag-release": "npx commit-and-tag-version --packageFiles version.txt -t release/v --skip.commit=true --skip.changelog=true --skip.bump=true",
    "commit-release": "npx commit-and-tag-version --packageFiles version.txt --bumpFiles version.txt --skip.tag=true", 
    "bump-release": "npx commit-and-tag-version --packageFiles version.txt --bumpFiles version.txt --skip.tag=true --skip.commit=true",
    "generate-changelog": "npx commit-and-tag-version --skip.bump=true --skip.commit=true --skip.tag=true"
  }
```

1) Execute `npm run commit-release` to prepare the release. This will automatically bump the version to the next correct
   semantic version, create a CHANGELOG.md file and make a commit.
2) Check the CHANGELOG.md for wrong entries which may have been committed in the history - We are human after all! :)
3) Push the commit, create a PR and wait for the merge.

### Deploying a release

1) Execute `npm run tag-release` on the main branch after the above merge.
2) Push the tag via 'git push origin feature-x'. "Origin" may differ if working with forks. The command should be shown
   in the terminal after step 1. A push of a release tag with the correct format will automatically trigger an official
   release GitHub workflow.
3) Check if the artifacts were uploaded successfully to the Nexus repository.
4) Close the uploaded staging repository and check the activity tab if all steps were successful.
