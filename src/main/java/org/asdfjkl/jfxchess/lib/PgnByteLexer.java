/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2026 Dominik Klein
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * High-performance, zero-allocation sliding-window byte-level PGN tokenizer.
 * Operates directly on raw ASCII/UTF-8 byte streams using a 64 KB block window,
 * bypassing line-by-line String allocation, character conversions, and regex.
 */
public class PgnByteLexer implements AutoCloseable {

    public static final int BUFFER_SIZE = 65536; // 64 KB block window

    // Token constants
    public static final int TOKEN_EOF = -1;
    public static final int TOKEN_TAG = 1;              // [TagName "TagValue"]
    public static final int TOKEN_MOVE = 2;             // e4, Nf3, Qh4e1+, 0-0, e2e4
    public static final int TOKEN_COMMENT = 3;          // { ... }
    public static final int TOKEN_NAG = 4;              // $1..$255
    public static final int TOKEN_VARIATION_OPEN = 5;   // (
    public static final int TOKEN_VARIATION_CLOSE = 6;  // )
    public static final int TOKEN_OPEN_VARIATION = 5;
    public static final int TOKEN_CLOSE_VARIATION = 6;
    public static final int TOKEN_RESULT = 7;           // 1-0, 0-1, 1/2-1/2, *
    public static final int TOKEN_NULL_MOVE = 8;        // --

    // Underlying source
    private RandomAccessFile raf;
    private InputStream in;
    private byte[] inMemoryData;

    // Sliding window buffer state
    private byte[] buffer;
    private int bufferPos;
    private int bufferLimit;
    private long bufferStartOffset; // offset in file corresponding to buffer[0]
    private int markPos = -1;       // active token start within buffer for boundary preservation
    private boolean eof;

    // Current token state
    private int currentToken = TOKEN_EOF;
    private int tokenOffset;
    private int tokenLength;

    // Tag cache
    private long tagStartFileOffset = 0;
    private String currentTagName = "";
    private String currentTagValue = "";

    // Result cache
    private int currentResult = CONSTANTS.RES_UNDEF;

    // NAG cache
    private int currentNag = 0;

    // Comment cache
    private int commentOffset;
    private int commentLength;
    private byte[] largeCommentBytes;

    // Lookahead / token pushback state
    private boolean tokenPushedBack = false;

    public PgnByteLexer(RandomAccessFile raf) {
        this.raf = raf;
        this.buffer = new byte[BUFFER_SIZE];
        this.bufferPos = 0;
        this.bufferLimit = 0;
        this.bufferStartOffset = 0;
        this.eof = false;
    }

    public PgnByteLexer(File file) throws FileNotFoundException {
        this(new RandomAccessFile(file, "r"));
    }

    public PgnByteLexer(String path) throws FileNotFoundException {
        this(new File(path));
    }

    public PgnByteLexer(InputStream in) {
        this.in = in;
        this.buffer = new byte[BUFFER_SIZE];
        this.bufferPos = 0;
        this.bufferLimit = 0;
        this.bufferStartOffset = 0;
        this.eof = false;
    }

    public PgnByteLexer(byte[] data) {
        this.inMemoryData = data;
        this.buffer = data;
        this.bufferPos = 0;
        this.bufferLimit = data.length;
        this.bufferStartOffset = 0;
        this.eof = true; // no more data to read beyond array
    }

    /**
     * Returns the exact byte offset in the underlying file of the current read pointer.
     */
    public long getFilePointer() {
        return bufferStartOffset + bufferPos;
    }

    public long getOffset() {
        return getFilePointer();
    }

