# Notes App (se-lab)

A small beginner project made with plain HTML, CSS, and JavaScript. You can add notes, see them in a list, and delete them.

## Run the app

Open `index.html` in a web browser. There is no setup or server needed.

## How it works

The browser reads `index.html` to create the page, including the form, text box, and empty notes list. It also loads `app.js`, which finds those page elements and listens for browser events.

When you submit the form, the browser raises a `submit` event. JavaScript handles that event, reads the text box, puts the note into a JavaScript array, and redraws the visible list. Clicking a note's Delete button raises a `click` event; JavaScript removes that note from the array and redraws the list.

The array is the app's only storage. It exists only while the page is open in memory, so refreshing or closing the page clears all the notes.

## Try these experiments

1. Add a few notes, then delete one. Notice that only the selected note disappears.
2. In `app.js`, change `notes.push(note)` to `notes.unshift(note)`. Add two notes and see how the newest note now appears first.
3. In `index.html`, change the `background` or `color` in the `body` CSS rule, save the file, and reload the page to see your style change.
