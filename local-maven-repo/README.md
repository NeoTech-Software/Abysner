# Temporary local koalaplot-core build

Abysner depends on Koala Plot, a Kotlin multi-platform chart library. However, a bug in the current
release (`koalaplot-core:0.12.1`) prevents charts from rendering in static previews and
screenshot tests.

A fix proposal was made here: https://github.com/KoalaPlot/koalaplot-core/pull/157

However until that fix is released, Abysner uses a temporary local build of Koala Plot to work
around the limitations of the current release.

This directory contains a stripped temporary local build of `koalaplot-core` based on commit
`7ad3144b` from PR #157 (as found in: https://github.com/KoalaPlot/koalaplot-core/pull/157). Sources
are included in `koalaplot-core-source-7ad3144b.tar.gz` for reference. This temporary solution can
be removed once the fix is released in an official version of Koala Plot.
