package com.example.gestionnairenotes.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;

import com.example.gestionnairenotes.database.NoteDao;
import com.example.gestionnairenotes.database.NoteDatabase;
import com.example.gestionnairenotes.model.Note;

import java.util.List;

public class NoteRepository {

    private final NoteDao noteDao;

    public NoteRepository(Application application) {
        NoteDatabase db = NoteDatabase.getDatabase(application);
        noteDao = db.noteDao();
    }

    public LiveData<List<Note>> getAllNotes() {
        return noteDao.getAllNotes();
    }

    public LiveData<List<Note>> getFavorites() {
        return noteDao.getFavorites();
    }

    public LiveData<List<Note>> searchByTitle(String title) {
        return noteDao.searchByTitle("%" + title + "%");
    }


    public void insert(Note note) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.insert(note);
        });
    }

    public void update(Note note) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.update(note);
        });
    }

    public void delete(Note note) {
        NoteDatabase.databaseWriteExecutor.execute(() -> {
            noteDao.delete(note);
        });
    }
}