    /**
     * Fast-seeks to an exact byte offset in the file.
     * If the target offset is already within the active 64 KB window, seeks with 0 disk I/O.
     */
    public void seek(long targetOffset) throws IOException {
        tokenPushedBack = false;
        markPos = -1;
        if (inMemoryData != null) {
            bufferPos = (int) Math.min(Math.max(0, targetOffset), bufferLimit);
            return;
        }

        // 1. In-buffer fast seek
        if (targetOffset >= bufferStartOffset && targetOffset < bufferStartOffset + bufferLimit) {
            bufferPos = (int) (targetOffset - bufferStartOffset);
            return;
        }

        // 2. Out-of-buffer seek
        if (raf != null) {
            raf.seek(targetOffset);
            bufferStartOffset = targetOffset;
            bufferPos = 0;
            bufferLimit = 0;
            eof = false;
            ensureBytes(1);
        } else {
            throw new IOException("Seeking not supported on non-seekable InputStream");
        }
    }

    /**
     * Pushes back the current token so the next call to {@link #nextToken()} returns it again.
     */
    public void pushBackToken() {
        tokenPushedBack = true;
    }

    /**
     * Scans and returns the next token type from the PGN stream.
     */
    public int nextToken() throws IOException {
        if (tokenPushedBack) {
            tokenPushedBack = false;
            return currentToken;
        }

        while (true) {
            skipWhitespaceAndLineComments();

            if (!ensureBytes(1)) {
                currentToken = TOKEN_EOF;
                return TOKEN_EOF;
            }

            byte b = buffer[bufferPos];

            // 1. Check for tag: [
            if (b == '[') {
                currentToken = parseTag();
                return currentToken;
            }

            // 2. Check for comments: {
            if (b == '{') {
                currentToken = parseComment();
                return currentToken;
            }

            // 3. Variations: ( and )
            if (b == '(') {
                bufferPos++;
                currentToken = TOKEN_VARIATION_OPEN;
                return currentToken;
            }
            if (b == ')') {
                bufferPos++;
                currentToken = TOKEN_VARIATION_CLOSE;
                return currentToken;
            }

            // 4. NAG: $
            if (b == '$') {
                currentToken = parseNag();
                return currentToken;
            }

            // 5. Result: *
            if (b == '*') {
                bufferPos++;
                currentResult = CONSTANTS.RES_UNDEF;
                currentToken = TOKEN_RESULT;
                return currentToken;
            }

            // 6. Null move: --
            if (b == '-' && hasBytes(2) && buffer[bufferPos + 1] == '-') {
                bufferPos += 2;
                currentToken = TOKEN_NULL_MOVE;
                return currentToken;
            }

            // 7. Results starting with digits or zero-castling:
            // 1-0, 0-1, 1/2-1/2, or move numbers (1., 1..., 24.), or castling (0-0, 0-0-0)
            if (b >= '0' && b <= '9') {
                ensureBytes(8);
                // 1-0
                if (b == '1' && matchSeqWithDelimiter("1-0")) {
                    bufferPos += 3;
                    currentResult = CONSTANTS.RES_WHITE_WINS;
                    currentToken = TOKEN_RESULT;
                    return currentToken;
                }
                // 0-1
                if (b == '0' && matchSeqWithDelimiter("0-1")) {
                    bufferPos += 3;
                    currentResult = CONSTANTS.RES_BLACK_WINS;
                    currentToken = TOKEN_RESULT;
                    return currentToken;
                }
                // 1/2-1/2
                if (b == '1' && (matchSeqWithDelimiter("1/2-1/2") || matchSeqWithDelimiter("1/2 - 1/2") || matchSeqWithDelimiter("1/2"))) {
                    if (matchSeq("1/2 - 1/2")) {
                        bufferPos += 9;
                    } else if (matchSeq("1/2-1/2")) {
                        bufferPos += 7;
                    } else {
                        bufferPos += 3;
                    }
                    currentResult = CONSTANTS.RES_DRAW;
                    currentToken = TOKEN_RESULT;
                    return currentToken;
                }
                // Castling with zeros: 0-0 or 0-0-0
                if (b == '0' && matchSeq("0-0-0")) {
                    tokenOffset = bufferPos;
                    tokenLength = 5;
                    bufferPos += 5;
                    currentToken = TOKEN_MOVE;
                    return currentToken;
                }
                if (b == '0' && matchSeq("0-0") && (bufferLimit - bufferPos <= 3 || buffer[bufferPos + 3] != '-')) {
                    tokenOffset = bufferPos;
                    tokenLength = 3;
                    bufferPos += 3;
                    currentToken = TOKEN_MOVE;
                    return currentToken;
                }

                // If none of the above, it's a move number (e.g. "1.", "12.", "4..."):
                // Skip all digits and dots
                while (ensureBytes(1)) {
                    byte cb = buffer[bufferPos];
                    if ((cb >= '0' && cb <= '9') || cb == '.') {
                        bufferPos++;
                    } else {
                        break;
                    }
                }
                continue; // loop to next token
            }

            // 8. Move token (SAN or coordinate LAN)
            // Starts with a piece (K,Q,R,B,N), pawn column (a-h), or 'O' (O-O)
            markPos = bufferPos;
            tokenOffset = bufferPos;
            while (ensureBytes(1)) {
                byte cb = buffer[bufferPos];
                if (isDelimiter(cb)) {
                    break;
                }
                bufferPos++;
            }

            tokenOffset = markPos;
            tokenLength = bufferPos - tokenOffset;
            markPos = -1;
            if (tokenLength > 0) {
                currentToken = TOKEN_MOVE;
                return currentToken;
            }

            // Unknown or stray byte: advance and retry
            bufferPos++;
        }
    }

