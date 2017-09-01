# Change Log


## ::1.0.0::

### Overview

* Forked from Sparse rss version 1.7, with `doits` patches.
* Migrated to modern Android development environment.
* Enhanced feed entry viewing.
* Overview enhancements.
* Bug fixes.

### Details

* Forked from Sparse rss version 1.7.
    * It was hosted on the now defunct Google Code site.
* Forked from `doits` (added patches)
    * Includes unreleased patches.
    * Choose different notification tones for different feeds
    * Disable notification of updates for selected feeds
* Migrated to modern Android development environment
    * Gradle based build.
    * Some API migrations.
* Migrated to modern Android development environment.
* Enhanced feed entry viewing.
    * Alt-text for images are shown (xkcd fix)
    * BBCode in the text entries now support *slightly* improved conversion to HTML.
    * Initial option and code to strip out known patterns of user-tracking pictures ("web bugs").
    * For entries that don't have a formal link, the link for the feed itself is used for the
        link button.
* Overview enhancements.
    * Made the "refresh" button first on the list.
* Bugs fixes:
    * Fixed a crash that occurred when a feed item had no text and no link.
    * Fixed issues with showing images embedded in the feed entry.  Some of the
      old conversions would not keep the URL correctly, or sometimes aggressively
      wrap URLs in links when they are already in links.
    * Preferences now have more descriptive names for the enable/disable options.
