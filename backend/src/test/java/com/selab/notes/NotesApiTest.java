package com.selab.notes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.Set;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotesApiTest {
    private static final Set<String> SAFE_TEST_DATABASES =
            Set.of("selab_notes_test", "selab_notes_regression");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NoteRepository notes;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void clearTestDatabaseBeforeEachTest() throws SQLException {
        assertSafeTestDatabase();
        notes.deleteAll();
    }

    @AfterEach
    void clearTestDatabaseAfterEachTest() throws SQLException {
        assertSafeTestDatabase();
        notes.deleteAll();
    }

    private void assertSafeTestDatabase() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            if (!SAFE_TEST_DATABASES.contains(connection.getCatalog())) {
                throw new IllegalStateException(
                        "Refusing to clear notes from non-test database " + connection.getCatalog());
            }
        }
    }

    @Test
    void createsListsAndDeletesNotes() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Learn REST\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.text").value("Learn REST"))
                .andReturn();

        long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/notes").header("Origin", "http://localhost:5500"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].text").value(hasItem("Learn REST")));

        assertNotNull(notes.findById(id).orElse(null));

        mockMvc.perform(delete("/api/notes/" + id))
                .andExpect(status().isNoContent());

        assertFalse(notes.existsById(id));

        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").isEmpty());
    }

    @Test
    void getReturnsNotesInAscendingIdOrder() throws Exception {
        Note first = notes.save(new Note("First"));
        Note second = notes.save(new Note("Second"));

        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("First"))
                .andExpect(jsonPath("$[1].text").value("Second"))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[1].id").value(second.getId()));
    }

    @Test
    void rejectsBlankNoteText() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deletingMissingNoteIsIdempotent() throws Exception {
        mockMvc.perform(delete("/api/notes/9223372036854775807"))
                .andExpect(status().isNoContent());
    }
}
