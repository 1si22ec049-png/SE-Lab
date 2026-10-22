package com.selab.notes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class NotesControllerTest {
    @Mock
    private NoteRepository notes;

    @InjectMocks
    private NotesController controller;

    @Test
    void createsNoteWithTrimmedText() {
        when(notes.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Note> response =
                controller.createNote(new NotesController.CreateNoteRequest("  Learn testing  "));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Learn testing", response.getBody().getText());

        ArgumentCaptor<Note> savedNote = ArgumentCaptor.forClass(Note.class);
        verify(notes).save(savedNote.capture());
        assertEquals("Learn testing", savedNote.getValue().getText());
    }

    @Test
    void rejectsMissingOrBlankTextWithoutSaving() {
        assertEquals(
                HttpStatus.BAD_REQUEST,
                controller.createNote(new NotesController.CreateNoteRequest(null)).getStatusCode());
        assertEquals(
                HttpStatus.BAD_REQUEST,
                controller.createNote(new NotesController.CreateNoteRequest("  ")).getStatusCode());

        verifyNoInteractions(notes);
    }

    @Test
    void retrievesNotesInAscendingIdOrder() {
        List<Note> expected = List.of(new Note("First"), new Note("Second"));
        when(notes.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(expected);

        assertEquals(expected, controller.getNotes());
        verify(notes).findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void delegatesDeletionForTheRequestedId() {
        ResponseEntity<Void> response = controller.deleteNote(42L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(notes).deleteById(42L);
    }

    @Test
    void intentionalCiFailureDemo() {
        assertEquals(2, 2);
    }
}
