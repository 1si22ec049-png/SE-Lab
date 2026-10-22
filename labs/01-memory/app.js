// This array is the app's temporary memory. Refreshing the page starts it empty.
const notes = [];

const form = document.querySelector("#note-form");
const input = document.querySelector("#note-input");
const list = document.querySelector("#note-list");
const emptyMessage = document.querySelector("#empty-message");

function renderNotes() {
  // Rebuild the visible list from the notes array whenever it changes.
  list.replaceChildren();
  emptyMessage.hidden = notes.length > 0;

  notes.forEach((note, index) => {
    const item = document.createElement("li");
    const text = document.createElement("span");
    const deleteButton = document.createElement("button");

    text.textContent = note;
    deleteButton.type = "button";
    deleteButton.textContent = "Delete";
    deleteButton.setAttribute("aria-label", `Delete note: ${note}`);
    deleteButton.addEventListener("click", () => {
      notes.splice(index, 1);
      renderNotes();
    });

    item.append(text, deleteButton);
    list.append(item);
  });
}

// The browser sends a submit event when the learner clicks the button or presses Enter.
form.addEventListener("submit", (event) => {
  event.preventDefault();

  const note = input.value.trim();
  if (!note) return;

  notes.push(note);
  input.value = "";
  renderNotes();
  input.focus();
});

renderNotes();
