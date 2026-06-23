package com.example.gestionnairenotes.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionnairenotes.R;
import com.example.gestionnairenotes.adapter.NoteAdapter;
import com.example.gestionnairenotes.repository.NoteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    // ─── Clés Intent partagées avec NoteActivity ─────────────────────────────
    public static final String EXTRA_NOTE_ID = "note_id";
    public static final String EXTRA_NOTE_COLOR = "note_color";
    public static final String EXTRA_NOTE_MODE = "note_mode";
    public static final String MODE_CREATE = "mode_create";
    public static final String MODE_EDIT = "mode_edit";

    // ─── Vues ────────────────────────────────────────────────────────────────
    private RecyclerView         recyclerView;
    private TextView             textViewEmptyState;
    private FloatingActionButton fab;
    private EditText             etSearch;
    private Button               btnFavorites;

    // Palette
    private LinearLayout colorPalette;
    private View colorGreen, colorRed, colorBlue, colorYellow, colorOrange, colorGray;
    private View overlay;

    // ─── Logique ─────────────────────────────────────────────────────────────
    private NoteAdapter    adapter;
    private NoteRepository repository;

    private boolean isPaletteVisible  = false;
    private boolean isFavoritesActive = false;

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
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        fab                = findViewById(R.id.fabAddNote);
        etSearch           = findViewById(R.id.etSearch);
        btnFavorites       = findViewById(R.id.btnFavorites);

        colorPalette = findViewById(R.id.colorPalette);
        colorGreen   = findViewById(R.id.colorGreen);
        colorRed     = findViewById(R.id.colorRed);
        colorBlue    = findViewById(R.id.colorBlue);
        colorYellow  = findViewById(R.id.colorYellow);
        colorOrange  = findViewById(R.id.colorOrange);
        colorGray    = findViewById(R.id.colorGray);
        overlay      = findViewById(R.id.overlay);
    }

    private void initRecyclerView() {
        adapter = new NoteAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Simple click -> modification note
        adapter.setOnNoteClickListener(note -> {
            Intent intent = new Intent(this, NoteActivity.class);
            intent.putExtra(EXTRA_NOTE_ID,    note.getId());
            intent.putExtra(EXTRA_NOTE_COLOR, note.getCouleur());
            intent.putExtra(EXTRA_NOTE_MODE,  MODE_EDIT);
            startActivity(intent);
        });

        // Double click -> toggle favori
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
        Log.d("NotesApp", "initFab called");
        fab.setOnClickListener(v -> {
            Log.d("NotesApp", "FAB clicked");
            togglePalette();
        });
        overlay.setOnClickListener(v -> {
            Log.d("NotesApp", "Overlay clicked");
            hidePalette();
        });

        colorGreen.setOnClickListener(v  -> {
            Log.d("NotesApp", "colorGreen clicked");
            openCreateNote("#219653");
        });
        colorRed.setOnClickListener(v    -> {
            Log.d("NotesApp", "colorRed clicked");
            openCreateNote("#EB5757");
        });
        colorBlue.setOnClickListener(v   -> {
            Log.d("NotesApp", "colorBlue clicked");
            openCreateNote("#2F80ED");
        });
        colorYellow.setOnClickListener(v -> {
            Log.d("NotesApp", "colorYellow clicked");
            openCreateNote("#F2C94C");
        });
        colorOrange.setOnClickListener(v -> {
            Log.d("NotesApp", "colorOrange clicked");
            openCreateNote("#F2994A");
        });
        colorGray.setOnClickListener(v   -> {
            Log.d("NotesApp", "colorGray clicked");
            openCreateNote("#828282");
        });
    }

    private void togglePalette() {
        Log.d("NotesApp", "togglePalette called, isPaletteVisible: " + isPaletteVisible);
        if (isPaletteVisible) hidePalette();
        else showPalette();
    }

    private void showPalette() {
        Log.d("NotesApp", "showPalette called");
        isPaletteVisible = true;
        overlay.setVisibility(View.VISIBLE);
        colorPalette.setVisibility(View.VISIBLE);

        fab.animate().cancel();
        fab.animate().rotation(45f).setDuration(150).start();

        View[] cercles = {colorGreen, colorRed, colorBlue, colorYellow, colorOrange, colorGray};
        for (int i = 0; i < cercles.length; i++) {
            View c = cercles[i];
            if (c != null) {
                c.animate().cancel();
                c.setVisibility(View.VISIBLE);
                c.setAlpha(0f);
                c.setScaleX(0f);
                c.setScaleY(0f);
                c.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(150).setStartDelay(i * 30L).start();
            }
        }
    }

    private void hidePalette() {
        Log.d("NotesApp", "hidePalette called");
        isPaletteVisible = false;
        overlay.setVisibility(View.GONE);

        fab.animate().cancel();
        fab.animate().rotation(0f).setDuration(150).start();

        View[] cercles = {colorGray, colorOrange, colorYellow, colorRed, colorBlue, colorGreen};
        int count = cercles.length;
        for (int i = 0; i < count; i++) {
            View c = cercles[i];
            if (c != null) {
                c.animate().cancel();
                final boolean isLast = (i == count - 1);
                c.animate()
                 .alpha(0f)
                 .scaleX(0f)
                 .scaleY(0f)
                 .setDuration(150)
                 .setStartDelay(i * 30L)
                 .withEndAction(() -> {
                     if (isLast) {
                         colorPalette.setVisibility(View.GONE);
                     }
                 })
                 .start();
            }
        }
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
            textViewEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewEmptyState.setVisibility(View.GONE);
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