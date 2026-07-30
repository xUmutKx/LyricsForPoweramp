# What's different in this fork

Lyrics for Poweramp searches LRCLIB and hands the result to Poweramp. This fork keeps all
of that and adds the offline half: searching the `.lrc` files that are already on the phone,
and jumping straight to the line you found.

## Offline lyrics library

Menu → **Offline Lyrics**.

Pick the folder your music (and lyrics) live in. Everything under it is walked once, every
`.lrc` file is parsed, and the result is kept in a small cache file, so opening the screen
again is instant. A rescan only re-reads files whose timestamp changed, so a library of a
few thousand tracks doesn't have to be parsed from scratch every time.

Searching runs over the parsed lines, not the file names, so you can find a song from a
half remembered line in the middle of it. Every hit is shown with the line above and below
it for context, and the matched words are highlighted.

Files: `helpers/LocalLyricsIndexer.kt`, `utils/LocalLyricsSearch.kt`,
`viewmodels/LocalLyricsViewModel.kt`, `ui/locallyrics/`.

## Play from the matched line in Poweramp

If an audio file with the same name sits next to the `.lrc` file, each result gets a play
button, and every timestamped line is tappable. Poweramp is opened with `OPEN_TO_PLAY` and
seeks to that line's timestamp, so playback starts exactly where the words you searched for
are sung. If Poweramp isn't installed the file is offered to any other player instead.

The seek position is sent both as `pos` in seconds (what the documented API reads) and as a
millisecond extra, since older Poweramp builds only honour the latter.

Files: `helpers/PowerampPlaybackHelper.kt`.

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

## AMOLED theme

Settings → Theme → **AMOLED**. Pure black surfaces instead of the dark grey ones, for OLED
screens.

Files: `ui/theme/Theme.kt`, `utils/Constants.kt`.
