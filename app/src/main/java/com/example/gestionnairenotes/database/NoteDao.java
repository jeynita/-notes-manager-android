package com.example.gestionnairenotes.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.gestionnairenotes.model.Note;
import java.util.List;

@Dao
public interface NoteDao {

    @Insert
    void insert(Note note);

    @Update
    void update(Note note);

    @Delete
    void delete(Note note);


    @Query("SELECT * FROM notes ORDER BY date DESC")
    LiveData<List<Note>> getAllNotes();


    @Query("SELECT * FROM notes WHERE favori = 1 ORDER BY date DESC")
    LiveData<List<Note>> getFavorites();


    @Query("SELECT * FROM notes WHERE titre LIKE :searchQuery ORDER BY date DESC")
    LiveData<List<Note>> searchByTitle(String searchQuery);
}
