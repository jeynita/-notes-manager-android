package com.example.gestionnairenotes.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gestionnairenotes.R;
import com.example.gestionnairenotes.model.Note;
import com.example.gestionnairenotes.repository.NoteRepository;

public class NoteActivity extends AppCompatActivity {

    // ─── Vues ────────────────────────────────────────────────────────────────
    private RelativeLayout rootLayout;
    private EditText       etTitle;
    private EditText       etContent;
    private Button         btnAction;

    // ─── Logique ─────────────────────────────────────────────────────────────
    private NoteRepository repository;
    private String  mode;
    private String  hexColor;
    private int     noteId = -1;
    private Note    currentNote;

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        initViews();
        readExtras();
        applyColor();

        repository = new NoteRepository(getApplication());

        if (MainActivity.MODE_EDIT.equals(mode)) {
            loadNoteForEditing();
        } else {
            // Mode création
            btnAction.setText("Créer");
            btnAction.setOnClickListener(v -> saveNewNote());
        }
    }

    // ─── Initialisation ──────────────────────────────────────────────────────
    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        etTitle    = findViewById(R.id.etTitle);
        etContent  = findViewById(R.id.etContent);
        btnAction  = findViewById(R.id.btnAction);
    }

    private void readExtras() {
        mode     = getIntent().getStringExtra(MainActivity.EXTRA_NOTE_MODE);
        hexColor = getIntent().getStringExtra(MainActivity.EXTRA_NOTE_COLOR);
        noteId   = getIntent().getIntExtra(MainActivity.EXTRA_NOTE_ID, -1);

        if (mode == null)     mode     = MainActivity.MODE_CREATE;
        if (hexColor == null) hexColor = "#219653";
    }

    private void applyColor() {
        try {
            rootLayout.setBackgroundColor(Color.parseColor(hexColor));
        } catch (IllegalArgumentException e) {
            rootLayout.setBackgroundColor(Color.parseColor("#219653"));
        }
    }

    // ─── Mode Modification ───────────────────────────────────────────────────
    /**
     * Charge la note via LiveData et pré-remplit le formulaire.
     * On observe une seule fois puis on retire l'observer (removeObservers).
     */
    private void loadNoteForEditing() {
        repository.getAllNotes().observe(this, notes -> {
            if (notes == null) return;
            for (Note n : notes) {
                if (n.getId() == noteId) {
                    currentNote = n;
                    etTitle.setText(n.getTitre());
                    etContent.setText(n.getContenu());
                    etTitle.setSelection(etTitle.getText().length());
                    break;
                }
            }
            // Bouton "Modifier"
            btnAction.setText("Modifier");
            btnAction.setOnClickListener(v -> updateNote());

            // On retire l'observer après le premier chargement
            repository.getAllNotes().removeObservers(this);
        });
    }

    // ─── Sauvegarde ──────────────────────────────────────────────────────────
    private void saveNewNote() {
        String titre   = etTitle.getText().toString().trim();
        String contenu = etContent.getText().toString().trim();
        if (!validateInputs(titre, contenu)) return;

        Note note = new Note(titre, contenu, hexColor, false, System.currentTimeMillis());
        repository.insert(note);

        Toast.makeText(this, "Note créée ✓", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void updateNote() {
        String titre   = etTitle.getText().toString().trim();
        String contenu = etContent.getText().toString().trim();
        if (!validateInputs(titre, contenu)) return;

        if (currentNote == null) {
            Toast.makeText(this, "Erreur : note introuvable", Toast.LENGTH_SHORT).show();
            return;
        }

        currentNote.setTitre(titre);
        currentNote.setContenu(contenu);
        currentNote.setCouleur(hexColor);
        currentNote.setDate(System.currentTimeMillis());
        repository.update(currentNote);

        Toast.makeText(this, "Note modifiée ✓", Toast.LENGTH_SHORT).show();
        finish();
    }

    // ─── Validation ──────────────────────────────────────────────────────────
    private boolean validateInputs(String titre, String contenu) {
        if (TextUtils.isEmpty(titre)) {
            etTitle.setError("Le titre est obligatoire");
            etTitle.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(contenu)) {
            etContent.setError("Le contenu est obligatoire");
            etContent.requestFocus();
            return false;
        }
        return true;
    }
}
