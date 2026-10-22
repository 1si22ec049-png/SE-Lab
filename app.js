const apiUrl = "http://localhost:8080/api/notes";
const notes = [];

const form = document.querySelector("#note-form");
const input = document.querySelector("#note-input");
const list = document.querySelector("#note-list");
const emptyMessage = document.querySelector("#empty-message");
const errorMessage = document.querySelector("#error-message");

function showError(action, error) {
  errorMessage.textContent = `${action}: ${error.message}`;
  errorMessage.hidden = false;
}

function clearError() {
  errorMessage.textContent = "";
  errorMessage.hidden = true;
}

async function loadNotes() {
  try {
    const response = await fetch(apiUrl);
    if (!response.ok) throw new Error(`server returned HTTP ${response.status}`);

    const savedNotes = await response.json();
    notes.push(...savedNotes);
    renderNotes();
  } catch (error) {
    showError("Could not load notes. Is the backend running?", error);
  }
}

function renderNotes() {
  // Rebuild the visible list from the notes array whenever it changes.
  list.replaceChildren();
  emptyMessage.hidden = notes.length > 0;

  notes.forEach((note) => {
    const item = document.createElement("li");
    const text = document.createElement("span");
    const deleteButton = document.createElement("button");

    text.textContent = note.text;
    deleteButton.type = "button";
    deleteButton.textContent = "Delete";
    deleteButton.setAttribute("aria-label", `Delete note: ${note.text}`);
    deleteButton.addEventListener("click", async () => {
      clearError();
      try {
        const response = await fetch(`${apiUrl}/${note.id}`, { method: "DELETE" });
        if (!response.ok) throw new Error(`server returned HTTP ${response.status}`);

        const index = notes.findIndex((savedNote) => savedNote.id === note.id);
        if (index !== -1) notes.splice(index, 1);
        renderNotes();
      } catch (error) {
        showError("Could not delete note", error);
      }
    });

    item.append(text, deleteButton);
    list.append(item);
  });
}

form.addEventListener("submit", (event) => {
  event.preventDefault();

  const note = input.value.trim();
  if (!note) return;

  clearError();
  fetch(apiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ text: note }),
  })
    .then((response) => {
      if (!response.ok) throw new Error(`server returned HTTP ${response.status}`);
      return response.json();
    })
    .then((savedNote) => {
      notes.push(savedNote);
      input.value = "";
      renderNotes();
      input.focus();
    })
    .catch((error) => showError("Could not add note", error));
});

loadNotes();
