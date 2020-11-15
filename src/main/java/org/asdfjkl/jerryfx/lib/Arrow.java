package org.asdfjkl.jerryfx.lib;

public class Arrow {

    public int xFrom = 0;
    public int yFrom = 0;
    public int xTo = 0;
    public int yTo = 0;

    @Override
    public boolean equals(Object o) {

        if (o instanceof Arrow) {
            Arrow other = (Arrow) o;
            if (other.xFrom == xFrom && other.yFrom == yFrom
                    && other.xTo == xTo && other.yTo == yTo) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
}
