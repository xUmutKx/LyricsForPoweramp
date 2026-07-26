# Usage Instructions

No changes are required in the Poweramp settings to use this plugin.

This app is a lyrics plugin, not a standalone lyrics app.
You cannot directly view lyrics in this plugin or select it as a **_Preferred Lyrics App_
** in Poweramp settings.

Poweramp automatically sends a lyrics request when lyrics are unavailable for a track.

## Lyrics Fetching
### Automatic Lyrics Fetching

- When there are no lyrics available for a track to display, Poweramp makes an automatic background request with the track information.
- This request will be intercepted by the plugin, which will attempt to find lyrics from the internet and third-party lyrics providers.
- When Poweramp initiates a lyrics request, you will see the message
  ***“Searching lyrics via plugin”*** in the Poweramp lyrics UI.
- When the plugin receives a lyrics request, it will try to get the exact matching result from the server (i.e. metadata matches exactly).
- You can also enable [Fallback to Broader Search](#fall-back-to-broader-search) to perform an automatic fine search if the plugin fails to find the exact match.
- **If the plugin cannot find any lyrics during an automatic search, you will need to perform a
  manual search**.
- To initiate a manual search easily, enable [Notify on Lyrics Request Failure](#notify-on-lyrics-request-failure) to get a notification where you can easily start a manual search.

### Manual Search

If the app fails to retrieve lyrics for a track, you can perform a manual search.

You can do manual search from following methods.

1. When automatic background search fails you will receive a notification, from which you can start a manual search. See [Lyrics Fetching](#lyrics-fetching) for more.
2. If Poweramp shows cached lyrics sent by this plugin, you can find a (») button in the top header that launches the plugin search screen with pre-populated search fields.
3. On the search screen(Main screen) of the plugin, tap on the Floating action button which will show you a search button labeled "Search for current track", clicking on it will populate the search field for now-playing track.
4. For the PlayStore variant, clicking on the library item will show a panel of additional actions which contains a search button, clicking on it will take you for a manual search.

### Manual Search Modes

The app offers two search options to perform a manual search:

* **Coarse Search**: Performs a keyword-based search that yields a broad range of results.

  This method prioritizes quantity over accuracy.

* **Fine Search**: Focuses on specific fields such as track title, artist name, and album name.

  It provides more relevant results but limits the number of matches returned.

## Lyrics Result Page

- For each result, the availability of synced lyrics or plain lyrics will be indicated with a chip.
- You can swipe on the lyrics or click on the chip to change the lyrics preview.
- Upon clicking on the lyrics area, the result item expands so you can preview the full lyrics.
- You can only edit or save lyrics to a track if the search was initiated within the context of a track (i.e., started from the Poweramp lyrics UI, a plugin notification, or the plugin library).Therefore, if you perform a random search, you will only see the results without the option to edit or save them.
- Both edit and save lyrics buttons will display a dialog allowing you to choose whether to proceed with synced lyrics or plain lyrics. Your preferred choice will be highlighted.
- An "Edit metadata" option is available alongside the edit/save options, allowing you to update or fix the track metadata(title, artist, album).
- On the topbar you can find the sort button and shortcut to launch settings.

## Edit Lyrics for a Track

- You can launch the editor from both the search screen and the result screen.
- When you open the lyrics editor, it will automatically attempt to read available saved lyrics. If needed, it will prompt you to grant folder access.
- If lyrics are available to edit, a dialog will appear at the top of the editor.
- If there are multiple sources available to load lyrics from, a popup dialog will prompt you to choose one.
- Note that there is no way to edit lyrics that have already been sent to the Poweramp cache.
- If no re-editable saving method is selected, you will be prompted to enable one.
- When editing `.lrc` lyrics content, any invalid timestamp will be highlighted in an error color.

### AI Lyrics Transformation

You can modify lyrics using supported AI models directly inside the lyrics editor.

To use this feature:

1. [Configure your API key](#api-key) in settings
2. Open lyrics editor for a track
3. Tap the AI Edit button(✨) in the bottom bar
4. Write down your prompt and click the generate button inside the text field

**Notes:**

- This feature is designed strictly for modifying lyrics; please avoid casual conversation with the model.
- There is no history support for the AI chat, so you must treat every prompt as if it were the first one.
- The existing lyrics content is passed along with each prompt. Adding multiple instructions increases the prompt length, which may exhaust your usage quota faster.
- For translations, you can instruct the AI to keep the original lyrics alongside the translation or replace the original lyrics entirely.
- By default, the AI model is instructed to replace the original lyrics. However, because some models may not strictly follow this direction, you may need to specify your preference clearly in the prompt.
- A missing API key configuration will display an error indicator.
- Network errors or other issues may cause the translation/transformation to fail.
- Some AI models may corrupt timestamps or lyrics lines; please manually verify the final output.
- If you have suggestions for the predefined models list in the app, feel free to let me know.

## Edit Metadata

For Play Store builds, you can edit embedded song metadata (including lyrics) from the Library:

1. Open the Library screen and locate the track.
2. Tap the track to expand the additional actions panel.
3. Tap "Edit Metadata".
4. Update the fields (Title, Artist, Album, Lyrics, etc.) in the dialog and save.

Note: This requires [Storage Access Permission](#selected-folders).

You can also edit metadata from lyrics search results.

Important: You need to refresh the library in Poweramp first, and then refresh the plugin library to see the updated metadata in the plugin library.

## App Settings

- ### Theme
  You can manually change the app's theme.

  Devices running Android 10+ can choose the system default mode to follow the system theme.

- ### Lyrics Request

    - #### Fall Back to Broader Search
      If the plugin cannot [find a best match](https://lrclib.net/docs#:~:text=Get%20lyrics%20with%20a%20track's%20signature), it will fall back to a [search method](https://lrclib.net/docs#:~:text=Search%20for%20lyrics%20records), which may occasionally retrieve incorrect lyrics.

    - #### Notify on Lyrics Request Failure
      If a lyrics request fails, the app posts a notification. From this notification, you can launch the plugin prepopulated with track metadata to perform a manual search and update the lyrics.

    - #### Replace Previous Notification
      Enabling this option replaces the previous notification with the newest one, preventing the notification panel from being flooded with failure alerts. However, this means you can only perform a manual search for the most recently failed track.

    - #### Preferred Lyrics Type
      Choose whether the app should always attempt to send synced or plain lyrics to Poweramp. If the preferred type is unavailable for a track, the alternative type will be sent if it exists.

    - #### Mark Instrumental Songs
      For non-vocal music tracks (instrumental songs) without lyrics, Poweramp will prompt for lyrics every time you open the lyrics UI during playback. Enabling this option sets dummy lyrics for instrumental tracks, preventing the prompt from appearing. This sends lyrics only to the [Poweramp cache](#send-lyrics-to-poweramp).

- ### Lyrics Storage Settings
    - #### Send Lyrics to Poweramp
      [**Recommended to keep enabled always**]
      The results will be sent to Poweramp and cached in Poweramp's internal memory.

      Advantages:
        * Lyrics appear immediately in the Poweramp lyrics UI.
        * You can easily launch the plugin with track information from the Poweramp lyrics UI using the (») button.

      Disadvantages:
        * Poweramp will not include these lyrics in its system backup.
        * Poweramp assigns lower priority to plugin lyrics, preferring lyrics from embedded tags or dedicated files.

    - #### Save Lyrics as Files
      Lyrics will be stored in the same folder as your music files. Synced lyrics are saved as `.lrc` files, and plain lyrics are saved as `.txt` files.

      Advantages:
        * Lyrics will be preserved even if you perform a **full rescan** or **reinstall Poweramp**.
        * The lyrics may appear in other music players if they support displaying lyrics from files.

      Disadvantages:
        * There may be a delay when loading lyrics into Poweramp; sometimes a library rescan or app restart is required.
        * You cannot easily launch the plugin from the Poweramp lyrics UI to modify lyrics.
        * When sharing a music file with another device, the lyrics file may not be automatically included.

    - #### Save ID Tags in LRC File
      This option requires [Save Lyrics as Files](#save-lyrics-as-files) to be enabled. It saves song metadata within the `.lrc` file itself. Poweramp will not display these tags in the Lyrics UI, and this option does not work for plain `.txt` lyrics.

    - #### Embed Lyrics into File
      Lyrics will be embedded directly into the song's metadata by modifying the track file.

      Advantages:
        * The lyrics always stay with the file, even when shared with another device.

      Disadvantages:
        * Modifying the file may alter its position/order in Poweramp's `Recently Added` playlist.

    - #### Fix Song Metadata
      This option requires the [Embed Lyrics into File](#embed-lyrics-into-file) option to be enabled.
      The track metadata (Title, Artist, and Album Name) will be set from the lyrics metadata.

    - #### Selected Folders
      The plugin automatically lists folders from Poweramp library data. 
      
      The plugin requires read and write access to these folders to manage lyrics files and track metadata.

      When you grant access to a parent folder, the plugin will automatically get access to all subfolders.

      Tips for Selecting Folders:
        * When choosing a music directory, try to select the parent folder rather than multiple individual subfolders.
        * Due to Android security restrictions, you cannot select the root directory of your storage.

- ### Lyrics Providers
    - #### Lrclib API URLs
      You can add custom Lrclib server URLs. The app will route subsequent requests to the specified servers.
    - #### Active Lrclib API
      This displays the primary API server currently handling all requests. Only one can be active at a time; secondary fallbacks are not supported.
    
      **IMPORTANT**: It's recommended to use Default API for faster and reliable lyrics search operations. 

- ### Lyrics Editor
    - #### AI Provider Settings

    - ##### API Key
      You need to configure this field by entering API keys from your own account on the API key provider's website.
      This allows the feature to remain free for everyone, as it avoids the need for me to pay for API usage and turn it into a paid feature.
      Your keys are stored only on your device.

    - ##### AI Model
      Select your preferred AI model here. You can choose a model from the predefined list or specify a custom identifier. Ensure that the model you choose supports text generation.

    - ##### Supported AI Providers
        - **Google Gemini**
            * Get an API Key: https://aistudio.google.com/app/apikey
            * Choose models: https://ai.google.dev/gemini-api/docs/models
        - **Open Router**
            * Get an API Key: https://openrouter.ai/
            * Choose models: https://openrouter.ai/models?output_modalities=text

- ### Filters
  This feature allows you to strip unwanted keywords or phrases from track metadata during searches. You can specify custom rules for the title, artist, and album fields.

  Filters support both plain text strings and regular expressions (Regex).

  For example, to exclude terms like `320kbps` or `128kbps` from a track title, enter `\d{3}kbps` in the title filter box to automatically scrub matching text before searching.

## Notes

- Lyrics saved/sent to Poweramp by this plugin will be available offline.
- Lyrics availability is subjected to third-party resource.
- To reset or delete all lyrics set by this plugin in Poweramp, perform a full rescan
  `Poweramp Settings → Library → Full Rescan`

Need any help? You can [contact me](https://abhishekabhi789.github.io/#contact)
