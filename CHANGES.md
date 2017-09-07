# Change Log


## ::1.0.0::

### Overview

* Forked from Sparse rss version 1.7, with `doits` patches.
* Migrated to modern Android development environment.
* Enhanced feed entry viewing.
* Overview enhancements.
* Feed view enhancements.
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
    * As a result of this migration, the top 2 buttons
      ("Add Feed" and "Refresh") are now forced into the main overview
      overflow button drop-down.  Future versions will need to correct this,
      but the fix will require a major rewrite of the core view.
* Migrated to modern Android development environment.
* Enhanced feed entry viewing.
    * Alt-text for images are shown (xkcd fix)
    * BBCode in the text entries now support *slightly* improved conversion to HTML.
    * Initial option and code to strip out known patterns of user-tracking pictures ("web bugs").
    * For entries that don't have a formal link, the link for the feed itself is used for the
        link button.
    * Added a feed option to show a picture from the feed's entry link content whose
        URL matches a pattern.  This allows for easy viewing of entries that are usually just
        a blurb of text that want you to visit the page, and instead just show that one image
        from the page.  The implementation is still pretty raw, but allows for a much needed
        improvement to RSS feeds.  To find an image URL, visit the offending page, and select
        *view image in its own tab*.  From here, you can copy the URL for the image, paste
        it into the feed options, and edit it to create an appropriate Regular Expression.
        The scope of this is beyond this document.
* Overview enhancements.
    * Made the "refresh" button first on the list.
* Feed view enhancements.
    * Added "Edit" and "Additional settings" to the feed list view's menu.  It used
        to be that the only place to edit these settings was the unwieldy hold press
        context menu on the overview.  This should make the editing a bit more user
        friendly.
* Bugs fixes:
    * Fixed a crash that occurred when a feed item had no text and no link.
    * Fixed issues with showing images embedded in the feed entry.  Some of the
        old conversions would not keep the URL correctly, or sometimes aggressively
        wrap URLs in links when they are already in links.
    * Preferences now have more descriptive names for the enable/disable options.
