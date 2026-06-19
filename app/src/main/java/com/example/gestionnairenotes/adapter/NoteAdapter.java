package com.example.gestionnairenotes.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gestionnairenotes.R;
import com.example.gestionnairenotes.model.Note;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    // ─── Interfaces callbacks ────────────────────────────────────────────────
    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    public interface OnNoteDoubleTapListener {
        void onNoteDoubleTap(Note note);
    }

    public interface OnNoteLongClickListener {
        void onNoteLongClick(Note note);
    }
    // ─── Champs ─────────────────────────────────────────────────────────────
    private final Context context;
    private List<Note> noteList;
    private OnNoteClickListener     clickListener;
    private OnNoteDoubleTapListener doubleTapListener;

    private static final String DEFAULT_COLOR = "#828282";

    // ─── Constructeur ────────────────────────────────────────────────────────
    public NoteAdapter(Context context) {
        this.context  = context;
        this.noteList = new ArrayList<>();
    }

    // ─── Setters ─────────────────────────────────────────────────────────────
    public void setOnNoteClickListener(OnNoteClickListener l)         { this.clickListener     = l; }
    public void setOnNoteDoubleTapListener(OnNoteDoubleTapListener l) { this.doubleTapListener = l; }
    private OnNoteLongClickListener mLongClickListener;

    public void setOnNoteLongClickListener(OnNoteLongClickListener listener) {
        this.mLongClickListener = listener;
    }

    /**
     * Met à jour la liste affichée.
     * Appelé par l'Observer LiveData dans MainActivity.
     */
    public void setNotes(List<Note> notes) {
        this.noteList = notes != null ? notes : new ArrayList<>();
        notifyDataSetChanged();
    }

    public Note getNoteAt(int position) {
        if (position >= 0 && position < noteList.size()) {
            return noteList.get(position);
        }
        return null;
    }

    // ─── RecyclerView.Adapter ────────────────────────────────────────────────
    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        holder.bind(noteList.get(position));
    }

    @Override
    public int getItemCount() { return noteList.size(); }

    // ─── ViewHolder ──────────────────────────────────────────────────────────
    class NoteViewHolder extends RecyclerView.ViewHolder {

        private final CardView       cardNote;
        private final RelativeLayout noteContainer;
        private final TextView       tvTitle;
        private final TextView       tvContent;
        private final TextView       tvDate;
        private final ImageView      ivStar;

        private final SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardNote      = itemView.findViewById(R.id.cardNote);
            noteContainer = itemView.findViewById(R.id.noteContainer);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvContent     = itemView.findViewById(R.id.tvContent);
            tvDate        = itemView.findViewById(R.id.tvDate);
            ivStar        = itemView.findViewById(R.id.ivStar);
            setupGestureDetector();
        }

        private void setupGestureDetector() {
            GestureDetector detector = new GestureDetector(context,
                    new GestureDetector.SimpleOnGestureListener() {

                        @Override
                        public boolean onDown(MotionEvent e) { return true; }

                        @Override
                        public boolean onSingleTapConfirmed(MotionEvent e) {
                            int pos = getAdapterPosition();
                            if (pos != RecyclerView.NO_ID && clickListener != null)
                                clickListener.onNoteClick(noteList.get(pos));
                            return true;
                        }

                        @Override
                        public boolean onDoubleTap(MotionEvent e) {
                            int pos = getAdapterPosition();
                            if (pos != RecyclerView.NO_ID && doubleTapListener != null)
                                doubleTapListener.onNoteDoubleTap(noteList.get(pos));
                            return true;
                        }

                        @Override
                        public void onLongPress(MotionEvent e) {
                            int pos = getAdapterPosition();
                            // On vérifie que la position est valide et qu'un écouteur a été configuré
                            if (pos != RecyclerView.NO_POSITION && mLongClickListener != null) {
                                mLongClickListener.onNoteLongClick(noteList.get(pos));
                            }
                        }
                    });

            itemView.setOnTouchListener((v, event) -> {
                detector.onTouchEvent(event);
                return true;
            });
        }

        void bind(Note note) {
            tvTitle.setText(note.getTitre());
            tvContent.setText(note.getContenu() != null ? note.getContenu() : "");
            tvDate.setText(note.getDate() > 0
                    ? dateFormat.format(new Date(note.getDate())) : "");
            ivStar.setVisibility(note.isFavori() ? View.VISIBLE : View.INVISIBLE);

            // Couleur de fond
            try {
                String hex = (note.getCouleur() != null && !note.getCouleur().isEmpty())
                        ? note.getCouleur() : DEFAULT_COLOR;
                int colorVal = Color.parseColor(hex);
                noteContainer.setBackgroundColor(colorVal);

                boolean isLight = androidx.core.graphics.ColorUtils.calculateLuminance(colorVal) > 0.5;
                int textColor = isLight ? Color.BLACK : Color.WHITE;
                int subTextColor = isLight ? Color.parseColor("#555555") : Color.parseColor("#E0E0E0");
                int dateColor = isLight ? Color.parseColor("#777777") : Color.parseColor("#CCCCCC");

                tvTitle.setTextColor(textColor);
                tvContent.setTextColor(subTextColor);
                tvDate.setTextColor(dateColor);

                if (isLight) {
                    ivStar.setColorFilter(Color.parseColor("#E28500"));
                } else {
                    ivStar.setColorFilter(Color.parseColor("#F2C94C"));
                }
            } catch (IllegalArgumentException e) {
                noteContainer.setBackgroundColor(Color.parseColor(DEFAULT_COLOR));
                tvTitle.setTextColor(Color.WHITE);
                tvContent.setTextColor(Color.parseColor("#E0E0E0"));
                tvDate.setTextColor(Color.parseColor("#CCCCCC"));
                ivStar.setColorFilter(Color.parseColor("#F2C94C"));
            }
        }
    }
}
