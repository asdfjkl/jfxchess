package org.asdfjkl.jfxchess.lib;

public class ColoredField {

    public int x = 0;
    public int y = 0;

    @Override
    public boolean equals(Object o) {

        if (o instanceof ColoredField) {
            ColoredField other = (ColoredField) o;
            if(other.x == x && other.y == y) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

}
