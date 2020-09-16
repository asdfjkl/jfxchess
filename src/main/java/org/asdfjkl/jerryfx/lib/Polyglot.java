package org.asdfjkl.jerryfx.lib;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Polyglot {

    byte[] book;
    boolean readFile = false;
    char[] promotionPieces = { ' ', 'n', 'b', 'r', 'q'};

    public void loadBook(File file) {

        OptimizedRandomAccessFile raf = null;
        try {
            //File file = new File(filename);
            long fileLength = file.length();
            //System.out.println(fileLength);
            //System.out.println((int) fileLength);
            raf = new OptimizedRandomAccessFile(file, "r");
            book = new byte[(int) fileLength];
            raf.readFully(book, 0, (int) fileLength);
            //System.out.println("book length: " + book.length);
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

        /*
        System.out.println("key...: " + Long.toHexString(key));
        System.out.println("move..: " + Integer.toHexString(move));
        System.out.println("weight: " + Integer.toHexString(weight));
        System.out.println("learn.: " + Integer.toHexString(learn));
         */

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

            long low = 0;
            long high = book.length / 16;

            // find entry fast
            while(low < high) {
                long middle = (low + high)/2;
                PolyglotEntry e = getEntryFromOffset((int) middle*16);
                long middleKey = e.key;
                if(middleKey < zobrist) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            long offset = low;
            long size = book.length / 16;

            // now we have the lowest key pos
            // where a possible entry is. collect all
            while(offset < size) {
                PolyglotEntry e = getEntryFromOffset((int) offset*16);
                if(e.key != zobrist) {
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

        long low = 0;
        long high = book.length / 16;

        // find entry fast
        while(low < high) {
            long middle = (low + high)/2;
            PolyglotEntry e = getEntryFromOffset((int) middle*16);
            long middleKey = e.key;
            if(middleKey < zobrist) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        long offset = low;
        long size = book.length / 16;

        // now we have the lowest key pos
        // where a possible entry is.
        if(offset < size) {
            PolyglotEntry e = getEntryFromOffset((int) offset*16);
            if(e.key == zobrist) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

}

