# What's different in this fork

Lyrics for Poweramp searches LRCLIB and hands the result to Poweramp. This fork keeps all
of that and adds the offline half: searching the `.lrc` files that are already on the phone,
and jumping straight to the line you found.

## Offline lyrics library

Right of the two online search tabs there's a third one, **Offline**.

Pick the folder your music (and lyrics) live in. Everything under it is walked once, every
`.lrc` file is parsed, and the result is kept in a small cache file, so opening the screen
again is instant. A rescan only re-reads files whose timestamp changed, so a library of a
few thousand tracks doesn't have to be parsed from scratch every time.

Searching runs over the parsed lines, not the file names, so you can find a song from a
half remembered line in the middle of it. Every hit is shown with the line above and below
it for context, and the matched words are highlighted.

Each result carries its cover art, read straight out of the audio file's tags — no media
permission needed, since the folder grant already covers it, and decoded covers are cached so
scrolling doesn't re-read the files. The card itself is styled after the online result card
(same icon rows, same chip), so the two search modes don't look like different apps.

With the search box empty, the screen shows the whole indexed library instead — every `.lrc`
found, alphabetical, so you can browse it like a track list rather than having to remember a
line first.

Files: `helpers/LocalLyricsIndexer.kt`, `helpers/LocalArtLoader.kt`, `utils/LocalLyricsSearch.kt`,
`viewmodels/LocalLyricsViewModel.kt`, `ui/locallyrics/`.

## Download the lyrics that are still missing

The cloud-download icon in the offline screen's top bar walks the whole folder for audio files
that have no lyrics file next to them yet, searches LRCLIB for each one (using the track's own
tags, or the filename when it has none), and writes whatever it finds right beside the audio —
`.lrc` when synced lyrics exist, `.txt` for plain-only. A dialog tracks progress and can be
cancelled mid-run; either way the library re-scans itself afterward so new hits show up
immediately.

Files: `helpers/BulkLyricsDownloader.kt`, `helpers/LocalLyricsIndexer.kt`
(`findAudioWithoutLyrics`, `writeSiblingFile`).

## Play from the matched line in Poweramp

If an audio file with the same name sits next to the `.lrc` file, each result gets a play
button, and every timestamped line is tappable. Poweramp is opened with `OPEN_TO_PLAY` and
seeks to that line's timestamp, so playback starts exactly where the words you searched for
are sung. If Poweramp isn't installed the file is offered to any other player instead.

The seek position is sent both as `pos` in seconds (what the documented API reads) and as a
millisecond extra, since older Poweramp builds only honour the latter. Poweramp is then brought
to the front — the API command on its own starts playback in the background, which looks like
nothing happened.

Whether tapping a line seeks to that point or just opens the track from the beginning is a
toggle in the screen's overflow menu (⋮ → Playback → "Start from the matched line").

Files: `helpers/PowerampPlaybackHelper.kt`, `ui/locallyrics/LocalLyricsScreen.kt`.

## Searching only starts at three characters

One and two character queries used to fire a search and flood the list with everything that
happens to contain those letters. Both the online search and the offline one now wait for at
least three characters (`MIN_SEARCH_QUERY_LENGTH`).

## Multi-word searches are treated as a phrase

Typing more than one word used to match each word independently, anywhere: "kimseye söyleme"
would return songs with "kimseye" in the first verse and "söyleme" in the last chorus, which
is almost never what you wanted. Now the words have to follow each other, in the order you
typed them. A phrase is still allowed to continue on the next line, because lyric files wrap
lines at arbitrary points.

Turkish text is lowercased with `I → ı` and `İ → i` handled explicitly, so searching for
"ışık" or "İstanbul" behaves the way it should.

Files: `utils/LocalLyricsSearch.kt`, `utils/Utils.kt` (`matchesAsPhrase`).

## AMOLED theme and a choice of accent

Settings → Theme → **AMOLED** gives pure black surfaces instead of the dark grey ones.

Settings → Theme → **Accent Colour** picks what the highlights are painted with: green, blue,
purple, pink, orange, red, teal or amber, each with its own light and dark shade. "App default"
keeps the palette the app ships with — and on Android 12+ the wallpaper colours, which a picked
accent replaces. Only the accent roles change; black stays black on AMOLED.

Files: `ui/theme/Theme.kt`, `utils/Constants.kt`, `ui/settings/AppThemeSettings.kt`.
