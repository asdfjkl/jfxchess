/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.asdfjkl.jerryfx.lib;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Polyglot {

    byte[] book;
    public boolean readFile = false;
    final char[] promotionPieces = { ' ', 'n', 'b', 'r', 'q'};

    public void loadBook(File file) {

        OptimizedRandomAccessFile raf = null;
        try {
            //File file = new File(filename);
            long fileLength = file.length();
            raf = new OptimizedRandomAccessFile(file, "r");
            book = new byte[(int) fileLength];
            raf.readFully(book, 0, (int) fileLength);
            readFile = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public PolyglotEntry getEntryFromOffset(int offset) {

        if (book == null || offset >= book.length - 16) {
            throw new IllegalArgumentException("polyglot book is not loaded or offset out of range");
        }

        // polyglot entry:
        // key    uint64
        // move   uint16
        // weight uint16
        // learn  uint32
        byte[] bKey = Arrays.copyOfRange(book, offset, offset + 8);
        byte[] bMove = Arrays.copyOfRange(book, offset + 8, offset + 10);
        byte[] bWeight = Arrays.copyOfRange(book, offset + 10, offset + 12);
        byte[] bLearn = Arrays.copyOfRange(book, offset + 12, offset + 16);


        long key = ByteBuffer.wrap(bKey).getLong();
        int move = ByteBuffer.wrap(bMove).getShort();
        int weight = ByteBuffer.wrap(bWeight).getShort();
        int learn = ByteBuffer.wrap(bLearn).getInt();

        int from = 0;
        int fromRow = 0;
        int fromFile = 0;
        int to = 0;
        int toRow = 0;
        int toFile = 0;
        int promotion = 0;

        from      = (move>>6)&077;
        fromRow   = (from>>3)&0x7;
        fromFile  = from&0x7;
        to        = move&077;
        toRow     = (to>>3)&0x7;
        toFile    = to&0x7;
        promotion =(move>>12)&0x7;

        char cFromFile = (char) (fromFile+'a');
        char cFromRow  = (char) (fromRow+'1');
        char cToFile   = (char) (toFile+'a');
        char cToRow    = (char) (toRow+'1');

        StringBuilder sbUci = new StringBuilder().append(cFromFile).append(cFromRow).append(cToFile).append(cToRow);

        if(promotion > 0) {
            sbUci.append(promotionPieces[promotion]);
        }

        String uci = sbUci.toString();
        if(uci.equals("e1h1")) {
            uci = "e1g1";
        }
        if(uci.equals("e1a1")) {
            uci = "e1c1";
        }
        if(uci.equals("e8h8")) {
            uci = "e8g8";
        }
        if(uci.equals("e8a8")) {
            uci = "e8c8";
        }

        PolyglotEntry e = new PolyglotEntry();
        e.key = key;
        e.move = move;
        e.learn = learn;
        e.weight = weight;
        e.uci = uci;
        return e;
    }

    public ArrayList<String> findMoves(Board board) {

        long zobrist = board.getZobrist();
        return findMoves(zobrist);
    }

    public ArrayList<String> findMoves(long zobrist) {

        ArrayList<String> bookMoves = new ArrayList<String>();

        if(readFile) {

            int low = 0;
            int high = Integer.divideUnsigned(book.length, 16);

            // find entry fast
            while(Integer.compareUnsigned(low, high) < 0) {
                int middle = Integer.divideUnsigned(low + high, 2);
                PolyglotEntry e = getEntryFromOffset(middle*16);
                long middleKey = e.key;
                if(Long.compareUnsigned(middleKey, zobrist) < 0) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            int offset = low;
            int size = Integer.divideUnsigned(book.length, 16);

            // now we have the lowest key pos
            // where a possible entry is. collect all
            while(Integer.compareUnsigned(offset, size) < 0) {
                PolyglotEntry e = getEntryFromOffset(offset*16);
                if(Long.compareUnsigned(e.key,zobrist) != 0L) {
                    break;
                }
                String uci = e.uci;
                bookMoves.add(uci);
                offset += 1;
            }
        }
        return bookMoves;
    }

    public boolean inBook(Board board) {

        long zobrist = board.getZobrist();
        return inBook(zobrist);

    }

    public boolean inBook(long zobrist) {

        if(!readFile) {
            return false;
        }

        int low = 0;
        int high = Integer.divideUnsigned(book.length, 16);

        // find entry fast
        while(Integer.compareUnsigned(low, high) < 0) {
            int middle = Integer.divideUnsigned(low + high, 2);
            PolyglotEntry e = getEntryFromOffset(middle*16);
            long middleKey = e.key;
            if(Long.compareUnsigned(middleKey, zobrist) < 0L) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        int offset = low;
        int size = Integer.divideUnsigned(book.length, 16);

        // now we have the lowest key pos
        // where a possible entry is.
        if(Integer.compareUnsigned(offset, size) < 0) {
            PolyglotEntry e = getEntryFromOffset(offset*16);
            if(Long.compareUnsigned(e.key, zobrist) == 0) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

}

