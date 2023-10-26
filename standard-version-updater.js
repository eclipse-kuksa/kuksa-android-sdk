// standard-version-updater.js
const stringifyPackage = require('stringify-package')
const detectIndent = require('detect-indent')
const detectNewline = require('detect-newline')

module.exports.readVersion = function (contents) {
  return JSON.parse(contents).tracker.package.version;
}

module.exports.writeVersion = function (contents, version) {
  const versions = version.split(".")
  const major = versions[0]
  const minor = versions[1]
  const patch = versions[2]

  return `MAJOR=${major}
  MINOR=${minor}
  PATCH=${PATCH}`
}
