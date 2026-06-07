# Contributing to Abysner

Thanks for your interest in contributing. Abysner is an open-source dive planner, and contributions
are welcome, but please read this first. Abysner has a specific scope and direction, and it is a
safety-relevant application, so the bar for changes is higher than for a typical app.

## Before you start

**Open an issue or a discussion before writing significant code.** This project has a clear scope,
and not every change fits it. A short conversation up front is the best way to make sure your time is
well spent, and to avoid a pull request that cannot be merged. Small, obvious fixes (typos, clear
bugs) are fine to send directly.

By contributing you agree to license your work under the project's
[AGPLv3 license](LICENSE), and you must sign the
[Contributor License Agreement (CLA)](cla.txt). The CLA is automated: when you open a pull request,
the [CLA Assistant](https://github.com/contributor-assistant/github-action) bot will prompt you to
sign if you have not already. A pull request cannot be merged until the CLA is signed.

## Reporting bugs

Good bug reports are especially valuable for a dive planner, where a difference of one minute can
matter. When the bug is about a dive plan or a calculation, please include enough detail to
reproduce it exactly:

- The full configuration: algorithm (for example ZHL-16C), gradient factors, salinity, altitude,
  ascent/descent rates, max ppO2, last deco stop, and unit system (metric or imperial).
- The dive itself: each section's depth and duration, the gas mixes, and the cylinders.
- What you expected, and what Abysner produced. If you can, compare against another planner and say
  which one and which version.

The reference plan tables in [docs/REFERENCE_PLANS.md](docs/REFERENCE_PLANS.md) are a good template
for how to present a plan clearly. The more your report looks like those, the faster it can be
checked.

For anything that is not a calculation bug, just describe the steps to reproduce, what you expected,
and what happened, along with your platform (Android or iOS) and app version.

## Setting up your environment

See [Building from source](README.md#building-from-source) for prerequisites and build commands, and
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for a tour of how the codebase is organized. If you are
working on the decompression engine, [docs/DECOMPRESSION.md](docs/DECOMPRESSION.md) explains it in
detail.

## Making changes

**Code style.** The project uses the official Kotlin code style. An [`.editorconfig`](.editorconfig)
is included and is respected by IntelliJ IDEA and Android Studio, so in most cases you do not need to
configure anything. Match the style of the surrounding code.

**Tests.** Changes to the `domain` module (the decompression engine, gas planning, physics) must be
covered by tests, and all tests must pass. The engine is pure Kotlin, so its tests run quickly
without any device or simulator:

```sh
# Run the domain (engine) tests
./gradlew :domain:jvmTest

# Run everything CI runs (tests, coverage, screenshot validation)
./gradlew :koverXmlReportDomain :koverXmlReportPresentation
```

If you change calculations, prefer adding or extending a reference-plan style test in
`DivePlannerTest`, so the new behavior is pinned down and stays correct.

**UI changes.** The UI has screenshot tests with reference images stored in Git LFS. If your change
affects the UI, the references may need updating. There is a dedicated GitHub Actions workflow
([`update-screenshots.yml`](.github/workflows/update-screenshots.yml)) for regenerating them.

**Commits.** Commit messages in this project follow a simple `Category: Short description` format.
The categories in use are:

| Category    | For                                            |
|-------------|------------------------------------------------|
| `Add:`      | New features or capabilities                   |
| `Fix:`      | Bug fixes                                       |
| `Refactor:` | Code changes that do not change behavior        |
| `Update:`   | Dependency or maintenance updates              |
| `Docs:`     | Documentation only                             |
| `CI:`       | Build, CI, or release tooling                   |
| `Release:`  | Version bumps (maintainer only)                 |

Keep each commit focused, and write the description in plain language (see the project's git history
for examples).

## Opening a pull request

- Base your work on `main` and open the pull request against `main`.
- Keep pull requests focused on a single change. Smaller is easier to review and more likely to be
  merged.
- Make sure the build and tests pass locally before opening it. CI will run the JVM tests, an Android
  build, and an iOS build.
- Sign the CLA when prompted.

## A note on AI

This is not a vibe-coded project. AI assistance is used where it helps (boilerplate, surfacing bugs,
exploring ideas), but every architectural decision is made deliberately, every algorithm is
validated against known references and diving standards, and every line of code is reviewed by a
person. Decompression is safety-relevant and must be deterministic, so correctness is never
delegated to a probabilistic tool. Contributions are held to the same standard: if you used AI to
help write something, that is fine, but you are responsible for understanding and verifying it.

Thanks again, and dive safe.
