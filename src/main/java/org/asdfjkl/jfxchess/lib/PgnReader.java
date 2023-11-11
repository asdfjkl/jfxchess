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

package org.asdfjkl.jfxchess.lib;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import org.asdfjkl.jfxchess.gui.PgnDatabaseEntry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class PgnReader {

    GameNode currentNode = null;
    String currentLine;  // current line
    int currentIdx = 0; // current index
    final Stack<GameNode> gameStack;
    String encoding;

    public PgnReader() {
        gameStack = new Stack<>();
         this.encoding = "UTF-8";
    }

    public void setEncodingUTF8() {
        this.encoding = "UTF-8";
    }

    public void setEncodingIsoLatin1() {
        this.encoding = "ISO-8859-1";
    }

    public String getEncoding() { return encoding; }

    public boolean isIsoLatin1(String filename) {

        boolean isLatin1 = false;
        OptimizedRandomAccessFile raf = null;
        ArrayList<Byte> bytesRead = new ArrayList<>();
        String line = "";
        try {
            raf = new OptimizedRandomAccessFile(filename, "r");
            CharsetDetector detector = new CharsetDetector();
            // read the first line, and from
            // the first 10000 lines read all tags and comments
            int i=0;
            while((line = raf.readLine()) != null && i < 1000) {
                if(i == 0 || (line.contains("[") || line.contains("{") || line.contains("}"))) {
                    byte[] lineBytes = line.getBytes(StandardCharsets.ISO_8859_1);
                    for (int j = 0; j < lineBytes.length; j++) {
                        bytesRead.add(lineBytes[j]);
                    }
                }
                i++;
            }
            byte[] primBytesRead = new byte[bytesRead.size()];
            for(i = 0; i < bytesRead.size(); i++) {
                primBytesRead[i] = bytesRead.get(i).byteValue();
            }
            detector.setText(primBytesRead);
            CharsetMatch match = detector.detect();
            String encoding = match.getName();
            if(encoding.equals("ISO-8859-1")) {
                isLatin1 = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return isLatin1;
    }


    /*
    bool PgnReader::isUtf8(const QString &filename) {
        // very simple way to detect the majority of encodings:
        // first try ISO 8859-1
        // open the file and read a max of 100 first bytes
        // if conversion to unicode works, try some more bytes (at most 40 * 100)
        // if conversion errors occur, we simply assume UTF-8
        //const char* iso = "ISO 8859-1";
        //const char* utf8 = "UTF-8";
        QFile file(filename);
        if(!file.open(QFile::ReadOnly)) {
            return true;
        }
        QDataStream in(&file);
        // init some char array to read bytes
        char first100arr[100];
        for(int i=0;i<100;i++) {
            first100arr[i] = 0x00;
        }
        char *first100 = first100arr;
        // prep conversion tools
        QTextCodec::ConverterState state;
        QTextCodec *codec = QTextCodec::codecForName("UTF-8");

        int iterations = 40;
        int i=0;
        int l = 100;
        bool isUtf8 = true;
        while(i<iterations && l>=100) {
            l = in.readRawData(first100, 100);
        const QString text = codec->toUnicode(first100, 100, &state);
            if (state.invalidChars > 0) {
                isUtf8 = false;
                break;
            }
            i++;
        }
        return isUtf8;
    }
    */

    ArrayList<PgnDatabaseEntry> scanPgnGetSTR(String filename) {

        ArrayList<PgnDatabaseEntry> entries = new ArrayList<>();

        boolean inComment = false;
        long game_pos = -1;
        PgnDatabaseEntry current = null;
        long last_pos = 0;

        String currentLine = "";
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(filename, "r");
            while ((currentLine = raf.readLine()) != null) {
                // skip comments
                if (currentLine.startsWith("%")) {
                    continue;
                }

                if (!inComment && currentLine.startsWith("[")) {

                    if (game_pos == -1) {
                        game_pos = last_pos;
                        current = new PgnDatabaseEntry();
                    }
                    last_pos = raf.getFilePointer();

                    if (currentLine.length() > 4) {
                        int spaceOffset = currentLine.indexOf(' ');
                        int firstQuote = currentLine.indexOf('"');
                        int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                        String tag = currentLine.substring(1, spaceOffset);
                        String value = currentLine.substring(firstQuote + 1, secondQuote);
                        String valueEncoded = new String(value.getBytes(StandardCharsets.ISO_8859_1), encoding);
                        if(tag.equals("Event")) {
                            current.setEvent(valueEncoded);
                        }
                        if(tag.equals("Site")) {
                            current.setSite(valueEncoded);
                        }
                        if(tag.equals("Round")) {
                            current.setRound(valueEncoded);
                        }
                        if(tag.equals("White")) {
                            current.setWhite(valueEncoded);
                        }
                        if(tag.equals("Black")) {
                            current.setBlack(valueEncoded);
                        }
                        if(tag.equals("Result")) {
                            current.setResult(valueEncoded);
                        }
                    }

                    continue;
                }
                if ((!inComment && currentLine.contains("{"))
                        || (inComment && currentLine.contains("}"))) {
                    inComment = currentLine.lastIndexOf("{") > currentLine.lastIndexOf("}");
                }

                if (game_pos != -1) {
                    current.setOffset(game_pos);
                    entries.add(current);
                    game_pos = -1;
                }

                last_pos = raf.getFilePointer();
            }
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
        return entries;
    }



    public ArrayList<Long> scanPgn(String filename) {

        ArrayList<Long> offsets = new ArrayList<>();

        boolean inComment = false;
        long game_pos = -1;
        long last_pos = 0;

        String currentLine = "";
        OptimizedRandomAccessFile raf = null;
        try {
            raf = new OptimizedRandomAccessFile(filename, "r");
            while ((currentLine = raf.readLine()) != null) {
                // skip comments
                if (currentLine.startsWith("%")) {
                    continue;
                }

                if (!inComment && currentLine.startsWith("[")) {
                    if (game_pos == -1) {
                        game_pos = last_pos;
                    }
                    last_pos = raf.getFilePointer();
                    continue;
                }
                if ((!inComment && currentLine.contains("{"))
                        || (inComment && currentLine.contains("}"))) {
                    inComment = currentLine.lastIndexOf("{") > currentLine.lastIndexOf("}");
                }

                if (game_pos != -1) {
                    offsets.add(game_pos);
                    game_pos = -1;
                }

                last_pos = raf.getFilePointer();
            }
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
        return offsets;
    }

    public HashMap<String, String> readSingleHeader(String filename, long offset) {

        OptimizedRandomAccessFile raf = null;
        HashMap<String, String> header = null;
        try {
            raf = new OptimizedRandomAccessFile(filename, "r");
            header = readSingleHeader(raf, offset);
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
        if (header == null) {
            return new HashMap<String, String>();
        } else {
            return header;
        }
    }


    public HashMap<String, String> readSingleHeader(OptimizedRandomAccessFile raf, long offset) {

        HashMap<String, String> header = new HashMap<>();

        String currentLine = "";

        boolean continueSearch = true;
        boolean foundHeader = false;

        try {
            raf.seek(offset);
            while ((currentLine = raf.readLine()) != null) {
                // skip comments
                if (currentLine.startsWith("%")) {
                    continue;
                }

                if (currentLine.startsWith("[")) {

                    foundHeader = true;
                    //
                    if (currentLine.length() > 4) {
                        int spaceOffset = currentLine.indexOf(' ');
                        int firstQuote = currentLine.indexOf('"');
                        int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                        String tag = currentLine.substring(1, spaceOffset);
                        String value = currentLine.substring(firstQuote + 1, secondQuote);
                        header.put(tag, new String(value.getBytes(StandardCharsets.ISO_8859_1), encoding));
                    }
                } else {
                    if (foundHeader) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return header;
    }


    private boolean isCol(char c) {
        if(c >= 'a' && c <= 'h') {
            return true;
        } else {
            return false;
        }
    }

    private boolean isRow(char c) {
        int row = Character.getNumericValue(c) - 1;
        if(row >= 0 && row <= 7) {
            return true;
        } else {
            return false;
        }
    }

    private void addMove(Move m) {

        GameNode next = new GameNode();

        Board currentBoard = this.currentNode.getBoard();
        try {
            Board childBoard = currentBoard.makeCopy();
            childBoard.apply(m);
            next.setMove(m);
            next.setBoard(childBoard);
            next.setParent(this.currentNode);
            this.currentNode.addVariation(next);
            this.currentNode = next;
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void parsePawnMove() {

        int col = Board.alphaToPos(Character.toUpperCase(currentLine.charAt(currentIdx)));
        Board board = currentNode.getBoard();
        if(currentIdx +1 < currentLine.length()) {
            if(currentLine.charAt(currentIdx+1) == 'x') {
                // after x, next one must be letter denoting column
                // and then digit representing row, like exd4 (white)
                // then parse d, 4, and check whether there is a pawn
                // on e(4-1) = e3
                if(currentIdx+3 < currentLine.length()) {
                    if(this.isCol(currentLine.charAt(currentIdx+2))
                            && this.isRow(currentLine.charAt(currentIdx+3)))
                    {
                        int col_to = Board.alphaToPos(Character.toUpperCase(currentLine.charAt(currentIdx+2)));
                        int row_to = Character.getNumericValue(currentLine.charAt(currentIdx+3)) - 1;
                        int row_from = -1;
                        if(board.turn == CONSTANTS.WHITE && row_to - 1 >= 0
                                && board.isPieceAt(col, row_to - 1)
                            && board.getPieceAt(col, row_to - 1) == CONSTANTS.WHITE_PAWN) {
                            row_from = row_to - 1;
                        } else if(board.turn == CONSTANTS.BLACK && row_to + 1 <= 7
                                && board.isPieceAt(col, row_to + 1)
                                && board.getPieceAt(col, row_to + 1) == CONSTANTS.BLACK_PAWN) {
                            row_from = row_to + 1;
                        }
                        if(row_from >= 0 && row_from <= 7) {
                            // check whether this is a promotion, i.e. exd8=Q
                            if(currentIdx+5 < currentLine.length() && currentLine.charAt(currentIdx+4) == '=' &&
                                    (currentLine.charAt(currentIdx+5) == 'R' ||
                                    currentLine.charAt(currentIdx+5) == 'B' ||
                                    currentLine.charAt(currentIdx+5) == 'N' ||
                                    currentLine.charAt(currentIdx+5) == 'Q')) {
                                Move m = new Move(col, row_from, col_to, row_to, currentLine.charAt(currentIdx+5));
                                this.addMove(m);
                                currentIdx += 6;
                                return;
                            } else { // just a normal move, like exd4
                                Move m = new Move(col, row_from, col_to, row_to);
                                this.addMove(m);
                                currentIdx += 4;
                                return;
                            }
                        } else {
                            currentIdx += 4;
                            return;
                        }
                    } else {
                        currentIdx += 4;
                        return;
                    }
                } else {
                    currentIdx += 2;
                    return;
                }
            } else { // only other case: must be a number
                if(this.isRow(currentLine.charAt(currentIdx+1))) {
                    int row = Character.getNumericValue(currentLine.charAt(currentIdx+1)) - 1;
                    int from_row = -1;
                    if(board.turn == CONSTANTS.WHITE) {
                        for(int row_i = row - 1;row_i>= 1;row_i--) {
                            if(board.isPieceAt(col, row_i) && board.getPieceAt(col,row_i) == CONSTANTS.WHITE_PAWN) {
                                from_row = row_i;
                                break;
                            }
                        }
                    } else {
                        for(int row_i = row + 1;row_i<= 7;row_i++) {
                            if(board.isPieceAt(col, row_i) && board.getPieceAt(col,row_i) == CONSTANTS.BLACK_PAWN) {
                                from_row = row_i;
                                break;
                            }
                        }
                    }
                    if(from_row >= 0) { // means we found a from square
                        // check whether this is a promotion
                        if(currentIdx+3 < currentLine.length() && currentLine.charAt(currentIdx+2) == '=' &&
                                (currentLine.charAt(currentIdx+3) == 'R' ||
                                currentLine.charAt(currentIdx+3) == 'B' ||
                                currentLine.charAt(currentIdx+3) == 'N' ||
                                currentLine.charAt(currentIdx+3) == 'Q')) {
                            Move m = new Move(col, from_row, col, row, currentLine.charAt(currentIdx+3));
                            this.addMove(m);
                            currentIdx += 4;
                            return;
                        } else { // not a promotion, just a standard pawn move
                            Move m = new Move(col, from_row, col, row);
                            this.addMove(m);
                            currentIdx += 2;
                            return;
                        }
                    } else {
                        currentIdx+=2;
                        return;
                    }
                } else {
                    currentIdx+=2;
                    return;
                }
            }
        }
        currentIdx += 2;
        return;
    }

    private void createPieceMove(int pieceType, int to_col, int to_row) {

        Board board = currentNode.getBoard();
        int to_internal = board.xyToInternal(to_col, to_row);
        ArrayList<Move> pseudos = board.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, to_internal, pieceType, false, board.turn);
        if (pseudos.size() == 1) {
            Move m = pseudos.get(0);
            this.addMove(m);
        } else {
            ArrayList<Move> legals = board.legalsFromPseudos(pseudos);
            if (legals.size() == 1) {
                Move m = legals.get(0);
                this.addMove(m);
            }
        }
    }

    private void createPieceMove(int pieceType, int to_col, int to_row, char qc_from_col) {

        Board board = currentNode.getBoard();
        int from_col = Board.alphaToPos(Character.toUpperCase(qc_from_col));
        int to_internal = board.xyToInternal(to_col, to_row);
        ArrayList<Move> pseudos = board.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, to_internal, pieceType, false, board.turn);
        ArrayList<Move> filter = new ArrayList<>();
        for(int i=0;i<pseudos.size();i++) {
            Move m = pseudos.get(i);
            if((m.from % 10) - 1 == from_col) {
                filter.add(m);
            }
        }
        if(filter.size() == 1) {
            Move m = filter.get(0);
            this.addMove(m);
        } else {
            ArrayList<Move> legals = board.legalsFromPseudos(pseudos);
            if(legals.size() == 1) {
                Move m = legals.get(0);
                this.addMove(m);
            }
        }
    }

    private void createPieceMove(int pieceType, int to_col, int to_row, int from_row) {

        Board board = currentNode.getBoard();
        int to_internal = board.xyToInternal(to_col, to_row);
        ArrayList<Move> pseudos = board.pseudoLegalMoves(CONSTANTS.ANY_SQUARE, to_internal, pieceType, false, board.turn);
        ArrayList<Move> filter = new ArrayList<>();
        for(int i=0;i<pseudos.size();i++) {
            Move m = pseudos.get(i);
            if((m.from / 10) - 2 == from_row) {
                filter.add(m);
            }
        }
        if(filter.size() == 1) {
            Move m = filter.get(0);
            this.addMove(m);
        } else {
            ArrayList<Move> legals = board.legalsFromPseudos(pseudos);
            if(legals.size() == 1) {
                Move m = legals.get(0);
                this.addMove(m);
            }
        }
    }

    private void parsePieceMove(int pieceType) {

        // we have a piece move like "Qxe4" where index points to Q
        // First move idx after piece symbol, i.e. to ">x<e4"
        currentIdx+=1;
        if(currentIdx < currentLine.length() && currentLine.charAt(currentIdx) == 'x') {
            currentIdx+=1;
        }
        if(currentIdx < currentLine.length()) {
            if(this.isCol(currentLine.charAt(currentIdx))) {
                //Qe? or Qxe?, now either digit must follow (Qe4 / Qxe4)
                //or we have a disambiguation (Qee5, Qexe5)
                if(currentIdx+1 < currentLine.length()) {
                    if(this.isRow(currentLine.charAt(currentIdx+1))) {
                        int to_col = Board.alphaToPos(Character.toUpperCase(currentLine.charAt(currentIdx)));
                        int to_row = Character.getNumericValue(currentLine.charAt(currentIdx+1)) - 1;
                        currentIdx+=2;
                        // standard move, i.e. Qe4
                        try {
                            createPieceMove(pieceType, to_col, to_row);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // fix: skip x if we have Qexe5
                        int skipForTake = 0;
                        if(currentLine.charAt(currentIdx+1) == 'x' && currentIdx + 2 < currentLine.length()) {
                            skipForTake = 1;
                            currentIdx+=1;
                        }
                        // fix end
                        if(this.isCol(currentLine.charAt(currentIdx+1))) {
                            // we have a disambiguation, that should be resolved by
                            // the column denoted in the san, here in @line[idx]
                            int to_col = Board.alphaToPos(Character.toUpperCase(currentLine.charAt(currentIdx+1)));
                            if(currentIdx+2 < currentLine.length() && this.isRow(currentLine.charAt(currentIdx+2))) {
                                int to_row = Character.getNumericValue(currentLine.charAt(currentIdx+2)) - 1;
                                // move w/ disambig on col, i.e. Qee4
                                // provide line[idx] to cratePieceMove to resolve disamb.
                                currentIdx+=3;
                                try {
                                    createPieceMove(pieceType, to_col, to_row, currentLine.charAt(currentIdx - (3 + skipForTake)));
                                } catch (IllegalArgumentException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                currentIdx+=4;
                                return;
                            }
                        } else {
                            currentIdx+=3;
                            return;
                        }
                    }
                } else {
                    currentIdx+=2;
                    return;
                }
            } else {
                if(currentIdx+1 < currentLine.length() && this.isRow(currentLine.charAt(currentIdx))) {
                    // we have a move with disamb, e.g. Q4xe5 or Q4e5
                    int from_row = Character.getNumericValue(currentLine.charAt(currentIdx))- 1;
                    if(currentLine.charAt(currentIdx+1) == 'x') {
                        currentIdx+=1;
                    }
                    if(currentIdx+2 < currentLine.length() && this.isCol(currentLine.charAt(currentIdx+1))
                            && this.isRow(currentLine.charAt(currentIdx+2)))
                    {
                        int to_col = Board.alphaToPos(Character.toUpperCase(currentLine.charAt(currentIdx+1)));
                        int to_row = Character.getNumericValue(currentLine.charAt(currentIdx+2)) - 1;
                        // parse the ambig move
                        currentIdx+=3;
                        try {
                            createPieceMove(pieceType, to_col, to_row, from_row);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    } else {
                        currentIdx+=3;
                        return;
                    }
                } else {
                    currentIdx+=2;
                    return;
                }
            }
        } else {
            currentIdx+=2;
            return;
        }
    }


    private void parseCastleMove() {

        int lineSize = currentLine.length();
        if(currentIdx+4 < lineSize && ( currentLine.substring(currentIdx,currentIdx+5).equals("O-O-O")
                || currentLine.substring(currentIdx,currentIdx+5).equals("0-0-0")) )
        {
            if(currentNode.getBoard().turn == CONSTANTS.WHITE) {
                Move m = new Move(CONSTANTS.E1,CONSTANTS.C1);
                this.addMove(m);
                currentIdx += 5;
                return;
            } else {
                Move m = new Move(CONSTANTS.E8,CONSTANTS.C8);
                this.addMove(m);
                currentIdx += 5;
                return;
            }
        }
        if(currentIdx+2 < lineSize && ( currentLine.substring(currentIdx,currentIdx+3).equals("O-O"))) {  // || line.mid(idx,3) == QString::fromLatin1("0-0"))) {
            if(currentNode.getBoard().turn == CONSTANTS.WHITE) {
                Move m = new Move(CONSTANTS.E1,CONSTANTS.G1);
                this.addMove(m);
                currentIdx += 3;
                return;
            } else {
                Move m = new Move(CONSTANTS.E8,CONSTANTS.G8);
                this.addMove(m);
                currentIdx += 3;
                return;
            }
        }
        currentIdx+=1;
        return;
    }


    private void parseNAG() {

        int lineSize = currentLine.length();

        if(currentLine.charAt(currentIdx) == '$') {
            int idx_end = currentIdx;
            //while(idx_end < lineSize && currentLine.charAt(idx_end) != ' ') {
            while(idx_end < lineSize && (currentLine.charAt(idx_end) == '$'
                    || (currentLine.charAt(idx_end) >= '0' && currentLine.charAt(idx_end) <= '9'))) {
                idx_end++;
            }
            if(idx_end+1 > currentIdx) {
                boolean ok;
                try {
                    int nr = Integer.parseInt(currentLine.substring(currentIdx + 1, idx_end));
                    currentNode.addNag(nr);
                    currentIdx = idx_end;
                } catch(NumberFormatException e) {
                    currentIdx += 1;
                }
            } else {
                currentIdx += 1;
            }
            return;
        }
        //if(currentIdx+1 < lineSize && currentLine.substring(currentIdx,currentIdx+2).equals("??")) {
        if(currentLine.startsWith("??", currentIdx)) {
            currentNode.addNag(CONSTANTS.NAG_BLUNDER);
            currentIdx += 3;
            return;
        }
        //if(currentIdx+1 < lineSize && currentLine.substring(currentIdx,2).equals("!!")) {
        if(currentLine.startsWith("!!", currentIdx)) {
            currentNode.addNag(CONSTANTS.NAG_BRILLIANT_MOVE);
            currentIdx += 3;
            return;
        }
        //if(currentIdx+1 < lineSize && currentLine.substring(currentIdx,2).equals("!?")) {
        if(currentLine.startsWith("!?", currentIdx)) {
            currentNode.addNag(CONSTANTS.NAG_SPECULATIVE_MOVE);
            currentIdx += 3;
            return;
        }
        //if(currentIdx+1 < lineSize && currentLine.substring(currentIdx,2).equals("?!")) {
        if(currentLine.startsWith("?!", currentIdx)) {
            currentNode.addNag(CONSTANTS.NAG_DUBIOUS_MOVE);
            currentIdx += 3;
            return;
        }
        if(currentLine.charAt(currentIdx) == '?') {
            currentNode.addNag(CONSTANTS.NAG_MISTAKE);
            currentIdx += 2;
            return;
        }
        if(currentLine.charAt(currentIdx) == '!') {
            currentNode.addNag(CONSTANTS.NAG_GOOD_MOVE);
            currentIdx += 2;
            return;
        }
    }

    private int getNetxtToken() {

        int lineSize = currentLine.length();
        while(currentIdx < lineSize) {
            char ci = currentLine.charAt(currentIdx);
            if(ci == ' ' || ci == '.') {
                currentIdx += 1;
                continue;
            }
            if(ci >= '0' && ci <= '9') {
                if(ci == '1') {
                    if(currentIdx+1 < lineSize) {
                        if(currentLine.charAt(currentIdx+1) == '-') {
                            if(currentIdx+2 < lineSize && currentLine.charAt(currentIdx+2) == '0') {
                                return CONSTANTS.TKN_RES_WHITE_WIN;
                            }
                        }
                        if(currentIdx+2 < lineSize && currentLine.charAt(currentIdx+2) == '/') {
                            if(currentIdx+6 < lineSize && currentLine.substring(currentIdx,currentIdx+7).equals("1/2-1/2")) {
                                return CONSTANTS.TKN_RES_DRAW;
                            }
                        }
                    }
                }
                // irregular castling like 0-0 or 0-0-0
                if(ci == '0') {
                    if(currentIdx+1 < lineSize && currentLine.charAt(currentIdx+1) == '-') {
                        if(currentIdx+2 < lineSize && currentLine.charAt(currentIdx+2) == '1') {
                            return CONSTANTS.TKN_RES_BLACK_WIN;
                        } else {
                            if(currentIdx+2 < lineSize && currentLine.charAt(currentIdx+2) == '0') {
                                return CONSTANTS.TKN_CASTLE;
                            }
                        }
                    }
                }
                // none of the above -> move number, just continue
                currentIdx += 1;
                continue;
            }
            if(ci >= 'a' && ci <= 'h') {
                return CONSTANTS.TKN_PAWN_MOVE;
            }
            if(ci == 'O') {
                return CONSTANTS.TKN_CASTLE;
            }
            if(ci == 'R') {
                return CONSTANTS.TKN_ROOK_MOVE;
            }
            if(ci == 'N') {
                return CONSTANTS.TKN_KNIGHT_MOVE;
            }
            if(ci == 'B') {
                return CONSTANTS.TKN_BISHOP_MOVE;
            }
            if(ci == 'Q') {
                return CONSTANTS.TKN_QUEEN_MOVE;
            }
            if(ci == 'K') {
                return CONSTANTS.TKN_KING_MOVE;
            }
            if(ci == '+') {
                return CONSTANTS.TKN_CHECK;
            }
            if(ci == '(') {
                return CONSTANTS.TKN_OPEN_VARIATION;
            }
            if(ci == ')') {
                return CONSTANTS.TKN_CLOSE_VARIATION;
            }
            if(ci == '$' || ci == '!' || ci == '?') {
                return CONSTANTS.TKN_NAG;
            }
            if(ci == '{') {
                return CONSTANTS.TKN_OPEN_COMMENT;
            }
            if(ci == '*') {
                return CONSTANTS.TKN_RES_UNDEFINED;
            }
            if(ci == '-') {
                if(currentIdx + 1 < lineSize && currentLine.charAt(currentIdx+1) == '-') {
                    return CONSTANTS.TKN_NULL_MOVE;
                }
            }
            // if none of the above match, try to continue until we
            // find another usable token
            currentIdx += 1;
        }
        return CONSTANTS.TKN_EOL;
    }

    public Game readGame(OptimizedRandomAccessFile raf) {

        currentLine = "";
        currentIdx = 0;

        String startingFen = "";

        Game g = new Game();

        gameStack.clear();
        gameStack.push(g.getRootNode());
        currentNode = g.getRootNode();
        Board rootBoard = new Board(true);
        currentNode.setBoard(rootBoard);

        currentLine = null;

        try {
            while ((currentLine = raf.readLine()) != null) {
                if (currentLine.startsWith("%") || currentLine.isEmpty()) {
                    continue;
                }

                if (currentLine.startsWith("[")) {
                    if (currentLine.length() > 4) {
                        int spaceOffset = currentLine.indexOf(' ');
                        int firstQuote = currentLine.indexOf('"');
                        int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                        if(spaceOffset > 1 && firstQuote >= 0 && secondQuote >= 0 && secondQuote > (firstQuote+1)) {
                            String tag = currentLine.substring(1, spaceOffset);
                            String value = currentLine.substring(firstQuote + 1, secondQuote);
                            if (tag.equals("FEN")) {
                                startingFen = value;
                            } else {
                                g.setHeader(tag, new String(value.getBytes(StandardCharsets.ISO_8859_1), encoding));
                            }
                        }
                    }
                    continue;
                } else {
                    break; // finished reading header
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return g;
        }
        // now the actual game should start.
        // try to set the starting fen, if it exists
        if (!startingFen.isEmpty()) {
            try {
                Board boardFen = new Board(startingFen);
                if (!boardFen.isConsistent()) {
                    return g;
                } else {
                    currentNode.setBoard(boardFen);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // we should now have a header, seek first non-empty line
        // if we already reached it, just skip this part
        if(currentLine.trim().isEmpty()) {
            try {
                while (true) {
                    currentLine = raf.readLine();
                    if (currentLine == null) { //reached eof
                        return g;
                    }
                    if (currentLine.trim().isEmpty()) {
                        continue;
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return g;
            }
        }

        boolean firstLine = true;

        try {
            while (true) {
                // if we are at the first line after skipping
                // all the empty ones, don't read another line
                // otherwise, call readLine
                if (!firstLine) {
                    currentLine = raf.readLine();
                } else {
                    firstLine = false;
                }
                if (currentLine == null || currentLine.isEmpty()) {
                    return g;
                }
                if (currentLine.startsWith("%")) {
                    continue;
                }

                currentIdx = 0;
                while (currentIdx < currentLine.length()) {
                    int tkn = getNetxtToken();
                    if (tkn == CONSTANTS.TKN_EOL) {
                        break;
                    }
                    if (tkn == CONSTANTS.TKN_RES_WHITE_WIN) {
                        // 1-0
                        g.setResult(CONSTANTS.RES_WHITE_WINS);
                        currentIdx += 4;
                    }
                    if (tkn == CONSTANTS.TKN_RES_BLACK_WIN) {
                        // 0-1
                        g.setResult(CONSTANTS.RES_BLACK_WINS);
                        currentIdx += 4;
                    }
                    if (tkn == CONSTANTS.TKN_RES_UNDEFINED) {
                        // *
                        g.setResult(CONSTANTS.RES_UNDEF);
                        currentIdx += 2;
                    }
                    if (tkn == CONSTANTS.TKN_RES_DRAW) {
                        // 1/2-1/2
                        g.setResult(CONSTANTS.RES_DRAW);
                        currentIdx += 8;
                    }
                    if (tkn == CONSTANTS.TKN_PAWN_MOVE) {
                        parsePawnMove();
                    }
                    if (tkn == CONSTANTS.TKN_CASTLE) {
                        parseCastleMove();
                    }
                    if (tkn == CONSTANTS.TKN_ROOK_MOVE) {
                        parsePieceMove(CONSTANTS.ROOK);
                    }
                    if (tkn == CONSTANTS.TKN_KNIGHT_MOVE) {
                        parsePieceMove(CONSTANTS.KNIGHT);
                    }
                    if (tkn == CONSTANTS.TKN_BISHOP_MOVE) {
                        parsePieceMove(CONSTANTS.BISHOP);
                    }
                    if (tkn == CONSTANTS.TKN_QUEEN_MOVE) {
                        parsePieceMove(CONSTANTS.QUEEN);
                    }
                    if (tkn == CONSTANTS.TKN_KING_MOVE) {
                        parsePieceMove(CONSTANTS.KING);
                    }
                    if (tkn == CONSTANTS.TKN_CHECK) {
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_NULL_MOVE) {
                        Move m = new Move();
                        m.isNullMove = true;
                        addMove(m);
                        currentIdx += 2;
                    }
                    if (tkn == CONSTANTS.TKN_OPEN_VARIATION) {
                        // put current node on stack, so that we don't forget it.
                        // however if we are at the root node, something
                        // is wrong in the PGN. Silently ignore "(" then
                        if(currentNode != g.getRootNode()) {
                            gameStack.push(currentNode);
                            currentNode = currentNode.getParent();
                        }
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_CLOSE_VARIATION) {
                        // pop from stack. but always leave root
                        if (gameStack.size() > 1) {
                            currentNode = gameStack.pop();
                        }
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_NAG) {
                        parseNAG();
                    }
                    if (tkn == CONSTANTS.TKN_OPEN_COMMENT) {
                        //String rest_of_line = currentLine.substring(currentIdx + 1, currentLine.length() - (currentIdx + 1));
                        String rest_of_line = currentLine.substring(currentIdx + 1, currentLine.length());
                        int end = rest_of_line.indexOf("}");
                        if (end >= 0) {
                            String comment_line = rest_of_line.substring(0, end);
                            currentNode.setComment(new String(comment_line.getBytes(StandardCharsets.ISO_8859_1), encoding));
                            currentIdx = currentIdx + end + 1;
                        } else {
                            // get comment over multiple lines
                            StringBuilder comment_lines = new StringBuilder();
                            //String comment_line = currentLine.substring(currentIdx + 1, currentLine.length() - (currentIdx + 1));
                            String comment_line = currentLine.substring(currentIdx + 1);
                            comment_lines.append(comment_line).append("\n");
                            // we already have the comment part of the current line,
                            // so read-in the next line, and then loop until we find
                            // the end marker "}"
                            //currentLine = raf.readLine();
                            int linesRead = 0;
                            int end_index = -1;
                            while (linesRead < 500) { // what if we never find "}" ??? -> stop after 500 lines
                                currentLine = raf.readLine();
                                if(currentLine == null) {
                                    currentLine = "";
                                    end_index = -1;
                                    break;
                                }
                                linesRead += 1;
                                if (currentLine.contains("}")) {
                                    end_index = currentLine.indexOf("}");
                                    break;
                                } else {
                                    comment_lines.append(currentLine).append("\n");
                                }
                            }
                            if (end_index >= 0) {
                                comment_lines.append(currentLine, 0, end_index);
                                comment_lines.append("\n");
                                currentIdx = end_index + 1;
                            }
                            currentNode.setComment(new String(comment_lines.toString().getBytes(StandardCharsets.ISO_8859_1), encoding));
                        }
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return g;
    }


    public Game readGame(String gameString) {

        String[] lines = gameString.split("\n");

        currentLine = "";
        currentIdx = 0;

        String startingFen = "";

        Game g = new Game();

        gameStack.clear();
        gameStack.push(g.getRootNode());
        currentNode = g.getRootNode();
        Board rootBoard = new Board(true);
        currentNode.setBoard(rootBoard);

        currentLine = null;

        int lineIndex = 0;

            while (lineIndex < lines.length) {
                currentLine = lines[lineIndex];

                if (currentLine.startsWith("%") || currentLine.isEmpty()) {
                    lineIndex += 1;
                    continue;
                }

                if (currentLine.startsWith("[")) {
                    if (currentLine.length() > 4) {
                        int spaceOffset = currentLine.indexOf(' ');
                        int firstQuote = currentLine.indexOf('"');
                        int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                        if(spaceOffset > 1 && firstQuote >= 0 && secondQuote >= 0 && secondQuote > (firstQuote+1)) {
                            String tag = currentLine.substring(1, spaceOffset);
                            String value = currentLine.substring(firstQuote + 1, secondQuote);
                            if (tag.equals("FEN")) {
                                startingFen = value;
                            } else {
                                try {
                                    g.setHeader(tag, new String(value.getBytes(StandardCharsets.ISO_8859_1), encoding));
                                } catch(UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    lineIndex += 1;
                    continue;
                } else {
                    break; // finished reading header
                }
            }


        // now the actual game should start.
        // try to set the starting fen, if it exists
        if (!startingFen.isEmpty()) {
            try {
                Board boardFen = new Board(startingFen);
                if (!boardFen.isConsistent()) {
                    return g;
                } else {
                    currentNode.setBoard(boardFen);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        // we should now have a header, seek first non-empty line
        // if we already reached it, just skip this part
        if(currentLine.trim().isEmpty()) {
            while (lineIndex < lines.length - 1) {
                lineIndex += 1;
                currentLine = lines[lineIndex];
                if (currentLine.trim().isEmpty()) {
                    continue;
                } else {
                    break;
                }
            }
        }

        if(lineIndex >= lines.length) {
            return g;
        }

        boolean firstLine = true;

        try {
            while (true) {
                // if we are at the first line after skipping
                // all the empty ones, don't read another line
                // otherwise, call readLine
                if (!firstLine) {
                    lineIndex += 1;
                } else {
                    firstLine = false;
                }
                if (lineIndex >= lines.length) {
                    return g;
                }
                currentLine = lines[lineIndex];
                if(currentLine.isEmpty()) {
                    return g;
                }
                if (currentLine.startsWith("%")) {
                    lineIndex += 1;
                    continue;
                }

                currentIdx = 0;
                while (currentIdx < currentLine.length()) {
                    int tkn = getNetxtToken();
                    if (tkn == CONSTANTS.TKN_EOL) {
                        break;
                    }
                    if (tkn == CONSTANTS.TKN_RES_WHITE_WIN) {
                        // 1-0
                        g.setResult(CONSTANTS.RES_WHITE_WINS);
                        currentIdx += 4;
                    }
                    if (tkn == CONSTANTS.TKN_RES_BLACK_WIN) {
                        // 0-1
                        g.setResult(CONSTANTS.RES_BLACK_WINS);
                        currentIdx += 4;
                    }
                    if (tkn == CONSTANTS.TKN_RES_UNDEFINED) {
                        // *
                        g.setResult(CONSTANTS.RES_UNDEF);
                        currentIdx += 2;
                    }
                    if (tkn == CONSTANTS.TKN_RES_DRAW) {
                        // 1/2-1/2
                        g.setResult(CONSTANTS.RES_DRAW);
                        currentIdx += 8;
                    }
                    if (tkn == CONSTANTS.TKN_PAWN_MOVE) {
                        parsePawnMove();
                    }
                    if (tkn == CONSTANTS.TKN_CASTLE) {
                        parseCastleMove();
                    }
                    if (tkn == CONSTANTS.TKN_ROOK_MOVE) {
                        parsePieceMove(CONSTANTS.ROOK);
                    }
                    if (tkn == CONSTANTS.TKN_KNIGHT_MOVE) {
                        parsePieceMove(CONSTANTS.KNIGHT);
                    }
                    if (tkn == CONSTANTS.TKN_BISHOP_MOVE) {
                        parsePieceMove(CONSTANTS.BISHOP);
                    }
                    if (tkn == CONSTANTS.TKN_QUEEN_MOVE) {
                        parsePieceMove(CONSTANTS.QUEEN);
                    }
                    if (tkn == CONSTANTS.TKN_KING_MOVE) {
                        parsePieceMove(CONSTANTS.KING);
                    }
                    if (tkn == CONSTANTS.TKN_CHECK) {
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_NULL_MOVE) {
                        Move m = new Move();
                        m.isNullMove = true;
                        addMove(m);
                        currentIdx += 2;
                    }
                    if (tkn == CONSTANTS.TKN_OPEN_VARIATION) {
                        // put current node on stack, so that we don't forget it.
                        // however if we are at the root node, something
                        // is wrong in the PGN. Silently ignore "(" then
                        if(currentNode != g.getRootNode()) {
                            gameStack.push(currentNode);
                            currentNode = currentNode.getParent();
                        }
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_CLOSE_VARIATION) {
                        // pop from stack. but always leave root
                        if (gameStack.size() > 1) {
                            currentNode = gameStack.pop();
                        }
                        currentIdx += 1;
                    }
                    if (tkn == CONSTANTS.TKN_NAG) {
                        parseNAG();
                    }
                    if (tkn == CONSTANTS.TKN_OPEN_COMMENT) {
                        //String rest_of_line = currentLine.substring(currentIdx + 1, currentLine.length() - (currentIdx + 1));
                        String rest_of_line = currentLine.substring(currentIdx + 1, currentLine.length());
                        int end = rest_of_line.indexOf("}");
                        if (end >= 0) {
                            String comment_line = rest_of_line.substring(0, end+1);
                            currentNode.setComment(new String(comment_line.getBytes(StandardCharsets.ISO_8859_1), encoding));
                            currentIdx = currentIdx + end + 1;
                        } else {
                            // get comment over multiple lines
                            StringBuilder comment_lines = new StringBuilder();
                            //String comment_line = currentLine.substring(currentIdx + 1, currentLine.length() - (currentIdx + 1));
                            String comment_line = currentLine.substring(currentIdx + 1);
                            comment_lines.append(comment_line).append("\n");
                            // we already have the comment part of the current line,
                            // so read-in the next line, and then loop until we find
                            // the end marker "}"
                            //currentLine = raf.readLine();
                            int linesRead = 0;
                            int end_index = -1;
                            while (linesRead < 500) { // what if we never find "}" ??? -> stop after 500 lines
                                lineIndex += 1;
                                if(lineIndex >= lines.length) {
                                    currentLine = "";
                                    end_index = -1;
                                    break;
                                }
                                currentLine = lines[lineIndex];
                                linesRead += 1;
                                if (currentLine.contains("}")) {
                                    end_index = currentLine.indexOf("}");
                                    break;
                                } else {
                                    comment_lines.append(currentLine).append("\n");
                                }
                            }
                            if (end_index >= 0) {
                                comment_lines.append(currentLine, 0, end_index);
                                comment_lines.append("\n");
                                currentIdx = end_index + 1;
                            }
                            currentNode.setComment(new String(comment_lines.toString().getBytes(StandardCharsets.ISO_8859_1), encoding));
                        }
                    }
                }


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return g;
    }


}

