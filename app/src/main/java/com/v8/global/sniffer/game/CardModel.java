package com.v8.global.sniffer.game;

public class CardModel {
    private String image;
    private boolean isFlipped;
    private boolean isMatched;

    public CardModel(String image, boolean isFlipped, boolean isMatched) {
        this.image = image;
        this.isFlipped = isFlipped;
        this.isMatched = isMatched;
    }

    public String getImage() { return image; }
    public boolean isFlipped() { return isFlipped; }
    public void setFlipped(boolean flipped) { isFlipped = flipped; }
    public boolean isMatched() { return isMatched; }
    public void setMatched(boolean matched) { isMatched = matched; }
}
