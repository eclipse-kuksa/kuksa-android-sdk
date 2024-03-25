# Changelog

All notable changes to this project will be documented in this file. See [commit-and-tag-version](https://github.com/absolute-version/commit-and-tag-version) for commit guidelines.

## [0.2.0](https://github.com/eclipse-kuksa/kuksa-android-sdk/compare/release/release/v0.1.3...release/v0.2.0) (2024-03-25)


### ⚠ BREAKING CHANGES

* The API for fetch / subscribe / update did change.
The input for these methods are now wrapped via "DataBrokerRequest"
classes e.g. "FetchRequest", "UpdateRequest". For generated VSS models
the classes "VssNodeFetchRequest" etc. should be used.

- The Property model was removed in favor of DataBrokerRequest
* - A new Gradle plugin(VssProcessorPlugin) was introduced to improve
the input (VSS files) handling of the VSS KSP generation. This plugin
is now mandatory if the VssProcessor KSP module is used in the
project.
* - Method PropertyListener#onPropertyChanged(String, Field, DataEntry) was removed
- Method PropertyListener#onPropertyChanged(List<EntryUpdate>) was added
- Previous DataEntry can be retrieved via EntryUpdate#entry
- Subscribing to a branch will initially send out an update containing a list of all leafs

* Fix buildDir for javadocJar task ([49729c4](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/49729c40790ce324cb6756e43e10d6a2a9d4fa53))
* Improve PropertyListener for Wildcard Subscribes ([bb92476](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/bb9247692ee021c439f076cb80fd955f66ce9a8e))
* Replace Property with DataBrokerRequests ([ea1054d](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/ea1054d406e83af45abad28bcc4539e8abe060a6))


### Features

* **Build:** Add a fixed Databroker version for PR builds ([7d307d5](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/7d307d5d5dcb94c71467fb26a102a8d939412bf8))
* **Build:** Add Code Coverage Report ([1a3e8fe](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/1a3e8fe2d1fefcf0cc845252b1d041ae62e5e38f))
* **Build:** Add Automation for Github Releases Pages ([1add64b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/1add64b669b2b562b0a5c0ac6280c455f52a1ad9)), closes [#42](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/42)
* **Build:** Add Github Action to test SDK:main -> Databroker:master ([b5ea73b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/b5ea73b1c6bb7342e315de3d047d8cb3e66145ed))
* **Build:** Make "run-tests" able to run Custom Sets of Tests ([31ff2cf](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/31ff2cfc5fd5974c3bdcce28bbb29060f809dcce))
* **SDK:** Remove "Custom Wildcard Subscription"-Logic for Specs ([2632b4d](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/2632b4d8a13e2a9ab8a009e1f033164dede0a9ff)), closes [#60](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/60)
* **SDK:** Add Support for Authentication ([1d4ff54](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/1d4ff54278e6d4ede864480b389cec1c17db1766)), closes [#76](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/76)
* **TestApp:** Add Suggestions for VSS Path ([9221f0b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/9221f0b5def9ff67eb92a87a54eb0bd23e380e91)), closes [#16](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/16)
* **TestApp:** Add TestApp Support for Authentication ([7a40e92](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/7a40e924a9d66b0621e4e312418ac4233c7e538a))
* **TestApp:** Remove Field.METADATA from "casual" queries ([fac8d85](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/fac8d85a8e935e41063abcd0bd98787586d99929)), closes [#24](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/24)
* **TestApp:** Slimline ConnectedView ([77fbd4a](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/77fbd4a4af7f7fe213b4900e4513100993f3923f))
* **TestApp:** Add possibility to test the update of generated VSS Models ([44bb3a5](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/44bb3a5cad9dd7e6fabeb96b424acb0d739f29a9)), closes [#105](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/105)
* **VssProcessor** Add VSS Processor Plugin ([aa3d2d2](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/aa3d2d2b73759461ef4118454264fe4c95946e07)), closes [#45](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/45)
* **VssProcessor:** Add dataType to VssSignal ([461eda0](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/461eda0f7d4ee066e4bcf52be9551e522d25aff3))
* **VssProcessor:** Add Support for unit, min and max attributes of Specification ([49e1b8f](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/49e1b8fdb042d24ec19cf1a00b1a89188e8dd7d9))
* **VssProcessor:** Add JSON Parser to VSS Processor module ([f4a72a2](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/f4a72a2101c265534f6fc0312ab877579108ebe7))


### Bug Fixes

* **SDK:** Fix parent class of generated VSS specifications ([f481f64](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/f481f6459ef3655e3541096b22228680bd726767))
* **SDK:** Fix UInt not supported for VssNodes ([07e9f04](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/07e9f0482e15b402936482116a8d71a25ee536bf)), closes [#93](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/93)
* **TestApp:** Fix ConnectionInfo DataStore initialised again ([30927ef](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/30927ef0dd29d1fd8b4dd20b82faefd902ba667f)), closes [#81](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/81)


### Documentation

* Add Documentation about Fail-Early-Builds ([8eeebab](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/8eeebab0eb3a770825631d19577fc42a48118479))
* Add Documentation for Authentication ([ddcd0b6](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/ddcd0b67ec0e90c48a64168a8863a473f7cc27f8))
* Add VSS Processor Plugin docs ([aad0a0c](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/aad0a0cf29b0854c90577939eda51fd95adea3a7))


### Refactoring

* **SDK:** Align Terminology of VSS model classes / public API ([5748884](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/57488844ddfbd33ea93d58521252598716cde511)), closes [#70](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/70)

## [0.1.3](https://github.com/eclipse-kuksa/kuksa-android-sdk/compare/release/release/v0.1.2...release/v0.1.3) (2024-01-22)


### Bug Fixes

* VSS Specification Generation fails for specific Folder Structure ([9f9f285](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/9f9f28561dafe2e32fce5fb8be72d5882e04b035))

## [0.1.2](https://github.com/eclipse-kuksa/kuksa-android-sdk/compare/release/release/v0.1.1...release/v0.1.2) (2023-12-04)


### Features

* Publish to OSSRH / MavenCentral ([4294f7c](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/4294f7cf1754695489b7360fbc610bc1da4caeea)), closes [#46](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/46)
* Replace "Check-Dash" GitHub Action with Kuksa Shared Action ([f7a3d92](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/f7a3d926240cfabe392eec33d6266873079e4337)), closes [#37](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/37)


### Bug Fixes

* Fix exception on empty VssDefinition asset file ([5153077](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/5153077d7331fd9f808cf4fa69cf5b74e8508833)), closes [#43](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/43)


### Documentation

* Add Overview to README ([0fbb0fc](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/0fbb0fc52afa895f9ecb651e18591408b7c1a631))
* Add Quickstart guide ([9fabe34](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/9fabe3420db149b0c35823086734ba020d5c122c))
* Replace GitHub Packages with Maven Central ([895e65e](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/895e65ed901ebe9ca36952af64c1a19c7bc62ce8))

## [0.1.1](https://github.com/eclipse-kuksa/kuksa-android-sdk/compare/release/v0.1.0...release/v0.1.1) (2023-11-14)


### Bug Fixes

* Fix missing upload of sub project packages ([96d28e8](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/96d28e896233ec957a04919c61b86afa44a7c865)), closes [#35](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/35)

## 0.1.0 (2023-11-09)


### Features

* Add Centralized Versioning ([d7a946c](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/d7a946ccaaef9898e3d02a9b3f879e897ac1df0f))
* Add Unsubscribe API to SDK ([69c6294](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/69c62943780046316e4c04b91a063cef63679d0d)), closes [#18](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/18) [#20](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/20)
* Add update API for generated specifications ([9f150e8](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/9f150e801929c8836290c6db9dd76bee37d12340)), closes [#23](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/23)
* Add VSS Symbol Processing processor ([18926ee](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/18926ee0cb15f534855e9dd646c1a30e2542719c))
* Correctly trigger Actuator Target Events ([3c2428b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/3c2428b0efa73d75aee00b4632011b6f7caf7de3)), closes [#29](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/29)
* Disable Obfuscation ([0d66d97](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/0d66d97c398531fe9429e0f0d19285dbea3eaf1d)), closes [#5](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/5)
* Generate Dash License Report ([5fdc0d1](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/5fdc0d14a39b2259cfc13ae093a3ffb01e622edb)), closes [#2](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/2)
* Limit Maximum Number of Logs ([07a808a](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/07a808aad2d8a5dcfb4aa51e1efe67e3826633e2)), closes [#19](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/19)
* Make Certificate Selectable in TestApp ([34a4b4c](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/34a4b4c250d3b8da53ff912578e74e93b246efb8)), closes [#15](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/15)
* Notify about DataBroker Disconnect ([47e378b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/47e378bdacbe37e2c4b497a30872418244bc3537)), closes [#12](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/12)
* Publish Kuksa SDK Snapshot Builds to MavenLocal ([eb63872](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/eb63872da13e929b9664dc7904cc743ff0123867)), closes [#6](https://github.com/eclipse-kuksa/kuksa-android-sdk/issues/6)
* Use commit-and-tag-version for Release Management ([ea0866a](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/ea0866a2ec5df4db793cb932e5a5f2b516d9309b))


### Documentation

* Adapt Architecture Documentation ([0fbb6ab](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/0fbb6ab2af1b08156860f7cdb74650db458d50ef))
* Add class diagram for vss-core ([d00dc8e](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/d00dc8e79602bcbb0716804b0b9d7468dcccd022))
* Add class diagram for vss-processor ([a22b083](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/a22b083f010899a63acb19e03bf04d30603c6590))
* Add release HowTo docs ([b51059b](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/b51059bf0c5806e32ccd9ddcc1ea85cb7c91ef97))
* Improve documentation ([61b34cc](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/61b34cc903a9ecaff1c84b5451d14340ce2daaaa))


### Performance

* Apply build analyser recommendations ([2a108c3](https://github.com/eclipse-kuksa/kuksa-android-sdk/commit/2a108c3bf0dbe9688d781f2f199c8deaba2cd1fd))