    private boolean matchSeq(String s) {
        int len = s.length();
        if (bufferLimit - bufferPos < len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buffer[bufferPos + i] != (byte) s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchSeqWithDelimiter(String s) {
        int len = s.length();
        if (bufferLimit - bufferPos < len) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            if (buffer[bufferPos + i] != (byte) s.charAt(i)) {
                return false;
            }
        }
        if (bufferLimit - bufferPos > len) {
            return isDelimiter(buffer[bufferPos + len]);
        }
        return true;
    }

    private void skipWhitespaceAndLineComments() throws IOException {
        while (ensureBytes(1)) {
            byte b = buffer[bufferPos];
            if (b <= ' ') {
                bufferPos++;
                continue;
            }
            // Line comment starting with ';' or '%'
            if (b == ';' || b == '%') {
                bufferPos++;
                while (ensureBytes(1)) {
                    byte cb = buffer[bufferPos++];
                    if (cb == '\n' || cb == '\r') {
                        break;
                    }
                }
                continue;
            }
            break;
        }
    }

    private int parseTag() throws IOException {
        // bufferPos points to '['
        tagStartFileOffset = bufferStartOffset + bufferPos;
        bufferPos++; // skip '['
        skipSpaces();

        // 1. Tag name
        markPos = bufferPos;
        while (ensureBytes(1)) {
            byte b = buffer[bufferPos];
            if (b <= ' ' || b == '"' || b == ']') {
                break;
            }
            bufferPos++;
        }
        int nameStart = markPos;
        int nameLen = bufferPos - nameStart;
        currentTagName = (nameLen > 0) ? new String(buffer, nameStart, nameLen, StandardCharsets.US_ASCII) : "";
        markPos = -1;

        skipSpaces();

        // 2. Tag value inside quotes
        if (ensureBytes(1) && buffer[bufferPos] == '"') {
            bufferPos++; // skip opening '"'
            markPos = bufferPos;
            boolean hasEscapes = false;

            while (ensureBytes(1)) {
                byte b = buffer[bufferPos];
                if (b == '\\') {
                    hasEscapes = true;
                    bufferPos += 2; // skip escape and escaped char
                    continue;
                }
                if (b == '"') {
                    break;
                }
                bufferPos++;
            }

            int valStart = markPos;
            int valLen = bufferPos - valStart;
            if (!hasEscapes) {
                currentTagValue = (valLen > 0) ? new String(buffer, valStart, valLen, StandardCharsets.UTF_8) : "";
            } else {
                // Decode escape sequences like \"
                StringBuilder sb = new StringBuilder(valLen);
                for (int i = 0; i < valLen; i++) {
                    byte cb = buffer[valStart + i];
                    if (cb == '\\' && i + 1 < valLen) {
                        i++;
                        sb.append((char) buffer[valStart + i]);
                    } else {
                        sb.append((char) cb);
                    }
                }
                currentTagValue = sb.toString();
            }
            markPos = -1;

            if (ensureBytes(1) && buffer[bufferPos] == '"') {
                bufferPos++; // skip closing '"'
            }
        } else {
            currentTagValue = "";
        }

        // 3. Skip to closing ']'
        while (ensureBytes(1)) {
            byte b = buffer[bufferPos++];
            if (b == ']') {
                break;
            }
        }

        return TOKEN_TAG;
    }

    private int parseComment() throws IOException {
        // bufferPos points to '{'
        bufferPos++; // skip '{'
        markPos = bufferPos;
        largeCommentBytes = null;
        ByteArrayOutputStream overflow = null;

        while (true) {
            if (!ensureBytes(1)) {
                // EOF without closing '}'
                commentOffset = markPos;
                commentLength = bufferPos - markPos;
                markPos = -1;
                return TOKEN_COMMENT;
            }

            byte b = buffer[bufferPos++];
            if (b == '}') {
                int end = bufferPos - 1;
                if (overflow == null) {
                    commentOffset = markPos;
                    commentLength = end - markPos;
                } else {
                    overflow.write(buffer, markPos, end - markPos);
                    largeCommentBytes = overflow.toByteArray();
                }
                markPos = -1;
                return TOKEN_COMMENT;
            }

            // If we are at the end of the sliding window and haven't found '}' yet
            if (bufferPos >= bufferLimit) {
                if (overflow == null) {
                    overflow = new ByteArrayOutputStream(1024);
                }
                overflow.write(buffer, markPos, bufferPos - markPos);
                markPos = bufferPos; // next chunk will start after current pos
            }
        }
    }

    private int parseNag() throws IOException {
        // bufferPos points to '$'
        bufferPos++; // skip '$'
        int nag = 0;
        while (ensureBytes(1)) {
            byte b = buffer[bufferPos];
            if (b >= '0' && b <= '9') {
                nag = nag * 10 + (b - '0');
                bufferPos++;
            } else {
                break;
            }
        }
        currentNag = nag;
        return TOKEN_NAG;
    }

    private void skipSpaces() throws IOException {
        while (ensureBytes(1)) {
            byte b = buffer[bufferPos];
            if (b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                bufferPos++;
            } else {
                break;
            }
        }
    }

    public static boolean isDelimiter(byte b) {
        return (b <= ' ' || b == '(' || b == ')' || b == '{' || b == '}' || b == '[' || b == ']' || b == '$' || b == '*' || b == ';');
    }

    /**
     * Fast-skips all movetext, variations, and comments until the start of the next tag '['
     * or EOF. Used during database scanning to bypass move tokenization entirely.
     *
     * @return {@link #TOKEN_TAG} if a tag was found, or {@link #TOKEN_EOF} at end of file
     */
    public int skipToNextTag() throws IOException {
        tokenPushedBack = false;
        while (true) {
            if (!ensureBytes(1)) {
                currentToken = TOKEN_EOF;
                return TOKEN_EOF;
            }

            byte b = buffer[bufferPos];

            // 1. If we found a tag, parse it and return TOKEN_TAG
            if (b == '[') {
                currentToken = parseTag();
                return currentToken;
            }

            // 2. If comment, skip it completely
            if (b == '{') {
                parseComment();
                continue;
            }

            // 3. If line comment (; or %), skip until newline
            if (b == ';' || b == '%') {
                bufferPos++;
                while (ensureBytes(1)) {
                    byte cb = buffer[bufferPos++];
                    if (cb == '\n' || cb == '\r') {
                        break;
                    }
                }
                continue;
            }

            // 4. Fast scan in buffer until next delimiter that might be '[', '{', ';', or '%'
            bufferPos++;
            while (bufferPos < bufferLimit) {
                byte cb = buffer[bufferPos];
                if (cb == '[' || cb == '{' || cb == ';' || cb == '%') {
                    break;
                }
                bufferPos++;
            }
        }
    }

    /**
     * Ensures that at least minBytes are available in the buffer starting at bufferPos.
     * Compacts remaining unread bytes to index 0 (respecting active markPos) and refills if necessary.
     */
    private boolean ensureBytes(int minBytes) throws IOException {
        if (bufferLimit - bufferPos >= minBytes) {
            return true;
        }
        if (eof) {
            return (bufferLimit - bufferPos > 0);
        }

        // Compact remaining bytes to front starting from markPos if active
        int shiftStart = (markPos >= 0) ? markPos : bufferPos;
        int remaining = bufferLimit - shiftStart;
        if (remaining > 0 && shiftStart > 0) {
            System.arraycopy(buffer, shiftStart, buffer, 0, remaining);
        }
        bufferStartOffset += shiftStart;
        bufferPos -= shiftStart;
        if (tokenOffset >= 0) {
            tokenOffset -= shiftStart;
        }
        if (commentOffset >= 0) {
            commentOffset -= shiftStart;
        }
        if (markPos >= 0) {
            markPos = 0;
        }
        bufferLimit = remaining;

        // Ensure buffer has room; expand if a single token exceeds initial buffer size
        int space = buffer.length - bufferLimit;
        if (space == 0 && shiftStart == 0) {
            byte[] newBuf = new byte[buffer.length * 2];
            System.arraycopy(buffer, 0, newBuf, 0, bufferLimit);
            buffer = newBuf;
            space = buffer.length - bufferLimit;
        }

        // Refill from stream
        int bytesRead = 0;
        if (space > 0) {
            if (raf != null) {
                bytesRead = raf.read(buffer, bufferLimit, space);
            } else if (in != null) {
                bytesRead = in.read(buffer, bufferLimit, space);
            } else {
                bytesRead = -1;
            }

            if (bytesRead <= 0) {
                eof = true;
            } else {
                bufferLimit += bytesRead;
            }
        }

        return (bufferLimit - bufferPos >= minBytes) || (bufferLimit - bufferPos > 0);
    }

    private boolean hasBytes(int n) {
        try {
            return ensureBytes(n);
        } catch (IOException e) {
            return false;
        }
    }

    // Getters for token data
    public byte[] getBuffer() {
        return buffer;
    }

    public int getTokenOffset() {
        return tokenOffset;
    }

    public int getTokenLength() {
        return tokenLength;
    }

    public String getTagName() {
        return currentTagName;
    }

    public String getTagValue() {
        return currentTagValue;
    }

    public long getTagStartOffset() {
        return tagStartFileOffset;
    }

    public int getResult() {
        return currentResult;
    }

    public int getNag() {
        return currentNag;
    }

    public String getComment() {
        String s;
        if (largeCommentBytes != null) {
            s = new String(largeCommentBytes, StandardCharsets.UTF_8);
        } else if (commentLength <= 0) {
            return "";
        } else {
            s = new String(buffer, commentOffset, commentLength, StandardCharsets.UTF_8);
        }
        if (s.indexOf('\r') >= 0) {
            s = s.replace("\r\n", "\n").replace('\r', '\n');
        }
        return s;
    }

    public String getTokenString() {
        if (tokenLength <= 0) {
            return "";
        }
        return new String(buffer, tokenOffset, tokenLength, StandardCharsets.US_ASCII);
    }

    @Override
    public void close() throws IOException {
        if (raf != null) {
            raf.close();
            raf = null;
        }
        if (in != null) {
            in.close();
            in = null;
        }
    }
}
