# Contributing guide

**Want to contribute? Great!**
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples...
But first, read this page (including the small print at the end).

* [Legal](#legal)
* [Reporting an issue](#reporting-an-issue)
* [Building the project](#building-the-project)
* [Before you contribute](#before-you-contribute)
    + [Code reviews](#code-reviews)
    + [Continuous Integration](#continuous-integration)
    + [Tests and documentation are not optional](#tests-and-documentation-are-not-optional)
* [The small print](#the-small-print)

## Legal

All original contributions to SmallRye Stork are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues.
Open an issue directly in GitHub.

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.
Don't forget to indicate your Java and Maven/Gradle version.

If you use Quarkus, please also indicate the Quarkus version and, if the issue happens in native mode, the GraalVM version.

## Building the project

The project uses the _main_ branch for its development.

Before building the project, check that you have:

* Apache Maven 3.8+
* Java 11 (JDK)
* A functional _Docker_ environment

To build the project, run the following commands:

```
git clone https://github.com/smallrye/smallrye-stork.git
cd smallrye-stork
mvn install
```

Wait for a bit, and you're done.

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the same name is on all your computers.

We use this information to acknowledge your contributions to release announcements.

### Code reviews

All submissions, including submissions by project members, need to be reviewed before being merged.

### Coding Guidelines

* Commits should be atomic and semantic. Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process, but things should be squashed at the end to have meaningful commits.
  We use merge commits, so the GitHub Merge button cannot do that for us. If you don't know how to do that, just ask in your pull request; we will be happy to help!

### Continuous Integration

Because we are all humans, and to ensure Stork is stable for everyone, all changes must go through Stork's continuous integration. The CI is based on GitHub Actions, which means that everyone can automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process so that the contributor gets immediate feedback while at the same time keeping our CI fast and healthy for everyone.

The process requires only one additional step to enable Actions on your fork (clicking the green button in the Actions tab). [See the full video walkthrough](https://youtu.be/egqbx-Q-Cbg) for more details on how to do this.

To keep the caching of non-Stork artifacts efficient (speeding up CI), you should occasionally sync your fork's `main` branch with `main` of this repo (e.g., monthly).

### Tests and documentation are not optional

Don't forget to include tests in your pull requests.
Also, don't forget the documentation (reference documentation, JavaDoc...).

## The small print

This project is an open-source project; please act responsibly, be friendly, polite, and enjoy!
