# Usage Instructions

No changes are required in Poweramp settings to use this plugin.

This app is a lyrics plugin, not a standalone lyrics app.
You cannot directly view lyrics in this plugin or select it as a **_Preferred Lyrics App_** in
Poweramp settings.

Poweramp automatically sends a lyrics request when lyrics are unavailable for a track.

## Automatic Lyrics Request from Poweramp

- When there are no lyrics available for a track to show, Poweramp makes an automatic background
  request with the track information.
- This request will be intercepted by the plugin and attempt to find lyrics from the internet and
  third-party lyrics providers.
- When Poweramp initiates a lyrics request, you will see the message “Searching lyrics via plugin”
  in the Poweramp lyrics UI.
- When the plugin receives a lyrics request, it will try to get the exact matching result from the
  server (i.e. metadata matches exactly).
- You can also enable [Fallback to Broader Search](#fall-back-to-broader-search) to perform an
  automatic fine search if the plugin fails to find the exact match.
- If the plugin can't find any lyrics during automatic search, you will need to perform a manual
  search.
- To initiate a manual search easily,
  enable [Notify on Lyrics Request Failure](#notify-on-lyrics-request-failure) to get a notification
  where you can easily start a manual search.

## Search Modes

The app offers two search options to perform a manual search:

* **Coarse Search**: Performs a keyword-based search that yields a broad range of results.

  This method prioritizes quantity over accuracy.

* **Fine Search**: Focuses on specific fields such as track title, artist name, and album name.

  It provides more relevant results but limits the number of matches returned.

## Lyrics Translation

You can translate lyrics using AI-powered translation in the lyrics editor.

To use this feature:

1. [Configure the API key](#translation-service-api-keys) in settings
2. Open lyrics editor for a track
3. Tap the translation button in the bottom bar
4. Select target language from drop-down menu
5. Choose whether to:
    - Replace original lyrics with translation
    - Keep both original and translation
6. Tap the translate button and wait for completion

**Notes:**

- Not all lyrics are translatable.
- Available target languages are determined by AI model.
- A missing API key configuration shows an error indicator.
- Network or other errors may cause translation failure.
- Once original lyrics are combined with translation, they cannot be easily separated without
  performing a new search.

## Edit Metadata

For Play Store builds, you can edit embedded song metadata (including lyrics) from the Library:

1. Open the Library screen and locate the track.
2. Tap the track to expand the additional actions panel.
3. Tap "Edit Metadata".
4. In the dialog, update fields (Title, Artist, Album, Lyrics, etc.) and save.

Note: This requires [Storage Access Permission](#selected-folders).

## App Settings

- ### Theme
  You can manually change the app's theme.

  Devices running Android 10+ can choose the system default mode to follow the system theme.

- ### Lyrics Request

    - #### Fall Back to Broader Search
      If the plugin
      cannot [find a best match](https://lrclib.net/docs#:~:text=Get%20lyrics%20with%20a%20track's%20signature),
      it will fall back to
      a [search method](https://lrclib.net/docs#:~:text=Search%20for%20lyrics%20records), which may
      occasionally retrieve incorrect lyrics.

    - #### Notify on Lyrics Request Failure
      If a lyrics request fails, the app posts a notification. From this notification, you can
      launch the plugin prepopulated with track metadata to perform a manual search and update the
      lyrics.

    - #### Replace Previous Notification
      Enabling this option replaces the previous notification with a new one, preventing the
      notification panel from being flooded with failure notifications.

      However, this means you can perform a manual search only for the most recent failed track.

    - #### Preferred Lyrics Type
      Choose whether the app should always try to send synced or plain lyrics to Poweramp.

      If the chosen type is not available for a track, the other type will be sent if available.

    - #### Mark Instrumental Songs
      There are non-vocal music tracks, also known as instrumental songs.

      If no lyrics are set for these tracks, Poweramp will prompt for lyrics every time you open the
      lyrics UI while playing them.

      This option will set dummy lyrics for instrumental tracks, preventing the lyrics request from
      appearing.

      This will send lyrics only to [Poweramp cache](#send-lyrics-to-poweramp).

- ### Lyrics Storage Settings
    - #### Send Lyrics to Poweramp
      [**Recommended to keep enabled always**]

      The result will be sent to Poweramp and cached in Poweramp's internal memory.

      Advantages:
        * The lyrics will immediately appear in the Poweramp lyrics UI.
        * You can launch the plugin with track information easily from the Poweramp lyrics UI by
          launching the plugin using the (») button.

      Disadvantages:
        * Poweramp won't include these lyrics in its backup.
        * Poweramp gives a lower priority to lyrics from the plugin and prefers lyrics from tags (
          embedded) or files.

    - #### Save Lyrics as Files
      The lyrics will be stored in the same location as the music files.

      Synced files will be stored as `.lrc` files and plain lyrics will be stored as `.txt` files in
      the same folder as music files.

      Advantages:
        * Lyrics will be preserved even if you perform a **full rescan** or **reinstall Poweramp**.
        * The lyrics may appear in other music players if they support displaying lyrics from files.

      Disadvantages:
        * There may be a delay in loading lyrics into the Poweramp lyrics UI. Sometimes, Poweramp
          requires a rescan or restart.
        * You can't launch the plugin easily from Poweramp lyrics UI to modify lyrics.
        * When you share music file to another device, lyrics file may not be shared.

    - #### Save ID Tags in LRC File
      This option requires [Save Lyrics as File](#save-lyrics-as-files) option enabled.
      This option saves song metadata within the `.lrc` file when lyrics are saved.

      Poweramp will not show these tags in the Lyrics UI, and this option doesn’t work for plain
      lyrics.

    - #### Embed Lyrics into File
      The lyrics will be embedded into the song metadata by modifying the track file.

      Advantages:
        * The lyrics always stay with the file, even during sharing to another device.
        * Music players that access only the audio file can show these lyrics.

      Disadvantages:
        * The file gets modified and its order may change in the `Recently Added` playlist in
          Poweramp Library.

    - #### Fix Song Metadata
      This option requires the [Embed Lyrics into File](#embed-lyrics-into-file) option to be
      enabled.

      The track metadata (Title, Artist, and Album Name) will be set from the lyrics metadata.

    - #### Selected Folders
      The plugin uses read and write access to these folders to manage lyrics files and track metadata. 
      
      Tips for Selecting Folders:

      * When choosing a music folder, instead of selecting individual subfolders, try to select their parent folder.
      * You cannot select the root directory of the storage due to Android security restrictions.

- ### Lyrics Editor
    - #### Translation Service API Keys
      You need to configure this field by entering API keys from your own account on the API
      provider's website. This allows the feature to remain free for everyone, as it avoids the need
      for me to pay for API usage and turn it into a paid feature. Your keys are stored only on your
      device.

    - #### API Providers
        - Google Gemini

      Get an API Key from here - https://aistudio.google.com/app/apikey.

- ### Filters
  This feature allows you to remove unwanted words or sentences from metadata while searching.

  You can specify filters for the title, artist, and album fields to remove matches from the
  corresponding search parameter.

  Filters can be plain text strings or regular expressions.

  For example, to exclude strings like `320kbps` or `128kbps` from track title, enter `\d{3}kbps` in
  the title filter box. This will remove any such matches from the title.

## Updating Lyrics Files on GitHub Version:

1. Delete the existing lyrics file from storage.
2. Play the track again in Poweramp with[Send to Poweramp](#send-lyrics-to-poweramp) enabled.

This will let you launch the plugin from the Poweramp lyrics UI again.

1. Before proceeding to search, make sure [Save Lyrics as Files](#save-lyrics-as-files) enabled.
2. Go to the plugin’s main screen and start a new search.
3. From the search results, select the lyrics you want.

## Updating Lyrics Files on Play Store Version:

The Play Store version includes a Library screen, allowing you to change lyrics anytime without
changing playback or launching Poweramp.

## :information_source: Notes

- Lyrics saved/sent to Poweramp by this plugin will be available offline.
- Lyrics availability is subjected to third-party resource.
- To reset or delete all lyrics set by this plugin in Poweramp, perform a full rescan
  `Poweramp Settings → Library → Full Rescan`

Need any help? You can [contact me](https://abhishekabhi789.github.io/#contact)
