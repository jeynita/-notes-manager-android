package com.example.gestionnairenotes.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "titre")
    private String titre;

    @ColumnInfo(name = "contenu")
    private String contenu;

    @ColumnInfo(name = "couleur")
    private String couleur;

    @ColumnInfo(name = "favori")
    private boolean favori;

    @ColumnInfo(name = "date")
    private long date;

    // Constructeur vide requis par Room
    public Note() { }

    // Constructeur principal
    public Note(String titre, String contenu, String couleur, boolean favori, long date) {
        this.titre   = titre;
        this.contenu = contenu;
        this.couleur = couleur;
        this.favori  = favori;
        this.date    = date;
    }

    // Getters
    public int getId()          { return id; }
    public String getTitre()    { return titre; }
    public String getContenu()  { return contenu; }
    public String getCouleur()  { return couleur; }
    public boolean isFavori()   { return favori; }
    public long getDate()       { return date; }

    // Setters
    public void setId(int id)             { this.id = id; }
    public void setTitre(String titre)    { this.titre = titre; }
    public void setContenu(String c)      { this.contenu = c; }
    public void setCouleur(String c)      { this.couleur = c; }
    public void setFavori(boolean f)      { this.favori = f; }
    public void setDate(long date)        { this.date = date; }

    // Validation avant sauvegarde
    public boolean isValid() {
        return titre != null  && !titre.trim().isEmpty()
                && contenu != null && !contenu.trim().isEmpty();
    }
}