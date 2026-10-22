package com.selab.notes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {}


// This interface extends JpaRepository, which provides CRUD operations for the Note entity. The Note entity is assumed to be a JPA entity representing a note in the application. By extending JpaRepository, NoteRepository inherits methods for saving, deleting, and finding notes in the database without needing to implement them manually.