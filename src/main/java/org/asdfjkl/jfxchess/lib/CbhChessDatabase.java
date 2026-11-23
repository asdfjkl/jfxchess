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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Read-only {@link ChessDatabase} implementation for classic ChessBase
 * databases (`.cbh`, `.cbg`, `.cba`, `.cbp`, `.cbt`, `.cbc`, `.cbs`).
 */
public class CbhChessDatabase implements ChessDatabase {

    static final int CBH_HEADER_SIZE = 46;
    static final int CBH_RECORD_SIZE = 46;
    private static final int SCAN_BATCH_RECORDS = 4096;

    private Path basePath;
    private Path cbhPath;
    private Path cbgPath;
    private Path cbaPath;
    private FileChannel cbhChannel;
    private FileChannel cbgChannel;
    private FileChannel cbaChannel;
    private boolean open;

    private final CbhNamebase namebase = new CbhNamebase();
    private final ArrayList<CbhGameInfo> entries = new ArrayList<>();
    private final ArrayList<GameInfo> searchResults = new ArrayList<>();
    private boolean searchActive;
    private final List<ChessDatabaseListener> listeners = new ArrayList<>();
    private long revision;

    public CbhChessDatabase() {
    }

    public CbhChessDatabase(String filename) throws IOException {
        open(filename);
    }

    static Path resolveBasePath(String filename) {
        Objects.requireNonNull(filename, "filename");
        Path p = Path.of(filename).toAbsolutePath().normalize();
        String name = p.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        int strip = 0;
        if (lower.endsWith(".cbh") || lower.endsWith(".cbg") || lower.endsWith(".cba")
                || lower.endsWith(".cbp") || lower.endsWith(".cbt")
                || lower.endsWith(".cbc") || lower.endsWith(".cbs")) {
            strip = 4;
        }
        if (strip > 0) {
            String stem = name.substring(0, name.length() - strip);
            Path parent = p.getParent();
            if (parent != null) {
                return parent.resolve(stem);
            }
            return Path.of(stem);
        }
        return p;
    }

