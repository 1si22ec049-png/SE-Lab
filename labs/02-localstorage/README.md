# Notes App (se-lab)

A small beginner project made with plain HTML, CSS, and JavaScript. Add notes, view them in a list, and delete them.

## Run the app

For this lab, serve the project from a local HTTP server instead of opening the file directly:

1. Open a terminal in the project folder.
2. Start Python's built-in local server:

   ```text
   python -m http.server 8000
   ```

3. Open [http://localhost:8000](http://localhost:8000) in your browser.
4. To stop the server, return to the terminal and press Ctrl+C.

## How it works

The browser reads `index.html` to build the page and loads `app.js`. JavaScript listens for browser events: submitting the form adds the text to a JavaScript array, and clicking Delete removes it. After either change, the app turns the array into JSON text with `JSON.stringify()` and saves it in the browser using `localStorage.setItem()`.

When the page loads, JavaScript asks the browser for the saved text with `localStorage.getItem()` and turns it back into an array with `JSON.parse()`. If the saved text is invalid JSON or is not a list of text notes, the app warns in the browser console and starts with an empty list.

## Why notes survive a refresh

The JavaScript array only lives while the page is running. `localStorage` is browser-managed storage that keeps text data for a website after the page closes. Because the app saves the notes there and reads them again when it loads, refreshing or reopening the app restores them.

The browser keeps this data on your device, in its local profile, separately for each website origin (the protocol, host, and port). In this lab that origin is `http://localhost:8000`. Data belongs to that browser and profile; it is not sent to a server or automatically shared with other browsers or devices.

## Limitations

- `localStorage` stores strings, so the app converts the notes array to and from JSON.
- It has limited space and is intended for small amounts of data, not large files.
- Users can clear it through browser site data settings, and private browsing or browser settings may remove it.
- It is specific to a browser and website origin; it is not a backup or a cross-device sync service.
- This simple app handles damaged saved JSON by starting with an empty list, so invalid saved data cannot be recovered by the app.

## Try these experiments

1. Add a note and refresh the page. It should still be in the list.
2. Add a note, close the browser, then reopen it and visit [http://localhost:8000](http://localhost:8000). The note should still be there in the same browser profile.
3. Clear site data for `localhost` in your browser's site settings, then reload the app. The saved notes should be gone.
