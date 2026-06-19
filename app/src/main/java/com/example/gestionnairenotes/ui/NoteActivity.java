package com.example.gestionnairenotes.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
    
    private ImageButton    btnBack;
    private ImageButton    btnShare;
    private ImageButton    btnDelete;
    private ImageButton    btnFavorite;

    // ─── Logique ─────────────────────────────────────────────────────────────
    private NoteRepository repository;
    private String  mode;
    private String  hexColor;
    private int     noteId = -1;
    private Note    currentNote;
    private boolean isFavori = false;

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        initViews();
        readExtras();
        applyColor();

        repository = new NoteRepository(getApplication());

        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            isFavori = !isFavori;
            updateFavoriteIcon();
            if (MainActivity.MODE_EDIT.equals(mode) && currentNote != null) {
                currentNote.setFavori(isFavori);
                repository.update(currentNote);
                String msg = isFavori ? "Ajouté aux favoris ★" : "Retiré des favoris";
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        if (MainActivity.MODE_EDIT.equals(mode)) {
            btnDelete.setVisibility(View.VISIBLE);
            loadNoteForEditing();
        } else {
            // Mode création
            btnAction.setText("Créer");
            btnAction.setOnClickListener(v -> saveNewNote());
            btnShare.setOnClickListener(v -> {
                String titre = etTitle.getText().toString().trim();
                String contenu = etContent.getText().toString().trim();
                shareNote(titre, contenu);
            });
        }
    }

    // ─── Initialisation ──────────────────────────────────────────────────────
    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        etTitle    = findViewById(R.id.etTitle);
        etContent  = findViewById(R.id.etContent);
        btnAction  = findViewById(R.id.btnAction);
        btnBack    = findViewById(R.id.btnBack);
        btnShare   = findViewById(R.id.btnShare);
        btnDelete  = findViewById(R.id.btnDelete);
        btnFavorite = findViewById(R.id.btnFavorite);
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
            int colorVal = Color.parseColor(hexColor);
            rootLayout.setBackgroundColor(colorVal);

            boolean isLight = androidx.core.graphics.ColorUtils.calculateLuminance(colorVal) > 0.5;
            int textColor = isLight ? Color.BLACK : Color.WHITE;
            int hintColor = isLight ? Color.parseColor("#88000000") : Color.parseColor("#99FFFFFF");

            etTitle.setTextColor(textColor);
            etTitle.setHintTextColor(hintColor);
            etContent.setTextColor(textColor);
            etContent.setHintTextColor(hintColor);

            // Teinter le curseur de texte sur API 29+ (Android 10+) pour garantir sa visibilité
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.graphics.drawable.Drawable cursorDrawable = etTitle.getTextCursorDrawable();
                if (cursorDrawable != null) {
                    cursorDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN));
                    etTitle.setTextCursorDrawable(cursorDrawable);
                }
                android.graphics.drawable.Drawable contentCursorDrawable = etContent.getTextCursorDrawable();
                if (contentCursorDrawable != null) {
                    contentCursorDrawable.setColorFilter(new android.graphics.PorterDuffColorFilter(textColor, android.graphics.PorterDuff.Mode.SRC_IN));
                    etContent.setTextCursorDrawable(contentCursorDrawable);
                }
            }

            int iconColor = isLight ? Color.BLACK : Color.WHITE;
            btnBack.setColorFilter(iconColor);
            btnShare.setColorFilter(iconColor);
            btnDelete.setColorFilter(iconColor);
            updateFavoriteIcon();
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
                    isFavori = n.isFavori();
                    updateFavoriteIcon();
                    break;
                }
            }
            // Bouton "Modifier"
            btnAction.setText("Modifier");
            btnAction.setOnClickListener(v -> updateNote());

            // Actions nécessitant currentNote
            btnDelete.setOnClickListener(v -> showDeleteConfirmation());
            btnShare.setOnClickListener(v -> shareNote(currentNote));

            // On retire l'observer après le premier chargement
            repository.getAllNotes().removeObservers(this);
        });
    }

    // ─── Actions de l'en-tête ────────────────────────────────────────────────
    private void shareNote(Note note) {
        if (note != null) {
            shareNote(note.getTitre(), note.getContenu());
        }
    }

    private void shareNote(String titre, String contenu) {
        if (TextUtils.isEmpty(titre) && TextUtils.isEmpty(contenu)) {
            Toast.makeText(this, "Impossible de partager une note vide", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = (TextUtils.isEmpty(titre) ? "" : titre + "\n\n") + (TextUtils.isEmpty(contenu) ? "" : contenu);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, titre);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Partager la note via"));
    }

    private void showDeleteConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer la note")
                .setMessage("Voulez-vous vraiment supprimer cette note ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    if (currentNote != null) {
                        repository.delete(currentNote);
                        Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ─── Sauvegarde ──────────────────────────────────────────────────────────
    private void saveNewNote() {
        String titre   = etTitle.getText().toString().trim();
        String contenu = etContent.getText().toString().trim();
        if (!validateInputs(titre, contenu)) return;

        Note note = new Note(titre, contenu, hexColor, isFavori, System.currentTimeMillis());
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
        currentNote.setFavori(isFavori);
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

    private void updateFavoriteIcon() {
        btnFavorite.setImageResource(isFavori ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        try {
            int colorVal = Color.parseColor(hexColor);
            boolean isLight = androidx.core.graphics.ColorUtils.calculateLuminance(colorVal) > 0.5;
            if (isFavori) {
                btnFavorite.setColorFilter(isLight ? Color.parseColor("#E28500") : Color.parseColor("#F2C94C"));
            } else {
                btnFavorite.setColorFilter(isLight ? Color.BLACK : Color.WHITE);
            }
        } catch (IllegalArgumentException e) {
            btnFavorite.setColorFilter(Color.WHITE);
        }
    }
}
