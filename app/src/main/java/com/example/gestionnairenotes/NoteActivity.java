package com.example.gestionnairenotes;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionnairenotes.model.Note;
import com.example.gestionnairenotes.repository.NoteRepository;

public class NoteActivity extends AppCompatActivity {

    private EditText editTextTitle;
    private EditText editTextContent;
    private Button btnSaveNote;
    private LinearLayout noteActivityLayout;

    private String selectedColor;
    private int noteId = -1;
    private boolean isEditMode = false;

    private NoteRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        repository = new NoteRepository(getApplication());

        editTextTitle      = findViewById(R.id.editTextTitle);
        editTextContent    = findViewById(R.id.editTextContent);
        btnSaveNote        = findViewById(R.id.btnSaveNote);
        noteActivityLayout = findViewById(R.id.noteActivityLayout);

        Intent intent = getIntent();
        selectedColor = intent.getStringExtra("color");
        noteId        = intent.getIntExtra("noteId", -1);
        isEditMode    = intent.getBooleanExtra("isEditMode", false);

        // Appliquer la couleur de fond
        if (selectedColor != null && !selectedColor.isEmpty()) {
            try {
                noteActivityLayout.setBackgroundColor(Color.parseColor(selectedColor));
            } catch (Exception e) { }
        }

        // Mode modification → pré-remplir
        if (isEditMode) {
            editTextTitle.setText(intent.getStringExtra("noteTitle"));
            editTextContent.setText(intent.getStringExtra("noteContent"));
            btnSaveNote.setText("Modifier");
        }

        btnSaveNote.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title   = editTextTitle.getText().toString().trim();
        String content = editTextContent.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editTextTitle.setError("Le titre est obligatoire");
            editTextTitle.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(content)) {
            editTextContent.setError("Le contenu est obligatoire");
            editTextContent.requestFocus();
            return;
        }

        long currentDate = System.currentTimeMillis();
        String color = (selectedColor != null && !selectedColor.isEmpty())
                ? selectedColor : "#828282";

        if (isEditMode) {
            Note updatedNote = new Note(title, content, color,
                    getIntent().getBooleanExtra("isFavorite", false), currentDate);
            updatedNote.setId(noteId);
            repository.update(updatedNote);
            Toast.makeText(this, "Note modifiée ✓", Toast.LENGTH_SHORT).show();
        } else {
            Note newNote = new Note(title, content, color, false, currentDate);
            repository.insert(newNote);
            Toast.makeText(this, "Note créée ✓", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}