const storageKey = "se-lab-notes";

function loadNotes() {
  const savedNotes = localStorage.getItem(storageKey);
  if (savedNotes === null) return [];

  let parsedNotes;
  try {
    parsedNotes = JSON.parse(savedNotes);
  } catch (error) {
    console.warn("Saved notes were not valid JSON; starting with an empty list.", error);
    return [];
  }

  if (!Array.isArray(parsedNotes) || !parsedNotes.every((note) => typeof note === "string")) {
    console.warn("Saved notes were not a list of text; starting with an empty list.");
    return [];
  }

  return parsedNotes;
}

const notes = loadNotes();

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
      saveNotes();
      renderNotes();
    });

    item.append(text, deleteButton);
    list.append(item);
  });
}

function saveNotes() {
  localStorage.setItem(storageKey, JSON.stringify(notes));
}

// The browser sends a submit event when the learner clicks the button or presses Enter.
form.addEventListener("submit", (event) => {
  event.preventDefault();

  const note = input.value.trim();
  if (!note) return;

  notes.push(note);
  saveNotes();
  input.value = "";
  renderNotes();
  input.focus();
});

renderNotes();
