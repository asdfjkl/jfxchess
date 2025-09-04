package org.asdfjkl.jfxchess.gui;
import javafx.scene.image.Image;

public class BotEngine extends Engine {

    private String bio = "";
    private Image image;
    private String elo = "";

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setElo(String s) {
        elo = s;
    }

    public String getElo() {
        return elo;
    }

    public String getBio() {
        return bio;
    }

    public void loadImage(String resourcePath) {
        image = new Image(resourcePath);
    }

    public Image getImage() {
        return image;
    }

}
