# Rune brand assets

Mark: the Elder Futhark rune Raidō (ᚱ) — "journey", the R of Rune.
Ochre #A63F24 on Bone #F3F0E8 (light), Ember #E3805C on Granite #1E2124 (dark).
In the dark UI, links and buttons use the lighter Ember #EC9473: it keeps 4.5:1 (WCAG AA) also on Slate #3A3F44 surfaces, where #E3805C has 3.8:1. The marks and icons keep #E3805C.

brand/   Master SVGs (mark, glyph, lockups) and 1024 px PNGs.
         rune-glyph.svg uses currentColor for inline UI use.
web/     Drop-in PWA set: favicon.svg (follows light/dark), favicon.ico,
         apple-touch-icon.png, 192/512 icons, maskable and monochrome icons,
         manifest.webmanifest, head-snippet.html, rune-theme.css.
mobile/  Native app sources for later:
         ios/AppIcon-1024.png      App Store icon (opaque, square; iOS rounds it)
         ios/icon-dark-foreground  iOS 18+ dark icon glyph (place on transparent)
         android/adaptive-*        108dp adaptive icon foreground + monochrome;
                                   background colour #F3F0E8

Fonts: Space Grotesk (headings) and Inter (body), both OFL. Self-host them
(e.g. @fontsource/inter, @fontsource/space-grotesk) rather than loading
Google Fonts, so the PWA works offline and makes no third-party requests.
