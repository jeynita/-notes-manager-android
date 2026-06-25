package com.example.gestionnairenotes.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionnairenotes.R;
import com.example.gestionnairenotes.adapter.NoteAdapter;
import com.example.gestionnairenotes.repository.NoteRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.gestionnairenotes.model.Note;

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
    private FloatingActionButton btnSort;
    private ImageButton          btnThemeToggle;
    private TextView             tvNotesCount;

    // Palette
    private LinearLayout colorPalette;
    private View colorGreen, colorRed, colorBlue, colorYellow, colorOrange, colorGray;
    private View overlay;

    // ─── Logique ─────────────────────────────────────────────────────────────
    private NoteAdapter    adapter;
    private NoteRepository repository;

    private boolean isPaletteVisible  = false;
    private boolean isFavoritesActive = false;

    // Constantes de tri
    private static final int SORT_DATE_DESC = 0;
    private static final int SORT_DATE_ASC = 1;
    private static final int SORT_TITLE_ASC = 2;
    private static final int SORT_TITLE_DESC = 3;

    private int currentSortOption = SORT_DATE_DESC;
    private java.util.List<Note> currentRawNotes = new java.util.ArrayList<>();

    // ─── Lifecycle ───────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentSortOption = prefs.getInt("sort_option", SORT_DATE_DESC);

        initViews();
        repository = new NoteRepository(getApplication());
        initRecyclerView();
        initSearchBar();
        initFavoritesButton();
        initSortButton();
        initThemeToggle();
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
        btnSort            = findViewById(R.id.btnSort);
        btnThemeToggle     = findViewById(R.id.btnThemeToggle);
        tvNotesCount       = findViewById(R.id.tvNotesCount);

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

        // Appui long -> dialogue d'options (Partager / Supprimer / Annuler)
        adapter.setOnNoteLongClickListener(note -> {
            showNoteOptionsDialog(note);
        });

        // Activer le swipe-to-delete
        setupSwipeToDelete();
    }

    // ─── Observers LiveData ───────────────────────────────────────────────────
    private void observeAllNotes() {
        repository.getAllNotes().observe(this, notes -> {
            currentRawNotes = notes;
            sortAndSetNotes(notes);
        });
    }

    private void observeFavorites() {
        repository.getFavorites().observe(this, notes -> {
            currentRawNotes = notes;
            sortAndSetNotes(notes);
        });
    }

    private void observeSearch(String query) {
        repository.searchByTitle(query).observe(this, notes -> {
            currentRawNotes = notes;
            sortAndSetNotes(notes);
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
            btnFavorites.setText("Tout voir");
            btnFavorites.setBackgroundResource(R.drawable.bg_btn_favorites_active);
            btnFavorites.setTextColor(Color.BLACK);
        } else {
            btnFavorites.setText("Favoris");
            btnFavorites.setBackgroundResource(R.drawable.bg_btn_favorites);
            btnFavorites.setTextColor(getResources().getColor(R.color.text_primary, getTheme()));
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

    // ─── Initialisation des nouveaux boutons ─────────────────────────────────
    private void initSortButton() {
        btnSort.setOnClickListener(this::showSortPopupMenu);
    }

    private void initThemeToggle() {
        SharedPreferences prefs = getSharedPreferences("notes_prefs", MODE_PRIVATE);
        btnThemeToggle.setOnClickListener(v -> {
            boolean currentDark = prefs.getBoolean("dark_mode", false);
            prefs.edit().putBoolean("dark_mode", !currentDark).apply();
            AppCompatDelegate.setDefaultNightMode(!currentDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Appliquer l'icône correcte
        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        btnThemeToggle.setImageResource(isDark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
    }

    // ─── Gestion du tri des notes ───────────────────────────────────────────
    private void showSortPopupMenu(View anchor) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, anchor);
        popup.getMenu().add(0, SORT_DATE_DESC, 0, "Date : Récent en premier").setChecked(currentSortOption == SORT_DATE_DESC);
        popup.getMenu().add(0, SORT_DATE_ASC, 1, "Date : Ancien en premier").setChecked(currentSortOption == SORT_DATE_ASC);
        popup.getMenu().add(0, SORT_TITLE_ASC, 2, "Titre : A à Z").setChecked(currentSortOption == SORT_TITLE_ASC);
        popup.getMenu().add(0, SORT_TITLE_DESC, 3, "Titre : Z à A").setChecked(currentSortOption == SORT_TITLE_DESC);
        
        popup.getMenu().setGroupCheckable(0, true, true);
        
        popup.setOnMenuItemClickListener(item -> {
            currentSortOption = item.getItemId();
            getSharedPreferences("notes_prefs", MODE_PRIVATE).edit().putInt("sort_option", currentSortOption).apply();
            sortAndSetNotes(currentRawNotes);
            return true;
        });
        popup.show();
    }

    private void sortAndSetNotes(java.util.List<Note> notes) {
        if (notes == null) {
            adapter.setNotes(null);
            updateEmptyState();
            return;
        }
        java.util.List<Note> sortedList = new java.util.ArrayList<>(notes);
        switch (currentSortOption) {
            case SORT_DATE_DESC:
                sortedList.sort((n1, n2) -> Long.compare(n2.getDate(), n1.getDate()));
                break;
            case SORT_DATE_ASC:
                sortedList.sort((n1, n2) -> Long.compare(n1.getDate(), n2.getDate()));
                break;
            case SORT_TITLE_ASC:
                sortedList.sort((n1, n2) -> {
                    String t1 = n1.getTitre() != null ? n1.getTitre() : "";
                    String t2 = n2.getTitre() != null ? n2.getTitre() : "";
                    return t1.compareToIgnoreCase(t2);
                });
                break;
            case SORT_TITLE_DESC:
                sortedList.sort((n1, n2) -> {
                    String t1 = n1.getTitre() != null ? n1.getTitre() : "";
                    String t2 = n2.getTitre() != null ? n2.getTitre() : "";
                    return t2.compareToIgnoreCase(t1);
                });
                break;
        }
        adapter.setNotes(sortedList);
        updateEmptyState();
    }

    // ─── Swipe-to-delete ─────────────────────────────────────────────────────
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                final Note note = adapter.getNoteAt(position);
                if (note != null) {
                    repository.delete(note);
                    Snackbar.make(recyclerView, "Note supprimée", Snackbar.LENGTH_LONG)
                            .setAction("Annuler", v -> {
                                repository.insert(note);
                            })
                            .setActionTextColor(Color.YELLOW)
                            .show();
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView);
    }

    // ─── Appui long dialogue d'options ───────────────────────────────────────
    private void showNoteOptionsDialog(@NonNull Note note) {
        String favOption = note.isFavori() ? "Retirer des favoris" : "Ajouter aux favoris ★";
        String[] options = {"Modifier la note", favOption, "Partager la note", "Supprimer la note", "Annuler"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(note.getTitre())
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, NoteActivity.class);
                        intent.putExtra(EXTRA_NOTE_ID,    note.getId());
                        intent.putExtra(EXTRA_NOTE_COLOR, note.getCouleur());
                        intent.putExtra(EXTRA_NOTE_MODE,  MODE_EDIT);
                        startActivity(intent);
                    } else if (which == 1) {
                        note.setFavori(!note.isFavori());
                        repository.update(note);
                        String msg = note.isFavori() ? "Ajouté aux favoris ★" : "Retiré des favoris";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    } else if (which == 2) {
                        shareNote(note);
                    } else if (which == 3) {
                        showDeleteConfirmationDialog(note);
                    }
                })
                .show();
    }

    private void shareNote(Note note) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareBody = note.getTitre() + "\n\n" + note.getContenu();
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, note.getTitre());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(shareIntent, "Partager la note via"));
    }

    // ─── État vide & Compteur ───────────────────────────────────────────────
    private void updateEmptyState() {
        int count = adapter.getItemCount();
        if (count == 0) {
            recyclerView.setVisibility(View.GONE);
            textViewEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            textViewEmptyState.setVisibility(View.GONE);
        }

        String text;
        if (isFavoritesActive) {
            if (count == 0) text = "Aucun favori";
            else if (count == 1) text = "1 favori";
            else text = count + " favoris";
        } else {
            String query = etSearch.getText().toString().trim();
            if (!query.isEmpty()) {
                if (count == 0) text = "Aucune note trouvée";
                else if (count == 1) text = "1 note trouvée";
                else text = count + " notes trouvées";
            } else {
                if (count == 0) text = "Aucune note";
                else if (count == 1) text = "1 note";
                else text = count + " notes";
            }
        }
        tvNotesCount.setText(text);
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

    private void showDeleteConfirmationDialog(@NonNull Note note) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Supprimer la note")
                .setMessage("Es-tu sûre de vouloir supprimer la note \"" + note.getTitre() + "\" ?")
                .setPositiveButton("Supprimer", (dialog, which) -> {
                    repository.delete(note);
                    Toast.makeText(this, "Note supprimée", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
}