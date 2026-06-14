package com.example.gestionnairenotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionnairenotes.R;
import com.example.gestionnairenotes.adapter.NoteAdapter;
import com.example.gestionnairenotes.model.Note;
import com.example.gestionnairenotes.repository.NoteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    // ─── Clés Intent partagées avec NoteActivity ─────────────────────────────
    public static final String EXTRA_NOTE_ID    = "note_id";
    public static final String EXTRA_NOTE_COLOR = "note_color";
    public static final String EXTRA_NOTE_MODE  = "note_mode";
    public static final String MODE_CREATE      = "create";
    public static final String MODE_EDIT        = "edit";

    // ─── Vues ────────────────────────────────────────────────────────────────
    private RecyclerView         recyclerView;
    private TextView             textViewEmptyState; // Modifié ici (TextView à la place du LinearLayout)
    private EditText             etSearch;
    private Button               btnFavorites;
    private FloatingActionButton fab;

    // Palette
    private LinearLayout colorPalette;
    private View colorGreen, colorRed, colorBlue, colorYellow, colorOrange, colorGray;

    // ─── Logique ─────────────────────────────────────────────────────────────
    private NoteAdapter    adapter;
    private NoteRepository repository;

    private boolean isFavoritesActive = false;
    private boolean isPaletteVisible  = false;

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        repository = new NoteRepository(getApplication());
        initRecyclerView();
        initSearchBar();
        initFavoritesButton();
        initFab();
        setupBackPressedCallback();

        // Observer LiveData — toutes les notes par défaut
        observeAllNotes();
    }

    // ─── Initialisation ──────────────────────────────────────────────────────
    private void initViews() {
        recyclerView       = findViewById(R.id.recyclerView);
        textViewEmptyState = findViewById(R.id.textViewEmptyState); // Modifié ici
        etSearch           = findViewById(R.id.etSearch);
        btnFavorites       = findViewById(R.id.btnFavorites);
        fab                = findViewById(R.id.fabAddNote); // Modifié ici pour correspondre à l'ID du XML

        colorPalette = findViewById(R.id.colorPalette);
        colorGreen   = findViewById(R.id.colorGreen);
        colorRed     = findViewById(R.id.colorRed);
        colorBlue    = findViewById(R.id.colorBlue);
        colorYellow  = findViewById(R.id.colorYellow);
        colorOrange  = findViewById(R.id.colorOrange);
        colorGray    = findViewById(R.id.colorGray);
    }

    private void initRecyclerView() {
        adapter = new NoteAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Clic simple → NoteActivity mode modification
        adapter.setOnNoteClickListener(note -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra(EXTRA_NOTE_ID,    note.getId());
            intent.putExtra(EXTRA_NOTE_COLOR, note.getCouleur());
            intent.putExtra(EXTRA_NOTE_MODE,  MODE_EDIT);
            startActivity(intent);
        });

        // Double-clic → toggle favori
        adapter.setOnNoteDoubleTapListener(note -> {
            note.setFavori(!note.isFavori());
            repository.update(note);
            String msg = note.isFavori() ? "Ajouté aux favoris ★" : "Retiré des favoris";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    // ─── Observers LiveData ───────────────────────────────────────────────────
    private void observeAllNotes() {
        repository.getAllNotes().observe(this, notes -> {
            adapter.setNotes(notes);
            updateEmptyState();
        });
    }

    private void observeFavorites() {
        repository.getFavorites().observe(this, notes -> {
            adapter.setNotes(notes);
            updateEmptyState();
        });
    }

    private void observeSearch(String query) {
        repository.searchByTitle(query).observe(this, notes -> {
            adapter.setNotes(notes);
            updateEmptyState();
        });
    }

    // ─── SearchBar ───────────────────────────────────────────────────────────
    private void initSearchBar() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isFavoritesActive) {
                    isFavoritesActive = false;
                    updateFavoritesButtonStyle();
                }
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    observeAllNotes();
                } else {
                    observeSearch(query);
                }
            }
        });
    }

    // ─── Bouton Favoris ──────────────────────────────────────────────────────
    private void initFavoritesButton() {
        btnFavorites.setOnClickListener(v -> {
            isFavoritesActive = !isFavoritesActive;
            etSearch.setText("");
            if (isFavoritesActive) {
                observeFavorites();
            } else {
                observeAllNotes();
            }
            updateFavoritesButtonStyle();
        });
    }

    private void updateFavoritesButtonStyle() {
        if (isFavoritesActive) {
            btnFavorites.setBackgroundResource(R.drawable.bg_btn_favorites_active);
            btnFavorites.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            btnFavorites.setBackgroundResource(R.drawable.bg_btn_favorites);
            btnFavorites.setTextColor(getResources().getColor(android.R.color.black, null));
        }
    }

    // ─── FAB + Palette ───────────────────────────────────────────────────────
    private void initFab() {
        fab.setOnClickListener(v -> togglePalette());

        colorGreen.setOnClickListener(v  -> openCreateNote("#219653"));
        colorRed.setOnClickListener(v    -> openCreateNote("#EB5757"));
        colorBlue.setOnClickListener(v   -> openCreateNote("#2F80ED"));
        colorYellow.setOnClickListener(v -> openCreateNote("#F2C94C"));
        colorOrange.setOnClickListener(v -> openCreateNote("#F2994A"));
        colorGray.setOnClickListener(v   -> openCreateNote("#828282"));
    }

    private void togglePalette() {
        if (isPaletteVisible) hidePalette();
        else showPalette();
    }

    private void showPalette() {
        isPaletteVisible = true;
        colorPalette.setVisibility(View.VISIBLE);
        View[] cercles = {colorGreen, colorRed, colorBlue, colorYellow, colorOrange, colorGray};
        for (int i = 0; i < cercles.length; i++) {
            View c = cercles[i];
            if (c != null) { // Protection anti-crash
                c.setVisibility(View.VISIBLE); // Rend les cercles visibles
                c.setAlpha(0f);
                c.setScaleX(0f);
                c.setScaleY(0f);
                c.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(150).setStartDelay(i * 30L).start();
            }
        }
    }

    private void hidePalette() {
        isPaletteVisible = false;
        colorPalette.setVisibility(View.GONE);
    }

    private void openCreateNote(String hexColor) {
        hidePalette();
        Intent intent = new Intent(this, NoteActivity.class);
        intent.putExtra(EXTRA_NOTE_COLOR, hexColor);
        intent.putExtra(EXTRA_NOTE_MODE,  MODE_CREATE);
        startActivity(intent);
    }

    // ─── État vide ───────────────────────────────────────────────────────────
    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
            textViewEmptyState.setVisibility(View.VISIBLE); // Modifié ici
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewEmptyState.setVisibility(View.GONE); // Modifié ici
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isPaletteVisible) {
                    hidePalette();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }
}