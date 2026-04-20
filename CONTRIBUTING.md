# Testing in Krystal

- Testing in krystal is done in two layers
    - Module specific unit tests
    - End2End unit tests
- Module specific unit tests are present in the src/test directory of the module itself and are
  designed to give immediate feedback for locally testable features. Not all features - especially
  ones which are implemented across modules are testable in local unit tests.
- End2End unit tests are present in sample projects.
    - These tests are designed to tests complex features whose implementation spans multiple modules
    - All sample projects' names end with the "-sample" suffix (and all projects whose names end
      in "-sample" are sample projects).
    - These sample projects contain various krystal graphs which are tested in end to end fashion in
      the src/test of that sample project.
    - These sample projects also act as documentation for how use krystal and its various modules
      and extensions.
- All features implemented should preferably have test cases in both module specific unit tests and
  End2End unit tests.

# Testing on Local

1. After making changes to the framework,
   run [upgradeVersionAndPublishToLocal.macOS.sh](upgradeVersionAndPublishToLocal.macOS.sh) to
   publish the framework changes locally such that the new changes are available to the sample projects
2. Run `./gradlew test` to run all tests.