    static Path resolveExistingSibling(Path base, String lowerExt) {
        Path p = Path.of(base.toString() + lowerExt);
        if (Files.exists(p)) {
            return p;
        }
        Path upper = Path.of(base.toString() + lowerExt.toUpperCase(Locale.ROOT));
        if (Files.exists(upper)) {
            return upper;
        }
        return p;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void open(String filename) throws IOException {
        close();
        Path base = resolveBasePath(filename);
        Path cbh = resolveExistingSibling(base, ".cbh");
        if (!Files.exists(cbh)) {
            throw new FileNotFoundException("CBH index file not found: " + cbh);
        }
        Path cbg = resolveExistingSibling(base, ".cbg");
        Path cba = resolveExistingSibling(base, ".cba");

        this.basePath = base;
        this.cbhPath = cbh;
        this.cbgPath = cbg;
        this.cbaPath = cba;

        this.namebase.load(base);
        this.cbhChannel = FileChannel.open(cbh, StandardOpenOption.READ);
        if (Files.exists(cbg)) {
            this.cbgChannel = FileChannel.open(cbg, StandardOpenOption.READ);
        }
        if (Files.exists(cba)) {
            this.cbaChannel = FileChannel.open(cba, StandardOpenOption.READ);
        }
        this.open = true;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.revision = 0;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void createNew(String filename) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        IOException firstError = null;
        FileChannel[] channels = new FileChannel[]{cbhChannel, cbgChannel, cbaChannel};
        cbhChannel = null;
        cbgChannel = null;
        cbaChannel = null;
        for (FileChannel ch : channels) {
            if (ch != null) {
                try {
                    ch.close();
                } catch (IOException e) {
                    if (firstError == null) {
                        firstError = e;
                    }
                }
            }
        }
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.namebase.clear();
        if (firstError != null) {
            throw firstError;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getFilename() {
        if (cbhPath == null) {
            return "";
        }
        return cbhPath.toString();
    }

    @Override
    public Path getPath() {
        return cbhPath;
    }

    Path getBasePath() {
        return basePath;
    }

    Path getCbhPath() {
        return cbhPath;
    }

    Path getCbgPath() {
        return cbgPath;
    }

    Path getCbaPath() {
        return cbaPath;
    }

    CbhNamebase getNamebase() {
        return namebase;
    }

    @Override
    public void scanGames() {
        scanGames(null);
    }

    @Override
    public void scanGames(ProgressListener listener) {
        if (!open || cbhChannel == null) {
            return;
        }
        entries.clear();
        searchResults.clear();
        searchActive = false;

        try {
            long fileSize = cbhChannel.size();
            if (fileSize <= CBH_HEADER_SIZE) {
                if (listener != null && !listener.isCancelled()) {
                    listener.onProgress(100);
                }
                revision++;
                publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.INDEX_REBUILT, this, revision, null));
                return;
            }
            int totalRecords = (int) ((fileSize - CBH_HEADER_SIZE) / CBH_RECORD_SIZE);
            entries.ensureCapacity(totalRecords);

            byte[] buf = new byte[SCAN_BATCH_RECORDS * CBH_RECORD_SIZE];
            long offset = CBH_HEADER_SIZE;
            int rawIndex = 0;

            while (rawIndex < totalRecords) {
                if (listener != null && listener.isCancelled()) {
                    break;
                }
                int batchCount = Math.min(SCAN_BATCH_RECORDS, totalRecords - rawIndex);
                int bytesWanted = batchCount * CBH_RECORD_SIZE;
                int bytesRead = readUpToAt(cbhChannel, offset, buf, bytesWanted);
                int recordsRead = bytesRead / CBH_RECORD_SIZE;
                if (recordsRead <= 0) {
                    break;
                }

                for (int i = 0; i < recordsRead; i++) {
                    int off = i * CBH_RECORD_SIZE;
                    int flags = buf[off] & 0xFF;
                    // Skip guiding texts (0x02), deleted records (0x80), and non-game records (!(flags & 0x01))
                    if ((flags & 0x01) != 0 && (flags & 0x02) == 0 && (flags & 0x80) == 0) {
                        int cbgOffset = CbCommon.readI32BE(buf, off + 0x01);
                        int cbaOffset = CbCommon.readI32BE(buf, off + 0x05);
                        int whiteId = CbCommon.readU24BE(buf, off + 0x09);
                        int blackId = CbCommon.readU24BE(buf, off + 0x0C);
                        int tournamentId = CbCommon.readU24BE(buf, off + 0x0F);
                        int packedDate = CbCommon.readU24BE(buf, off + 0x18);
                        int resultByte = buf[off + 0x1B] & 0xFF;
                        int round = buf[off + 0x1D] & 0xFF;
                        int subround = buf[off + 0x1E] & 0xFF;
                        int whiteElo = CbCommon.readU16BE(buf, off + 0x1F);
                        int blackElo = CbCommon.readU16BE(buf, off + 0x21);
                        int ecoRaw = CbCommon.readU16BE(buf, off + 0x23);

                        entries.add(new CbhGameInfo(
                                namebase,
                                rawIndex + i,
                                cbgOffset,
                                cbaOffset,
                                whiteId,
                                blackId,
                                tournamentId,
                                packedDate,
                                resultByte,
                                round,
                                subround,
                                whiteElo,
                                blackElo,
                                ecoRaw
                        ));
                    }
                }

                rawIndex += recordsRead;
                offset += (long) recordsRead * CBH_RECORD_SIZE;

                if (listener != null && totalRecords > 0) {
                    int pct = (int) (((long) rawIndex * 100L) / totalRecords);
                    listener.onProgress(Math.min(99, pct));
                }
            }

            if (listener != null && !listener.isCancelled()) {
                listener.onProgress(100);
            }
            revision++;
            publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.INDEX_REBUILT, this, revision, null));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan CBH database: " + e.getMessage(), e);
        }
    }

    @Override
    public ArrayList<GameInfo> getIndex() {
        return new ArrayList<>(entries);
    }

    @Override
    public ArrayList<GameInfo> getSearchResults() {
        return new ArrayList<>(searchResults);
    }

    @Override
    public boolean isSearchActive() {
        return searchActive;
    }

    @Override
    public void resetSearch() {
        searchResults.clear();
        searchActive = false;
    }

    @Override
    public Game loadGame(GameInfo info) throws IOException {
        if (!open) {
            throw new IOException("CBH database is not open");
        }
        if (info == null) {
            throw new IllegalArgumentException("GameInfo cannot be null");
        }
        CbhGameInfo cgi;
        if (info instanceof CbhGameInfo c) {
            cgi = c;
        } else {
            cgi = findEntryById(info.getId());
            if (cgi == null) {
                throw new IOException("Game not found in CBH database: " + info.getId());
            }
        }

        Game game = new Game();
        game.setHeader("Event", cgi.getEvent());
        game.setHeader("Site", cgi.getSite());
        game.setHeader("Date", cgi.getDate());
        game.setHeader("Round", cgi.getRound());
        game.setHeader("White", cgi.getWhite());
        game.setHeader("Black", cgi.getBlack());
        game.setHeader("Result", cgi.getResult());
        String eco = cgi.getEco();
        if (!eco.isEmpty()) {
            game.setHeader("ECO", eco);
        }
        String whiteElo = cgi.getWhiteElo();
        if (!whiteElo.isEmpty()) {
            game.setHeader("WhiteElo", whiteElo);
        }
        String blackElo = cgi.getBlackElo();
        if (!blackElo.isEmpty()) {
            game.setHeader("BlackElo", blackElo);
        }
        game.setResult(CbCommon.decodeResultConstant(cgi.getResultByte()));

        if (cbhChannel != null) {
            long recOffset = CBH_HEADER_SIZE + ((long) cgi.getRawIndex() * CBH_RECORD_SIZE);
            byte[] ids = new byte[6];
            if (readUpToAt(cbhChannel, recOffset + 0x12, ids, 6) == 6) {
                int annotatorId = CbCommon.readU24BE(ids, 0);
                int sourceId = CbCommon.readU24BE(ids, 3);
                String annotator = namebase.getAnnotator(annotatorId);
                if (!annotator.isEmpty()) {
                    game.setHeader("Annotator", annotator);
                }
                String source = namebase.getSource(sourceId);
                if (!source.isEmpty()) {
                    game.setHeader("Source", source);
                }
            }
        }

        Map<Integer, MoveAnnotation> annotations = Collections.emptyMap();
        if (cgi.getCbaOffset() > 0 && cbaChannel != null) {
            annotations = readCbaBlock(cbaChannel, cgi.getCbaOffset());
        }

        if (cbgChannel != null && cgi.getCbgOffset() >= 26L) {
            decodeCbgGame(cbgChannel, cgi.getCbgOffset(), game, annotations);
        } else {
            Board rootBoard = new Board(true);
            game.getRootNode().setBoard(rootBoard);
            applyAnnotationToNode(game.getRootNode(), annotations.get(0));
        }

        return game;
    }

    private CbhGameInfo findEntryById(UUID id) {
        if (id == null) {
            return null;
        }
        for (CbhGameInfo entry : entries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    static final class MoveAnnotation {
        String preComment = "";
        int prePriority = 0;
        String postComment = "";
        int postPriority = 0;
        int moveNag = 0;
        int evalNag = 0;
        int prefixNag = 0;
        ArrayList<ColoredField> coloredFields;
        ArrayList<Arrow> arrows;

        void setPreText(String text, int priority) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (priority > prePriority) {
                preComment = text.trim();
                prePriority = priority;
            } else if (priority == prePriority) {
                preComment = CbCommon.appendCommentPart(preComment, text);
            }
        }

        void setPostText(String text, int priority) {
            if (text == null || text.isEmpty()) {
                return;
            }
            if (priority > postPriority) {
                postComment = text.trim();
                postPriority = priority;
            } else if (priority == postPriority) {
                postComment = CbCommon.appendCommentPart(postComment, text);
            }
        }

        String buildCombinedComment() {
            if (preComment.isEmpty()) {
                return postComment;
            }
            if (postComment.isEmpty()) {
                return preComment;
            }
            return preComment + " " + postComment;
        }
    }

    static Map<Integer, MoveAnnotation> readCbaBlock(FileChannel cbaChannel, long cbaOffset) throws IOException {
        long fileSize = cbaChannel.size();
        if (cbaOffset <= 0 || cbaOffset + 14 > fileSize) {
            return Collections.emptyMap();
        }
        byte[] hdr = new byte[14];
        if (readUpToAt(cbaChannel, cbaOffset, hdr, 14) < 14) {
            return Collections.emptyMap();
        }
        int totalLen = CbCommon.readI32BE(hdr, 10);
        if (totalLen <= 14 || totalLen > 16_777_216) {
            return Collections.emptyMap();
        }
        int bodyLen = (int) Math.min((long) totalLen - 14L, fileSize - (cbaOffset + 14L));
        if (bodyLen <= 0) {
            return Collections.emptyMap();
        }
        byte[] data = new byte[bodyLen];
        int actual = readUpToAt(cbaChannel, cbaOffset + 14L, data, bodyLen);
        Map<Integer, MoveAnnotation> map = new HashMap<>();

        int p = 0;
        while (p + 6 <= actual) {
            int mvIdx = CbCommon.readU24BE(data, p);
            if (mvIdx == 0xFFFFFF) {
                mvIdx = 0;
            }
            int chunkType = data[p + 3] & 0xFF;
            int chunkLen = CbCommon.readU16BE(data, p + 4);
            if (chunkLen < 6 || p + chunkLen > actual) {
                break;
            }

            if (chunkType == 0x02 || chunkType == 0x82) {
                if (chunkLen >= 8) {
                    int lang = data[p + 7] & 0xFF;
                    String text = CbCommon.decodeCommentText(data, p + 8, chunkLen - 8, true);
                    if (!text.isEmpty()) {
                        MoveAnnotation ma = map.computeIfAbsent(mvIdx, k -> new MoveAnnotation());
                        int prio = CbCommon.cbaLanguagePriority(lang);
                        if (chunkType == 0x82) {
                            ma.setPreText(text, prio);
                        } else {
                            ma.setPostText(text, prio);
                        }
                    }
                }
            } else if (chunkType == 0x03) {
                int payLen = chunkLen - 6;
                if (payLen >= 1) {
                    MoveAnnotation ma = map.computeIfAbsent(mvIdx, k -> new MoveAnnotation());
                    int mNag = CbCommon.mapMoveNag(data[p + 6] & 0xFF);
                    if (mNag > 0) {
                        ma.moveNag = mNag;
                    }
                    if (payLen >= 2) {
                        int eNag = CbCommon.mapEvalNag(data[p + 7] & 0xFF);
                        if (eNag > 0) {
                            ma.evalNag = eNag;
                        }
                    }
                    if (payLen >= 3) {
                        int pNag = CbCommon.mapPrefixNag(data[p + 8] & 0xFF);
                        if (pNag > 0) {
                            ma.prefixNag = pNag;
                        }
                    }
                }
            } else if (chunkType == 0x04) {
                int payLen = chunkLen - 6;
                MoveAnnotation ma = map.computeIfAbsent(mvIdx, k -> new MoveAnnotation());
                if (ma.coloredFields == null) {
                    ma.coloredFields = new ArrayList<>();
                }
                for (int q = 0; q + 1 < payLen; q += 2) {
                    int sq1 = data[p + 6 + q + 1] & 0xFF;
                    if (sq1 >= 1 && sq1 <= 64) {
                        int sq0 = sq1 - 1;
                        ColoredField cf = new ColoredField();
                        cf.x = sq0 / 8;
                        cf.y = sq0 % 8;
                        if (!ma.coloredFields.contains(cf)) {
                            ma.coloredFields.add(cf);
                        }
                    }
                }
            } else if (chunkType == 0x05) {
                int payLen = chunkLen - 6;
                MoveAnnotation ma = map.computeIfAbsent(mvIdx, k -> new MoveAnnotation());
                if (ma.arrows == null) {
                    ma.arrows = new ArrayList<>();
                }
                for (int q = 0; q + 2 < payLen; q += 3) {
                    int fromSq1 = data[p + 6 + q + 1] & 0xFF;
                    int toSq1 = data[p + 6 + q + 2] & 0xFF;
                    if (fromSq1 >= 1 && fromSq1 <= 64 && toSq1 >= 1 && toSq1 <= 64) {
                        int f0 = fromSq1 - 1;
                        int t0 = toSq1 - 1;
                        Arrow a = new Arrow();
                        a.xFrom = f0 / 8;
                        a.yFrom = f0 % 8;
                        a.xTo = t0 / 8;
                        a.yTo = t0 % 8;
                        if (!ma.arrows.contains(a)) {
                            ma.arrows.add(a);
                        }
                    }
                }
            }
            p += chunkLen;
        }
        return map;
    }

    static void applyAnnotationToNode(GameNode node, MoveAnnotation ma) {
        if (ma == null || node == null) {
            return;
        }
        String comment = ma.buildCombinedComment();
        if (!comment.isEmpty()) {
            node.setComment(comment);
        }
        if (ma.moveNag > 0) {
            node.addNag(ma.moveNag);
        }
        if (ma.evalNag > 0) {
            node.addNag(ma.evalNag);
        }
        if (ma.prefixNag > 0) {
            node.addNag(ma.prefixNag);
        }
        if (ma.coloredFields != null) {
            for (ColoredField cf : ma.coloredFields) {
                if (!node.getColoredFields().contains(cf)) {
                    node.addOrRemoveColoredField(cf);
                }
            }
        }
        if (ma.arrows != null) {
            for (Arrow a : ma.arrows) {
                if (!node.getArrows().contains(a)) {
                    node.addOrRemoveArrow(a);
                }
            }
        }
    }

    private static final class VarFrame {
        final GameNode node;
        final Board boardCopy;
        final CbhCrypto.RosterState rosterCopy;

        VarFrame(GameNode node, Board boardCopy, CbhCrypto.RosterState rosterCopy) {
            this.node = node;
            this.boardCopy = boardCopy;
            this.rosterCopy = rosterCopy;
        }
    }

    private static void decodeCbgGame(
            FileChannel cbgChannel,
            long cbgOffset,
            Game game,
            Map<Integer, MoveAnnotation> annotations) throws IOException {
        long fileSize = cbgChannel.size();
        if (cbgOffset + 4 > fileSize) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        byte[] hdr4 = new byte[4];
        if (readUpToAt(cbgChannel, cbgOffset, hdr4, 4) < 4) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        int hdr1 = CbCommon.readI32BE(hdr4, 0);
        if ((hdr1 & 0x80000000) != 0) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        int blockLen = hdr1 & 0x00FFFFFF;
        if (blockLen < 4 || blockLen > 16_777_216) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        int payloadLen = (int) Math.min((long) blockLen - 4L, fileSize - (cbgOffset + 4L));
        if (payloadLen <= 0) {
            game.getRootNode().setBoard(new Board(true));
            applyAnnotationToNode(game.getRootNode(), annotations.get(0));
            return;
        }
        byte[] data = new byte[payloadLen];
        int actual = readUpToAt(cbgChannel, cbgOffset + 4L, data, payloadLen);

        CbhCrypto.RosterState state = new CbhCrypto.RosterState();
        int pos = 0;
        boolean customSetup = (hdr1 & 0x40000000) != 0;
        Board curBoard;
        if (customSetup && actual >= 28) {
            String fen = CbhCrypto.decodeCustomSetup(data, 0, state);
            pos = 28;
            curBoard = new Board(fen);
            game.setHeader("SetUp", "1");
        } else {
            state.resetStandard();
            curBoard = new Board(true);
        }

        GameNode root = game.getRootNode();
        root.setBoard(curBoard.makeCopy());
        applyAnnotationToNode(root, annotations.get(0));

        GameNode curNode = root;
        ArrayDeque<VarFrame> varStack = new ArrayDeque<>();
        int mc = 0;
        int moveOnlySeq = 0;
        boolean hasSeenPop = false;

        while (pos < actual) {
            int dec = (data[pos++] - mc) & 0xFF;
            int kind = CbhCrypto.OP_KIND[dec];

            if (kind == CbhCrypto.OP_PUSH) {
                CbhCrypto.RosterState rCopy = new CbhCrypto.RosterState();
                rCopy.copyFrom(state);
                varStack.push(new VarFrame(curNode, curBoard.makeCopy(), rCopy));
                continue;
            }
            if (kind == CbhCrypto.OP_POP) {
                if (varStack.isEmpty()) {
                    break;
                }
                VarFrame frame = varStack.pop();
                curNode = frame.node;
                curBoard = frame.boardCopy;
                state = frame.rosterCopy;
                hasSeenPop = true;
                continue;
            }
            if (kind == CbhCrypto.OP_SKIP) {
                continue;
            }

            Move m = null;
            if (kind == CbhCrypto.OP_ESCAPE) {
                if (pos + 2 > actual) {
                    break;
                }
                int x1 = data[pos++] & 0xFF;
                int x2 = data[pos++] & 0xFF;
                int d1 = (x1 - mc) & 0xFF;
                int d2 = (x2 - mc) & 0xFF;
                mc = (mc + 1) & 0xFF;

                int b1 = CbhCrypto.SBOX_INV[d1];
                int b2 = CbhCrypto.SBOX_INV[d2];
                if (b1 < 0 || b2 < 0) {
                    break;
                }
                int promoIdx = (b1 >>> 4) & 0x03;
                int tsqCm = ((b1 & 0x0F) << 2) | ((b2 >>> 6) & 0x03);
                int fsqCm = b2 & 0x3F;
                int fx = fsqCm >> 3;
                int fy = fsqCm & 7;
                int tx = tsqCm >> 3;
                int ty = tsqCm & 7;
                int fromSq = (fy << 3) | fx;
                int toSq = (ty << 3) | tx;
                int promoConst = CbhCrypto.promoCodeToConstant(promoIdx);

                int fromInt = ((fy + 2) * 10) + (fx + 1);
                int toInt = ((ty + 2) * 10) + (tx + 1);
                m = new Move(fromInt, toInt, promoConst);
                state.applyMove(fromSq, toSq, promoConst);
            } else {
                mc = (mc + 1) & 0xFF;
                if (kind == CbhCrypto.OP_NULL) {
                    m = new Move();
                    m.isNullMove = true;
                    state.toMove = 1 - state.toMove;
                } else if (kind == CbhCrypto.OP_CASTLE_K) {
                    int fy;
                    if (state.toMove == 0) {
                        fy = 0;
                    } else {
                        fy = 7;
                    }
                    m = new Move(4, fy, 6, fy);
                    state.applyCastle(true);
                } else if (kind == CbhCrypto.OP_CASTLE_Q) {
                    int fy;
                    if (state.toMove == 0) {
                        fy = 0;
                    } else {
                        fy = 7;
                    }
                    m = new Move(4, fy, 2, fy);
                    state.applyCastle(false);
                } else if (kind == CbhCrypto.OP_MOVE) {
                    int pieceType = CbhCrypto.OP_PIECE_TYPE[dec];
                    int slot = CbhCrypto.OP_SLOT[dec];
                    int dx = CbhCrypto.OP_DX[dec];
                    int dy = CbhCrypto.OP_DY[dec];
                    int fromSq = state.getPieceSquare(pieceType, slot);
                    if (fromSq < 0) {
                        break;
                    }
                    int fx = fromSq & 7;
                    int fy = fromSq >> 3;
                    int tx;
                    int ty;
                    if (state.toMove == 1 && pieceType == CbhCrypto.ROSTER_PAWN) {
                        tx = (fx - dx) & 7;
                        ty = (fy - dy) & 7;
                    } else {
                        tx = (fx + dx) & 7;
                        ty = (fy + dy) & 7;
                    }
                    int toSq = (ty << 3) | tx;
                    m = new Move(fx, fy, tx, ty);
                    state.applyMove(fromSq, toSq, CONSTANTS.EMPTY);
                } else {
                    break;
                }
            }

            if (m != null) {
                Board nextBoard = curBoard.makeCopy();
                nextBoard.apply(m);
                GameNode nextNode = new GameNode();
                nextNode.setMove(m);
                nextNode.setBoard(nextBoard);
                nextNode.setParent(curNode);
                curNode.addVariation(nextNode);
                curNode = nextNode;
                curBoard = nextBoard;

                moveOnlySeq++;
                int cbaKey;
                if (hasSeenPop) {
                    cbaKey = moveOnlySeq - 1;
                } else {
                    cbaKey = moveOnlySeq;
                }
                if (cbaKey > 0) {
                    applyAnnotationToNode(curNode, annotations.get(cbaKey));
                }
            }
        }

        // CBG encodes variations before mainline so the last child attached at any node is the mainline.
        reverseVariationOrder(root);
    }

    static void reverseVariationOrder(GameNode node) {
        ArrayList<GameNode> vars = node.getVariations();
        if (vars.size() > 1) {
            Collections.reverse(vars);
        }
        for (int i = 0; i < vars.size(); i++) {
            reverseVariationOrder(vars.get(i));
        }
    }

    @Override
    public GameInfo appendGame(Game game) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void importGames(ChessDatabase sourceDatabase, List<GameInfo> gamesToImport, ProgressListener listener)
            throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo, ProgressListener listener) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void deleteGame(GameInfo info, ProgressListener listener) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void deleteGame(GameInfo info) throws IOException {
        throw new UnsupportedOperationException("CBH databases are read-only");
    }

    @Override
    public void search(SearchPattern pattern) {
        search(pattern, null);
    }

    @Override
    public void search(SearchPattern pattern, ProgressListener listener) {
        searchResults.clear();
        if (pattern == null || !open) {
            searchActive = false;
            return;
        }

        boolean searchHeader = pattern.isSearchGameData();
        boolean searchPos = pattern.isSearchPosition() && pattern.getSearchBoard() != null;

        boolean[] whiteMask = null;
        boolean[] blackMask = null;
        boolean[] eventMask = null;
        boolean[] siteMask = null;
        boolean ignoreColor = pattern.isIgnoreNameColor();
        String whiteNeedle = "";
        String blackNeedle = "";
        String eventNeedle = "";
        String siteNeedle = "";

        if (searchHeader) {
            if (pattern.getWhiteName() != null && !pattern.getWhiteName().isEmpty()) {
                whiteNeedle = pattern.getWhiteName().toLowerCase(Locale.ROOT);
                whiteMask = namebase.buildPlayerMatchMask(whiteNeedle);
            }
            if (pattern.getBlackName() != null && !pattern.getBlackName().isEmpty()) {
                blackNeedle = pattern.getBlackName().toLowerCase(Locale.ROOT);
                blackMask = namebase.buildPlayerMatchMask(blackNeedle);
            }
            if (pattern.getEvent() != null && !pattern.getEvent().isEmpty()) {
                eventNeedle = pattern.getEvent().toLowerCase(Locale.ROOT);
                eventMask = namebase.buildEventMatchMask(eventNeedle);
            }
            if (pattern.getSite() != null && !pattern.getSite().isEmpty()) {
                siteNeedle = pattern.getSite().toLowerCase(Locale.ROOT);
                siteMask = namebase.buildSiteMatchMask(siteNeedle);
            }
        }

        if (!searchPos) {
            int total = entries.size();
            for (int i = 0; i < total; i++) {
                if (listener != null && listener.isCancelled()) {
                    break;
                }
                CbhGameInfo e = entries.get(i);
                if (!searchHeader || matchesCbhHeader(e, pattern, whiteMask, blackMask, eventMask, siteMask, ignoreColor)) {
                    searchResults.add(e);
                }
                if (listener != null && (i % 4096 == 0) && total > 0) {
                    listener.onProgress((int) (((long) i * 100L) / total));
                }
            }
            if (listener != null && !listener.isCancelled()) {
                listener.onProgress(100);
            }
            searchActive = true;
            return;
        }

        int total = entries.size();
        if (total == 0) {
            if (listener != null && !listener.isCancelled()) {
                listener.onProgress(100);
            }
            searchActive = true;
            return;
        }

        int numThreads = Math.min(Math.max(1, Runtime.getRuntime().availableProcessors()), total);
        int chunkSize = (total + numThreads - 1) / numThreads;
        AtomicInteger processed = new AtomicInteger(0);
        boolean[] wM = whiteMask;
        boolean[] bM = blackMask;
        boolean[] eM = eventMask;
        boolean[] sM = siteMask;

        List<Callable<List<GameInfo>>> tasks = new ArrayList<>(numThreads);
        for (int t = 0; t < numThreads; t++) {
            final int start = t * chunkSize;
            final int end = Math.min(start + chunkSize, total);
            if (start >= end) {
                continue;
            }
            tasks.add(() -> {
                List<GameInfo> localHits = new ArrayList<>();
                Scid5FastSearchScanner matcher = new Scid5FastSearchScanner(pattern);
                CbgPositionScanner posScanner = new CbgPositionScanner();
                FileChannel myCbg = null;
                if (cbgPath != null && Files.exists(cbgPath)) {
                    myCbg = FileChannel.open(cbgPath, StandardOpenOption.READ);
                }
                int localCount = 0;
                try {
                    for (int i = start; i < end; i++) {
                        if (listener != null && listener.isCancelled()) {
                            break;
                        }
                        CbhGameInfo entry = entries.get(i);
                        if (searchHeader && !matchesCbhHeader(entry, pattern, wM, bM, eM, sM, ignoreColor)) {
                            localCount++;
                            continue;
                        }
                        if (myCbg != null && posScanner.scanCbg(myCbg, entry.getCbgOffset(), matcher)) {
                            localHits.add(entry);
                        }
                        localCount++;
                        if (localCount >= 1000) {
                            if (listener != null) {
                                int curr = processed.addAndGet(localCount);
                                listener.onProgress((int) (((long) curr * 100L) / total));
                            }
                            localCount = 0;
                        }
                    }
                    if (localCount > 0 && listener != null) {
                        int curr = processed.addAndGet(localCount);
                        listener.onProgress((int) (((long) curr * 100L) / total));
                    }
                } finally {
                    if (myCbg != null) {
                        try {
                            myCbg.close();
                        } catch (IOException ignored) {
                        }
                    }
                }
                return localHits;
            });
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        try {
            List<Future<List<GameInfo>>> futures = executor.invokeAll(tasks);
            for (Future<List<GameInfo>> f : futures) {
                searchResults.addAll(f.get());
            }
        } catch (Exception ignored) {
        } finally {
            executor.shutdown();
        }

        if (listener != null && !listener.isCancelled()) {
            listener.onProgress(100);
        }
        searchActive = true;
    }

    private static boolean matchesCbhHeader(
            CbhGameInfo info,
            SearchPattern pattern,
            boolean[] whiteMask,
            boolean[] blackMask,
            boolean[] eventMask,
            boolean[] siteMask,
            boolean ignoreColor) {
        if (eventMask != null) {
            int tid = info.getTournamentId();
            if (tid < 0 || tid >= eventMask.length || !eventMask[tid]) {
                return false;
            }
        }
        if (siteMask != null) {
            int tid = info.getTournamentId();
            if (tid < 0 || tid >= siteMask.length || !siteMask[tid]) {
                return false;
            }
        }
        if (pattern.isCheckYear()) {
            int year = CbCommon.extractYear(info.getPackedDate());
            if (year > 0 && (year < pattern.getMinYear() || year > pattern.getMaxYear())) {
                return false;
            }
        }
        int wid = info.getWhiteId();
        int bid = info.getBlackId();
        if (!ignoreColor) {
            if (whiteMask != null && (wid < 0 || wid >= whiteMask.length || !whiteMask[wid])) {
                return false;
            }
            if (blackMask != null && (bid < 0 || bid >= blackMask.length || !blackMask[bid])) {
                return false;
            }
        } else {
            if (whiteMask != null) {
                boolean wHit = (wid >= 0 && wid < whiteMask.length && whiteMask[wid])
                        || (bid >= 0 && bid < whiteMask.length && whiteMask[bid]);
                if (!wHit) {
                    return false;
                }
            }
            if (blackMask != null) {
                boolean bHit = (wid >= 0 && wid < blackMask.length && blackMask[wid])
                        || (bid >= 0 && bid < blackMask.length && blackMask[bid]);
                if (!bHit) {
                    return false;
                }
            }
        }

        int r = info.getResultByte();
        if (!pattern.isResultBlackWins() && r == 0) {
            return false;
        }
        if (!pattern.isResultDraw() && r == 1) {
            return false;
        }
        if (!pattern.isResultWhiteWins() && r == 2) {
            return false;
        }
        if (!pattern.isResultUndef() && r > 2) {
            return false;
        }

        int checkElo = pattern.getCheckElo();
        if (checkElo != SearchPattern.SEARCH_IGNORE_ELO) {
            int we = info.getWhiteEloVal();
            int be = info.getBlackEloVal();
            int minElo = pattern.getMinElo();
            int maxElo = pattern.getMaxElo();
            if (checkElo == SearchPattern.SEARCH_ONE_ELO) {
                boolean ok = (we > 0 && we > minElo && we < maxElo)
                        || (be > 0 && be > minElo && be < maxElo);
                if (!ok) {
                    return false;
                }
            } else if (checkElo == SearchPattern.SEARCH_BOTH_ELO) {
                boolean ok = (we > 0 && we > minElo && we < maxElo)
                        && (be > 0 && be > minElo && be < maxElo);
                if (!ok) {
                    return false;
                }
            } else if (checkElo == SearchPattern.SEARCH_AVG_ELO) {
                int avg = -1;
                if (we > 0 && be <= 0) {
                    avg = we;
                } else if (we <= 0 && be > 0) {
                    avg = be;
                } else if (we > 0 && be > 0) {
                    avg = (we + be) / 2;
                }
                if (!(avg > minElo && avg < maxElo)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Zero-allocation per-thread position replay scanner for `.cbg` streams using
     * {@link ScidPosition} and {@link Scid5FastSearchScanner#matches(ScidPosition)}.
     */
    private static final class CbgPositionScanner {
        private byte[] buf = new byte[16384];
        private final CbhCrypto.RosterState roster = new CbhCrypto.RosterState();
        private final ScidPosition scidPos = new ScidPosition();
        private final CbhCrypto.RosterState[] rosterStack = new CbhCrypto.RosterState[64];
        private final ScidPosition[] posStack = new ScidPosition[64];

        CbgPositionScanner() {
            for (int i = 0; i < 64; i++) {
                rosterStack[i] = new CbhCrypto.RosterState();
                posStack[i] = new ScidPosition();
            }
        }

        boolean scanCbg(FileChannel channel, long cbgOffset, Scid5FastSearchScanner matcher) throws IOException {
            if (cbgOffset < 26L) {
                return false;
            }
            if (buf.length < 4) {
                buf = new byte[16384];
            }
            if (readUpToAt(channel, cbgOffset, buf, 4) < 4) {
                return false;
            }
            int hdr1 = CbCommon.readI32BE(buf, 0);
            if ((hdr1 & 0x80000000) != 0) {
                return false;
            }
            int blockLen = hdr1 & 0x00FFFFFF;
            if (blockLen < 4 || blockLen > 16_777_216) {
                return false;
            }
            int payloadLen = blockLen - 4;
            if (payloadLen == 0) {
                scidPos.stdStart();
                return matcher.matches(scidPos);
            }
            if (buf.length < payloadLen) {
                buf = new byte[Math.max(payloadLen, buf.length * 2)];
            }
            int actual = readUpToAt(channel, cbgOffset + 4L, buf, payloadLen);
            if (actual <= 0) {
                return false;
            }

            int pos = 0;
            if ((hdr1 & 0x40000000) != 0) {
                if (actual < 28) {
                    return false;
                }
                String fen = CbhCrypto.decodeCustomSetup(buf, 0, roster);
                pos = 28;
                scidPos.setupFen(fen);
            } else {
                roster.resetStandard();
                scidPos.stdStart();
            }

            if (matcher.matches(scidPos)) {
                return true;
            }

            int depth = 0;
            int mc = 0;
            while (pos < actual) {
                int dec = (buf[pos++] - mc) & 0xFF;
                int kind = CbhCrypto.OP_KIND[dec];
                if (kind == CbhCrypto.OP_PUSH) {
                    if (depth < rosterStack.length) {
                        rosterStack[depth].copyFrom(roster);
                        posStack[depth].copyFrom(scidPos);
                        depth++;
                    }
                    continue;
                }
                if (kind == CbhCrypto.OP_POP) {
                    if (depth == 0) {
                        break;
                    }
                    depth--;
                    roster.copyFrom(rosterStack[depth]);
                    scidPos.copyFrom(posStack[depth]);
                    continue;
                }
                if (kind == CbhCrypto.OP_SKIP) {
                    continue;
                }
                if (kind == CbhCrypto.OP_ESCAPE) {
                    if (pos + 2 > actual) {
                        break;
                    }
                    int x1 = buf[pos++] & 0xFF;
                    int x2 = buf[pos++] & 0xFF;
                    int d1 = (x1 - mc) & 0xFF;
                    int d2 = (x2 - mc) & 0xFF;
                    mc = (mc + 1) & 0xFF;
                    int b1 = CbhCrypto.SBOX_INV[d1];
                    int b2 = CbhCrypto.SBOX_INV[d2];
                    if (b1 < 0 || b2 < 0) {
                        break;
                    }
                    int promoIdx = (b1 >>> 4) & 0x03;
                    int tsqCm = ((b1 & 0x0F) << 2) | ((b2 >>> 6) & 0x03);
                    int fsqCm = b2 & 0x3F;
                    int fromSq = ((fsqCm & 7) << 3) | (fsqCm >> 3);
                    int toSq = ((tsqCm & 7) << 3) | (tsqCm >> 3);
                    int promoConst = CbhCrypto.promoCodeToConstant(promoIdx);
                    roster.applyMove(fromSq, toSq, promoConst);
                    scidPos.doMove(fromSq, toSq, toScidPromo(promoConst), false, false);
                } else {
                    mc = (mc + 1) & 0xFF;
                    if (kind == CbhCrypto.OP_NULL) {
                        roster.toMove = 1 - roster.toMove;
                        scidPos.toMove = 1 - scidPos.toMove;
                    } else if (kind == CbhCrypto.OP_CASTLE_K) {
                        int baseSq;
                        if (roster.toMove == 0) {
                            baseSq = 0;
                        } else {
                            baseSq = 56;
                        }
                        roster.applyCastle(true);
                        scidPos.doMove(baseSq + 4, baseSq + 6, ScidPosition.EMPTY, true, false);
                    } else if (kind == CbhCrypto.OP_CASTLE_Q) {
                        int baseSq;
                        if (roster.toMove == 0) {
                            baseSq = 0;
                        } else {
                            baseSq = 56;
                        }
                        roster.applyCastle(false);
                        scidPos.doMove(baseSq + 4, baseSq + 2, ScidPosition.EMPTY, false, true);
                    } else if (kind == CbhCrypto.OP_MOVE) {
                        int pieceType = CbhCrypto.OP_PIECE_TYPE[dec];
                        int slot = CbhCrypto.OP_SLOT[dec];
                        int dx = CbhCrypto.OP_DX[dec];
                        int dy = CbhCrypto.OP_DY[dec];
                        int fromSq = roster.getPieceSquare(pieceType, slot);
                        if (fromSq < 0) {
                            break;
                        }
                        int fx = fromSq & 7;
                        int fy = fromSq >> 3;
                        int tx;
                        int ty;
                        if (roster.toMove == 1 && pieceType == CbhCrypto.ROSTER_PAWN) {
                            tx = (fx - dx) & 7;
                            ty = (fy - dy) & 7;
                        } else {
                            tx = (fx + dx) & 7;
                            ty = (fy + dy) & 7;
                        }
                        int toSq = (ty << 3) | tx;
                        roster.applyMove(fromSq, toSq, CONSTANTS.EMPTY);
                        scidPos.doMove(fromSq, toSq, ScidPosition.EMPTY, false, false);
                    } else {
                        break;
                    }
                }
                if (matcher.matches(scidPos)) {
                    return true;
                }
            }
            return false;
        }
    }

    static int toScidPromo(int promoConst) {
        if (promoConst == CONSTANTS.QUEEN) {
            return ScidPosition.QUEEN;
        } else if (promoConst == CONSTANTS.ROOK) {
            return ScidPosition.ROOK;
        } else if (promoConst == CONSTANTS.BISHOP) {
            return ScidPosition.BISHOP;
        } else if (promoConst == CONSTANTS.KNIGHT) {
            return ScidPosition.KNIGHT;
        }
        return ScidPosition.EMPTY;
    }

    static int readUpToAt(FileChannel ch, long offset, byte[] dst, int len) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(dst, 0, len);
        long pos = offset;
        int total = 0;
        while (bb.hasRemaining()) {
            int n = ch.read(bb, pos);
            if (n < 0) {
                break;
            }
            pos += n;
            total += n;
        }
        return total;
    }

    @Override
    public void addListener(ChessDatabaseListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(ChessDatabaseListener listener) {
        listeners.remove(listener);
    }

    @Override
    public long getRevision() {
        return revision;
    }

    private void publish(ChessDatabaseEvent event) {
        for (ChessDatabaseListener listener : List.copyOf(listeners)) {
            listener.onDatabaseChanged(event);
        }
    }
}
