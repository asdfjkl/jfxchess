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
 * Read/write {@link ChessDatabase} implementation for modern ChessBase
 * 2CBH databases (`.2cbh`, `.2cbg`, `.2cba`, `.2lid`).
 */
public class TwoCbhChessDatabase implements ChessDatabase {

    static final int TWO_CBH_HEADER_SIZE = 192;
    static final int TWO_CBH_RECORD_SIZE = 192;
    static final int TWO_CBA_HEADER_SIZE = 16;
    static final long TWO_CBA_EMPTY_SENTINEL = 12L;
    static final long TWO_CBG_MAGIC = 0x1122334455667788L;
    private static final int SCAN_BATCH_RECORDS = 2048;

    private Path basePath;
    private Path twoCbhPath;
    private Path twoCbgPath;
    private Path twoCbaPath;
    private Path twoLidPath;

    private FileChannel twoCbhChannel;
    private FileChannel twoCbgChannel;
    private FileChannel twoCbaChannel;
    private boolean open;
    private boolean readOnly;
    private int totalRawRecords;
    private boolean annotatorUsesSlot0Fallback;
    private boolean annotatorFallbackChecked;

    private final TwoCbhNamebase namebase = new TwoCbhNamebase();
    private final ArrayList<TwoCbhGameInfo> entries = new ArrayList<>();
    private final ArrayList<GameInfo> searchResults = new ArrayList<>();
    private boolean searchActive;
    private final List<ChessDatabaseListener> listeners = new ArrayList<>();
    private long revision;

    public TwoCbhChessDatabase() {
    }

    public TwoCbhChessDatabase(String filename) throws IOException {
        open(filename);
    }

    static Path resolveBasePath(String filename) {
        Objects.requireNonNull(filename, "filename");
        Path p = Path.of(filename).toAbsolutePath().normalize();
        String name = p.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        int strip = 0;
        if (lower.endsWith(".2cbh") || lower.endsWith(".2cbg")
                || lower.endsWith(".2cba") || lower.endsWith(".2lid")) {
            strip = 5;
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
        return readOnly || namebase.isReadOnly();
    }

    @Override
    public void open(String filename) throws IOException {
        close();
        Path base = resolveBasePath(filename);
        Path twoCbh = resolveExistingSibling(base, ".2cbh");
        if (!Files.exists(twoCbh)) {
            throw new FileNotFoundException("2CBH index file not found: " + twoCbh);
        }
        Path twoCbg = resolveExistingSibling(base, ".2cbg");
        Path twoCba = resolveExistingSibling(base, ".2cba");
        Path twoLid = resolveExistingSibling(base, ".2lid");

        this.basePath = base;
        this.twoCbhPath = twoCbh;
        this.twoCbgPath = twoCbg;
        this.twoCbaPath = twoCba;
        this.twoLidPath = twoLid;

        this.namebase.open(twoLid);
        boolean ro = this.namebase.isReadOnly();
        if (!ro) {
            try {
                this.twoCbhChannel = FileChannel.open(twoCbh, StandardOpenOption.READ, StandardOpenOption.WRITE);
            } catch (IOException e) {
                this.twoCbhChannel = FileChannel.open(twoCbh, StandardOpenOption.READ);
                ro = true;
            }
        } else {
            this.twoCbhChannel = FileChannel.open(twoCbh, StandardOpenOption.READ);
        }
        if (Files.exists(twoCbg)) {
            if (!ro) {
                try {
                    this.twoCbgChannel = FileChannel.open(twoCbg, StandardOpenOption.READ, StandardOpenOption.WRITE);
                } catch (IOException e) {
                    this.twoCbgChannel = FileChannel.open(twoCbg, StandardOpenOption.READ);
                    ro = true;
                }
            } else {
                this.twoCbgChannel = FileChannel.open(twoCbg, StandardOpenOption.READ);
            }
        }
        if (Files.exists(twoCba)) {
            if (!ro) {
                try {
                    this.twoCbaChannel = FileChannel.open(twoCba, StandardOpenOption.READ, StandardOpenOption.WRITE);
                } catch (IOException e) {
                    this.twoCbaChannel = FileChannel.open(twoCba, StandardOpenOption.READ);
                    ro = true;
                }
            } else {
                this.twoCbaChannel = FileChannel.open(twoCba, StandardOpenOption.READ);
            }
        }
        this.readOnly = ro;
        long cbhSize = this.twoCbhChannel.size();
        if (cbhSize > TWO_CBH_HEADER_SIZE) {
            this.totalRawRecords = (int) ((cbhSize - TWO_CBH_HEADER_SIZE) / TWO_CBH_RECORD_SIZE);
        } else {
            this.totalRawRecords = 0;
        }
        this.annotatorUsesSlot0Fallback = false;
        this.annotatorFallbackChecked = (this.totalRawRecords == 0 || this.namebase.getEntryCount(3) > 0L);
        this.open = true;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.revision = 0;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void createNew(String filename) throws IOException {
        close();
        Path base = resolveBasePath(filename);
        Path parent = base.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path twoCbh = Path.of(base.toString() + ".2cbh");
        Path twoCbg = Path.of(base.toString() + ".2cbg");
        Path twoCba = Path.of(base.toString() + ".2cba");
        Path twoLid = Path.of(base.toString() + ".2lid");

        byte[] cbhHdr = new byte[TWO_CBH_HEADER_SIZE];
        CbCommon.writeU16LE(cbhHdr, 0x0A, TWO_CBH_RECORD_SIZE);
        CbCommon.writeI64LE(cbhHdr, 0x10, 1L); // totalRecords + 1
        Files.write(twoCbh, cbhHdr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        Files.write(twoCbg, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        byte[] cbaHdr = new byte[TWO_CBA_HEADER_SIZE];
        Files.write(twoCba, cbaHdr, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

        this.namebase.createNew(twoLid);

        this.basePath = base;
        this.twoCbhPath = twoCbh;
        this.twoCbgPath = twoCbg;
        this.twoCbaPath = twoCba;
        this.twoLidPath = twoLid;
        this.twoCbhChannel = FileChannel.open(twoCbh, StandardOpenOption.READ, StandardOpenOption.WRITE);
        this.twoCbgChannel = FileChannel.open(twoCbg, StandardOpenOption.READ, StandardOpenOption.WRITE);
        this.twoCbaChannel = FileChannel.open(twoCba, StandardOpenOption.READ, StandardOpenOption.WRITE);
        this.open = true;
        this.readOnly = false;
        this.totalRawRecords = 0;
        this.annotatorUsesSlot0Fallback = false;
        this.annotatorFallbackChecked = true;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.revision = 0;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        IOException firstError = null;
        FileChannel[] channels = new FileChannel[]{twoCbhChannel, twoCbgChannel, twoCbaChannel};
        twoCbhChannel = null;
        twoCbgChannel = null;
        twoCbaChannel = null;
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
        try {
            this.namebase.close();
        } catch (IOException e) {
            if (firstError == null) {
                firstError = e;
            }
        }
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
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
        if (twoCbhPath == null) {
            return "";
        }
        return twoCbhPath.toString();
    }

    @Override
    public Path getPath() {
        return twoCbhPath;
    }

    Path getBasePath() {
        return basePath;
    }

    Path getTwoCbhPath() {
        return twoCbhPath;
    }

    Path getTwoCbgPath() {
        return twoCbgPath;
    }

    Path getTwoCbaPath() {
        return twoCbaPath;
    }

    Path getTwoLidPath() {
        return twoLidPath;
    }

    TwoCbhNamebase getNamebase() {
        return namebase;
    }

    @Override
    public void scanGames() {
        scanGames(null);
    }

    @Override
    public void scanGames(ProgressListener listener) {
        if (!open || twoCbhChannel == null) {
            return;
        }
        entries.clear();
        searchResults.clear();
        searchActive = false;

        try {
            long fileSize = twoCbhChannel.size();
            if (fileSize <= TWO_CBH_HEADER_SIZE) {
                this.totalRawRecords = 0;
                this.annotatorUsesSlot0Fallback = false;
                this.annotatorFallbackChecked = true;
                if (listener != null && !listener.isCancelled()) {
                    listener.onProgress(100);
                }
                revision++;
                publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.INDEX_REBUILT, this, revision, null));
                return;
            }
            int totalRecords = (int) ((fileSize - TWO_CBH_HEADER_SIZE) / TWO_CBH_RECORD_SIZE);
            this.totalRawRecords = totalRecords;
            boolean checkSlot0Fallback = (namebase.getEntryCount(3) == 0L);
            boolean sawSlot0Annotator = false;
            entries.ensureCapacity(totalRecords);

            byte[] buf = new byte[SCAN_BATCH_RECORDS * TWO_CBH_RECORD_SIZE];
            long offset = TWO_CBH_HEADER_SIZE;
            int rawIndex = 0;

            while (rawIndex < totalRecords) {
                if (listener != null && listener.isCancelled()) {
                    break;
                }
                int batchCount = Math.min(SCAN_BATCH_RECORDS, totalRecords - rawIndex);
                int bytesWanted = batchCount * TWO_CBH_RECORD_SIZE;
                int bytesRead = readUpToAt(twoCbhChannel, offset, buf, bytesWanted);
                int recordsRead = bytesRead / TWO_CBH_RECORD_SIZE;
                if (recordsRead <= 0) {
                    break;
                }

                for (int i = 0; i < recordsRead; i++) {
                    int off = i * TWO_CBH_RECORD_SIZE;
                    long flags = CbCommon.readI64LE(buf, off);
                    // Skip guiding texts (0x02), deleted records (0x80), and non-game records (!(flags & 0x01))
                    if ((flags & 0x01L) != 0L && (flags & 0x02L) == 0L && (flags & 0x80L) == 0L) {
                        if (checkSlot0Fallback && !sawSlot0Annotator) {
                            long annoId = CbCommon.readI64LE(buf, off + 0x30);
                            if (annoId >= 0L && annoId < namebase.getEntryCount(0)) {
                                sawSlot0Annotator = true;
                            }
                        }
                        long cbgOffset = CbCommon.readI64LE(buf, off + 0x08);
                        long cbaOffset = CbCommon.readI64LE(buf, off + 0x10);
                        int whiteId = clampIntId(CbCommon.readI64LE(buf, off + 0x18));
                        int blackId = clampIntId(CbCommon.readI64LE(buf, off + 0x20));
                        int tournamentId = clampIntId(CbCommon.readI64LE(buf, off + 0x28));
                        int resultByte = buf[off + 0x58] & 0xFF;
                        int round = buf[off + 0x5A] & 0xFF;
                        int subround = buf[off + 0x5C] & 0xFF;
                        int whiteElo = CbCommon.readU16LE(buf, off + 0x60);
                        int blackElo = CbCommon.readU16LE(buf, off + 0x70);
                        int ecoRaw = CbCommon.readU16LE(buf, off + 0x80);
                        int packedDate = CbCommon.readI32LE(buf, off + 0xBC);

                        entries.add(new TwoCbhGameInfo(
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
                offset += (long) recordsRead * TWO_CBH_RECORD_SIZE;

                if (listener != null && totalRecords > 0) {
                    int pct = (int) (((long) rawIndex * 100L) / totalRecords);
                    listener.onProgress(Math.min(99, pct));
                }
            }

            if (listener == null || !listener.isCancelled()) {
                this.annotatorUsesSlot0Fallback = sawSlot0Annotator;
                this.annotatorFallbackChecked = true;
            }
            if (listener != null && !listener.isCancelled()) {
                listener.onProgress(100);
            }
            revision++;
            publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.INDEX_REBUILT, this, revision, null));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan 2CBH database: " + e.getMessage(), e);
        }
    }

    static int clampIntId(long id) {
        if (id < 0L || id > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) id;
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
            throw new IOException("2CBH database is not open");
        }
        if (info == null) {
            throw new IllegalArgumentException("GameInfo cannot be null");
        }
        TwoCbhGameInfo tgi;
        if (info instanceof TwoCbhGameInfo t) {
            tgi = t;
        } else {
            tgi = findEntryById(info.getId());
            if (tgi == null) {
                throw new IOException("Game not found in 2CBH database: " + info.getId());
            }
        }

        Game game = new Game();
        game.setHeader("Event", tgi.getEvent());
        game.setHeader("Site", tgi.getSite());
        game.setHeader("Date", tgi.getDate());
        game.setHeader("Round", tgi.getRound());
        game.setHeader("White", tgi.getWhite());
        game.setHeader("Black", tgi.getBlack());
        game.setHeader("Result", tgi.getResult());
        String eco = tgi.getEco();
        if (!eco.isEmpty()) {
            game.setHeader("ECO", eco);
        }
        String whiteElo = tgi.getWhiteElo();
        if (!whiteElo.isEmpty()) {
            game.setHeader("WhiteElo", whiteElo);
        }
        String blackElo = tgi.getBlackElo();
        if (!blackElo.isEmpty()) {
            game.setHeader("BlackElo", blackElo);
        }
        game.setResult(CbCommon.decodeResultConstant(tgi.getResultByte()));

        if (twoCbhChannel != null) {
            long recOffset = TWO_CBH_HEADER_SIZE + ((long) tgi.getRawIndex() * TWO_CBH_RECORD_SIZE);
            byte[] ids = new byte[16];
            if (readUpToAt(twoCbhChannel, recOffset + 0x30, ids, 16) == 16) {
                int annotatorId = clampIntId(CbCommon.readI64LE(ids, 0));
                int sourceId = clampIntId(CbCommon.readI64LE(ids, 8));
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

        if (twoCbgChannel != null && tgi.getCbgOffset() >= 0L) {
            decodeTwoCbgGame(twoCbgChannel, tgi.getCbgOffset(), game);
        } else {
            game.getRootNode().setBoard(new Board(true));
        }

        // Reverse variation order so child 0 is mainline and siblings preserve C_rev order.
        CbhChessDatabase.reverseVariationOrder(game.getRootNode());

        if (tgi.getCbaOffset() > 12L && twoCbaChannel != null) {
            Map<Integer, CbhChessDatabase.MoveAnnotation> annotations =
                    readTwoCbaBlock(twoCbaChannel, tgi.getCbaOffset());
            if (!annotations.isEmpty()) {
                ArrayList<GameNode> cRevList = new ArrayList<>();
                buildCRevNodeList(game.getRootNode(), cRevList);
                for (Map.Entry<Integer, CbhChessDatabase.MoveAnnotation> entry : annotations.entrySet()) {
                    int mvIdx = entry.getKey();
                    if (mvIdx >= 0 && mvIdx < cRevList.size()) {
                        CbhChessDatabase.applyAnnotationToNode(cRevList.get(mvIdx), entry.getValue());
                    }
                }
            }
        }

        return game;
    }

    private TwoCbhGameInfo findEntryById(UUID id) {
        if (id == null) {
            return null;
        }
        for (TwoCbhGameInfo entry : entries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    static void buildCRevNodeList(GameNode root, ArrayList<GameNode> out) {
        out.add(root);
        if (!root.getVariations().isEmpty()) {
            for (int i = 0; i < root.getVariations().size(); i++) {
                collectCRev(root.getVariations().get(i), out);
            }
        }
    }

    private static void collectCRev(GameNode node, ArrayList<GameNode> out) {
        out.add(node);
        ArrayList<GameNode> vars = node.getVariations();
        for (int i = 0; i < vars.size(); i++) {
            collectCRev(vars.get(i), out);
        }
    }

    private static final class TwoCbgVarFrame {
        final GameNode node;
        final Board boardCopy;

        TwoCbgVarFrame(GameNode node, Board boardCopy) {
            this.node = node;
            this.boardCopy = boardCopy;
        }
    }

    private static void decodeTwoCbgGame(FileChannel channel, long cbgOffset, Game game) throws IOException {
        long fileSize = channel.size();
        if (cbgOffset < 0 || cbgOffset + 24L > fileSize) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        byte[] hdr = new byte[24];
        if (readUpToAt(channel, cbgOffset, hdr, 24) < 24) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        long magic = CbCommon.readI64LE(hdr, 0);
        int payloadLen = CbCommon.readI32LE(hdr, 8);
        if (magic != TWO_CBG_MAGIC || payloadLen < 4 || payloadLen > 16_777_216) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        int actualLen = (int) Math.min((long) payloadLen, fileSize - (cbgOffset + 24L));
        if (actualLen < 4) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }
        byte[] payload = new byte[actualLen];
        int read = readUpToAt(channel, cbgOffset + 24L, payload, actualLen);
        int wordCount = read / 2;
        if (wordCount < 2) {
            game.getRootNode().setBoard(new Board(true));
            return;
        }

        int w0 = CbCommon.readU16LE(payload, 0);
        int w1 = CbCommon.readU16LE(payload, 2);
        Board curBoard;
        int wIdx;
        if (w0 == 0x0001 && w1 == 0xFFFC) {
            curBoard = new Board(true);
            wIdx = 2;
        } else if (w0 == 0x0001 && w1 == 0xFFFB) {
            String[] outFen = new String[1];
            wIdx = TwoCbhMoveTable.decodeSetup(payload, read, outFen);
            if (outFen[0] != null) {
                curBoard = new Board(outFen[0]);
                game.setHeader("SetUp", "1");
            } else {
                curBoard = new Board(true);
            }
        } else {
            game.getRootNode().setBoard(new Board(true));
            return;
        }

        GameNode root = game.getRootNode();
        root.setBoard(curBoard.makeCopy());
        GameNode curNode = root;
        GameNode prevNode = null;
        Board prevBoard = curBoard.makeCopy();
        ArrayDeque<TwoCbgVarFrame> varStack = new ArrayDeque<>();

        while (wIdx < wordCount) {
            int w = CbCommon.readU16LE(payload, wIdx * 2);
            wIdx++;
            if (w == 0xFFFD) {
                if (prevNode != null) {
                    varStack.push(new TwoCbgVarFrame(prevNode, prevBoard.makeCopy()));
                }
                continue;
            }
            if (w == 0xFFFF) {
                if (varStack.isEmpty()) {
                    break;
                }
                TwoCbgVarFrame frame = varStack.pop();
                curNode = frame.node;
                curBoard = frame.boardCopy;
                prevNode = curNode.getParent();
                continue;
            }

            Move m = null;
            if (w == 0xFFFA) {
                m = new Move();
                m.isNullMove = true;
            } else if (w >= 1 && w <= TwoCbhMoveTable.TABLE_SIZE) {
                int packed = TwoCbhMoveTable.MOVES[w];
                int fromSq = TwoCbhMoveTable.fromSq64(packed);
                int toSq = TwoCbhMoveTable.toSq64(packed);
                int promo = TwoCbhMoveTable.promoConst(packed);
                int fx = fromSq & 7;
                int fy = fromSq >> 3;
                int tx = toSq & 7;
                int ty = toSq >> 3;
                if (promo != CONSTANTS.EMPTY) {
                    int fInt = ((fy + 2) * 10) + (fx + 1);
                    int tInt = ((ty + 2) * 10) + (tx + 1);
                    m = new Move(fInt, tInt, promo);
                } else {
                    m = new Move(fx, fy, tx, ty);
                }
            } else {
                continue;
            }

            prevNode = curNode;
            prevBoard.copy(curBoard);
            Board nextBoard = curBoard.makeCopy();
            nextBoard.apply(m);

            GameNode nextNode = new GameNode();
            nextNode.setMove(m);
            nextNode.setBoard(nextBoard);
            nextNode.setParent(curNode);
            curNode.addVariation(nextNode);
            curNode = nextNode;
            curBoard = nextBoard;
        }
    }

    static Map<Integer, CbhChessDatabase.MoveAnnotation> readTwoCbaBlock(
            FileChannel cbaChannel,
            long cbaOffset) throws IOException {
        long fileSize = cbaChannel.size();
        if (cbaOffset <= 12L || cbaOffset + 24L > fileSize) {
            return Collections.emptyMap();
        }
        byte[] hdr = new byte[24];
        if (readUpToAt(cbaChannel, cbaOffset, hdr, 24) < 24) {
            return Collections.emptyMap();
        }
        long magic = CbCommon.readI64LE(hdr, 0);
        int payloadLen = CbCommon.readI32LE(hdr, 8);
        if (magic != TWO_CBG_MAGIC || payloadLen < 4 || payloadLen > 16_777_216) {
            return Collections.emptyMap();
        }
        int actual = (int) Math.min((long) payloadLen, fileSize - (cbaOffset + 24L));
        if (actual < 4) {
            return Collections.emptyMap();
        }
        byte[] data = new byte[actual];
        int read = readUpToAt(cbaChannel, cbaOffset + 24L, data, actual);
        if (read < 4) {
            return Collections.emptyMap();
        }
        if (CbCommon.readU16LE(data, 0) != 0x2000) {
            return Collections.emptyMap();
        }

        Map<Integer, CbhChessDatabase.MoveAnnotation> map = new HashMap<>();
        int q = 2;
        while (q + 2 <= read) {
            int marker = CbCommon.readU16LE(data, q);
            if (marker == 0xFFFF) {
                break;
            }
            if (q + 8 > read) {
                break;
            }
            int mvIdx = CbCommon.readI32LE(data, q);
            int itemCount = CbCommon.readI32LE(data, q + 4);
            q += 8;
            if (mvIdx < 0 || itemCount < 0 || itemCount > 100000) {
                break;
            }
            boolean bail = false;
            for (int itIdx = 0; itIdx < itemCount; itIdx++) {
                if (q + 2 > read) {
                    bail = true;
                    break;
                }
                int itemType = CbCommon.readU16LE(data, q);
                q += 2;
                if (itemType == 0x0002 || itemType == 0x0082) {
                    if (q + 8 > read) {
                        bail = true;
                        break;
                    }
                    int lang = CbCommon.readU16LE(data, q + 2);
                    int slen = CbCommon.readI32LE(data, q + 4);
                    q += 8;
                    if (slen < 0 || q + slen > read) {
                        bail = true;
                        break;
                    }
                    if (slen > 0) {
                        String text = CbCommon.decodeCommentText(data, q, slen, false);
                        if (!text.isEmpty()) {
                            CbhChessDatabase.MoveAnnotation ma =
                                    map.computeIfAbsent(mvIdx, k -> new CbhChessDatabase.MoveAnnotation());
                            int prio = CbCommon.twoCbaLanguagePriority(lang);
                            if (itemType == 0x0082) {
                                ma.setPreText(text, prio);
                            } else {
                                ma.setPostText(text, prio);
                            }
                        }
                    }
                    q += slen;
                } else if (itemType == 0x0003) {
                    if (q + 3 > read) {
                        bail = true;
                        break;
                    }
                    CbhChessDatabase.MoveAnnotation ma =
                            map.computeIfAbsent(mvIdx, k -> new CbhChessDatabase.MoveAnnotation());
                    int mNag = CbCommon.mapMoveNag(data[q] & 0xFF);
                    int eNag = CbCommon.mapEvalNag(data[q + 1] & 0xFF);
                    int pNag = CbCommon.mapPrefixNag(data[q + 2] & 0xFF);
                    if (mNag > 0) {
                        ma.moveNag = mNag;
                    }
                    if (eNag > 0) {
                        ma.evalNag = eNag;
                    }
                    if (pNag > 0) {
                        ma.prefixNag = pNag;
                    }
                    q += 3;
                } else if (itemType == 0x0004) {
                    if (q + 4 > read) {
                        bail = true;
                        break;
                    }
                    int byteLen = CbCommon.readI32LE(data, q);
                    q += 4;
                    if (byteLen < 0 || q + byteLen > read) {
                        bail = true;
                        break;
                    }
                    CbhChessDatabase.MoveAnnotation ma =
                            map.computeIfAbsent(mvIdx, k -> new CbhChessDatabase.MoveAnnotation());
                    if (ma.coloredFields == null) {
                        ma.coloredFields = new ArrayList<>();
                    }
                    for (int k = 0; k + 1 < byteLen; k += 2) {
                        int sq1 = data[q + k + 1] & 0xFF;
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
                    q += byteLen;
                } else if (itemType == 0x0005) {
                    if (q + 4 > read) {
                        bail = true;
                        break;
                    }
                    int byteLen = CbCommon.readI32LE(data, q);
                    q += 4;
                    if (byteLen < 0 || q + byteLen > read) {
                        bail = true;
                        break;
                    }
                    CbhChessDatabase.MoveAnnotation ma =
                            map.computeIfAbsent(mvIdx, k -> new CbhChessDatabase.MoveAnnotation());
                    if (ma.arrows == null) {
                        ma.arrows = new ArrayList<>();
                    }
                    for (int k = 0; k + 2 < byteLen; k += 3) {
                        int f1 = data[q + k + 1] & 0xFF;
                        int t1 = data[q + k + 2] & 0xFF;
                        if (f1 >= 1 && f1 <= 64 && t1 >= 1 && t1 <= 64) {
                            int f0 = f1 - 1;
                            int t0 = t1 - 1;
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
                    q += byteLen;
                } else if (itemType == 0x0014 || itemType == 0x0018) {
                    q += 1;
                } else if (itemType == 0x0007 || itemType == 0x0022 || itemType == 0x0023) {
                    q += 4;
                } else if (itemType == 0x0021) {
                    q += 6;
                } else if (itemType == 0x0009) {
                    q += 12;
                } else if (itemType == 0x0013) {
                    int nextPos = skipRefGameItem(data, q, read);
                    if (nextPos < 0) {
                        bail = true;
                        break;
                    }
                    q = nextPos;
                } else {
                    bail = true;
                    break;
                }
                if (q > read) {
                    bail = true;
                    break;
                }
            }
            if (bail) {
                break;
            }
        }
        return map;
    }

    static int skipRefGameItem(byte[] data, int pos, int limit) {
        int q = pos + 10;
        if (q > limit) {
            return -1;
        }
        for (int s = 0; s < 6; s++) {
            if (q >= limit) {
                return -1;
            }
            int len = data[q] & 0xFF;
            q += 1 + len;
            if (q > limit) {
                return -1;
            }
        }
        q += 79 + 5;
        if (q + 4 > limit) {
            return -1;
        }
        int len1 = CbCommon.readI32LE(data, q);
        q += 4;
        if (len1 < 0 || q + len1 + 5 + 4 > limit) {
            return -1;
        }
        q += len1 + 5;
        int len2 = CbCommon.readI32LE(data, q);
        q += 4;
        if (len2 < 0 || q + len2 + 29 + 4 > limit) {
            return -1;
        }
        q += len2 + 29;
        int nMoves = CbCommon.readI32LE(data, q);
        q += 4;
        if (nMoves < 0 || nMoves > 100000) {
            return -1;
        }
        long mvBytes = (long) nMoves * 5L + 4L;
        if (q + mvBytes > limit) {
            return -1;
        }
        return (int) (q + mvBytes);
    }

    @Override
    public GameInfo appendGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        ensureWritable();
        ensureAnnotatorFallbackChecked();

        int rawIndex = nextRawIndex();
        TwoCbhGameInfo info = writeEncodedGameAt(game, rawIndex);
        totalRawRecords = rawIndex + 1;
        updateTwoCbhHeaderRecordCount();

        entries.add(info);
        revision++;
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_APPENDED, this, revision, List.of(info)));
        return info;
    }

    @Override
    public void importGames(ChessDatabase sourceDatabase, List<GameInfo> gamesToImport, ProgressListener listener)
            throws IOException {
        Objects.requireNonNull(sourceDatabase, "sourceDatabase");
        Objects.requireNonNull(gamesToImport, "gamesToImport");
        ensureWritable();
        ensureAnnotatorFallbackChecked();
        if (gamesToImport.isEmpty()) {
            if (listener != null) {
                listener.onProgress(100);
            }
            return;
        }

        int total = gamesToImport.size();
        ArrayList<GameInfo> importedEntries = new ArrayList<>(total);
        int rawIndex = nextRawIndex();
        ensureTwoCbgWritableChannel();

        namebase.beginBatch();
        try {
            if (sourceDatabase instanceof TwoCbhChessDatabase srcTwo && srcTwo.isOpen()) {
                FileChannel srcCbhCh = srcTwo.twoCbhChannel;
                FileChannel srcCbgCh = srcTwo.twoCbgChannel;
                FileChannel srcCbaCh = srcTwo.twoCbaChannel;
                for (int i = 0; i < total; i++) {
                    if (listener != null && listener.isCancelled()) {
                        break;
                    }
                    GameInfo info = gamesToImport.get(i);
                    if (info instanceof TwoCbhGameInfo srcInfo && srcCbgCh != null && srcInfo.getCbgOffset() >= 0L) {
                        byte[] rawCbg = readRawMagicBlock(srcCbgCh, srcInfo.getCbgOffset());
                        if (rawCbg != null) {
                            int whiteId = namebase.findOrAddPlayer(srcInfo.getWhite());
                            int blackId = namebase.findOrAddPlayer(srcInfo.getBlack());
                            int tournamentId = namebase.findOrAddTournament(srcInfo.getEvent(), srcInfo.getSite());
                            String annotator = "";
                            String source = "";
                            if (srcCbhCh != null) {
                                long srcRecOff = TWO_CBH_HEADER_SIZE + ((long) srcInfo.getRawIndex() * TWO_CBH_RECORD_SIZE);
                                byte[] ids = new byte[16];
                                if (readUpToAt(srcCbhCh, srcRecOff + 0x30, ids, 16) == 16) {
                                    int srcAnnoId = clampIntId(CbCommon.readI64LE(ids, 0));
                                    int srcSrcId = clampIntId(CbCommon.readI64LE(ids, 8));
                                    annotator = srcTwo.namebase.getAnnotator(srcAnnoId);
                                    source = srcTwo.namebase.getSource(srcSrcId);
                                }
                            }
                            int annotatorId = namebase.findOrAddAnnotator(annotator, annotatorUsesSlot0Fallback);
                            int sourceId = namebase.findOrAddSource(source);

                            long cbgOffset = appendTwoCbgBlock(rawCbg);
                            long cbaOffset = TWO_CBA_EMPTY_SENTINEL;
                            if (srcCbaCh != null && srcInfo.getCbaOffset() > TWO_CBA_EMPTY_SENTINEL) {
                                byte[] rawCba = readRawMagicBlock(srcCbaCh, srcInfo.getCbaOffset());
                                if (rawCba != null) {
                                    cbaOffset = appendTwoCbaBlock(rawCba);
                                }
                            }

                            byte[] rec = buildTwoCbhRecord(
                                    0x01010001L,
                                    cbgOffset,
                                    cbaOffset,
                                    whiteId,
                                    blackId,
                                    tournamentId,
                                    annotatorId,
                                    sourceId,
                                    srcInfo.getResultByte(),
                                    srcInfo.getRoundNumber(),
                                    srcInfo.getSubroundNumber(),
                                    srcInfo.getWhiteEloVal(),
                                    srcInfo.getBlackEloVal(),
                                    srcInfo.getEcoRaw(),
                                    srcInfo.getPackedDate()
                            );
                            long recOffset = TWO_CBH_HEADER_SIZE + ((long) rawIndex * TWO_CBH_RECORD_SIZE);
                            writeAllAt(twoCbhChannel, recOffset, rec);

                            TwoCbhGameInfo newInfo = new TwoCbhGameInfo(
                                    namebase,
                                    rawIndex,
                                    cbgOffset,
                                    cbaOffset,
                                    whiteId,
                                    blackId,
                                    tournamentId,
                                    srcInfo.getPackedDate(),
                                    srcInfo.getResultByte(),
                                    srcInfo.getRoundNumber(),
                                    srcInfo.getSubroundNumber(),
                                    srcInfo.getWhiteEloVal(),
                                    srcInfo.getBlackEloVal(),
                                    srcInfo.getEcoRaw()
                            );
                            entries.add(newInfo);
                            importedEntries.add(newInfo);
                            rawIndex++;
                            reportImportProgress(listener, i, total);
                            continue;
                        }
                    }
                    Game g = sourceDatabase.loadGame(info);
                    if (g != null) {
                        TwoCbhGameInfo newInfo = writeEncodedGameAt(g, rawIndex);
                        entries.add(newInfo);
                        importedEntries.add(newInfo);
                        rawIndex++;
                    }
                    reportImportProgress(listener, i, total);
                }
            } else {
                for (int i = 0; i < total; i++) {
                    if (listener != null && listener.isCancelled()) {
                        break;
                    }
                    GameInfo info = gamesToImport.get(i);
                    Game g = sourceDatabase.loadGame(info);
                    if (g != null) {
                        TwoCbhGameInfo newInfo = writeEncodedGameAt(g, rawIndex);
                        entries.add(newInfo);
                        importedEntries.add(newInfo);
                        rawIndex++;
                    }
                    reportImportProgress(listener, i, total);
                }
            }
        } finally {
            namebase.flushBatch();
            if (rawIndex > totalRawRecords) {
                totalRawRecords = rawIndex;
                updateTwoCbhHeaderRecordCount();
            }
        }

        if (!importedEntries.isEmpty()) {
            revision++;
            publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_APPENDED, this, revision, importedEntries));
        }
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo) throws IOException {
        replaceGame(newGame, currentGameInfo, null);
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo, ProgressListener listener) throws IOException {
        Objects.requireNonNull(newGame, "newGame");
        Objects.requireNonNull(currentGameInfo, "currentGameInfo");
        ensureWritable();
        ensureAnnotatorFallbackChecked();

        TwoCbhGameInfo target;
        if (currentGameInfo instanceof TwoCbhGameInfo tgi && entries.contains(tgi)) {
            target = tgi;
        } else {
            target = findEntryById(currentGameInfo.getId());
        }
        if (target == null) {
            throw new IllegalArgumentException("Game not found in 2CBH database: " + currentGameInfo.getId());
        }

        TwoCbhMoveEncoder.EncodeResult enc = TwoCbhMoveEncoder.encode(newGame);

        int whiteId = namebase.findOrAddPlayer(newGame.getHeader("White"));
        int blackId = namebase.findOrAddPlayer(newGame.getHeader("Black"));
        int tournamentId = namebase.findOrAddTournament(newGame.getHeader("Event"), newGame.getHeader("Site"));
        int annotatorId = namebase.findOrAddAnnotator(newGame.getHeader("Annotator"), annotatorUsesSlot0Fallback);
        int sourceId = namebase.findOrAddSource(newGame.getHeader("Source"));

        long cbgOffset = writeOrReplaceTwoCbgBlock(target.getCbgOffset(), enc.twoCbgBlock);
        long cbaOffset = TWO_CBA_EMPTY_SENTINEL;
        if (enc.twoCbaBlock != null) {
            cbaOffset = writeOrReplaceTwoCbaBlock(target.getCbaOffset(), enc.twoCbaBlock);
        }

        int packedDate = CbCommon.encodeDate(newGame.getHeader("Date"));
        int resultByte = CbCommon.encodeResult(newGame.getHeader("Result"), newGame.getResult());
        int[] rs = CbCommon.parseRoundAndSubround(newGame.getHeader("Round"));
        int whiteElo = CbCommon.parseElo(newGame.getHeader("WhiteElo"));
        int blackElo = CbCommon.parseElo(newGame.getHeader("BlackElo"));
        int ecoRaw = CbCommon.encodeEco(newGame.getHeader("ECO"));

        byte[] rec = buildTwoCbhRecord(
                0x01010001L,
                cbgOffset,
                cbaOffset,
                whiteId,
                blackId,
                tournamentId,
                annotatorId,
                sourceId,
                resultByte,
                rs[0],
                rs[1],
                whiteElo,
                blackElo,
                ecoRaw,
                packedDate
        );

        long recOffset = TWO_CBH_HEADER_SIZE + ((long) target.getRawIndex() * TWO_CBH_RECORD_SIZE);
        writeAllAt(twoCbhChannel, recOffset, rec);

        target.update(
                cbgOffset,
                cbaOffset,
                whiteId,
                blackId,
                tournamentId,
                packedDate,
                resultByte,
                rs[0],
                rs[1],
                whiteElo,
                blackElo,
                ecoRaw
        );

        revision++;
        if (listener != null) {
            listener.onProgress(100);
        }
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_REPLACED, this, revision, List.of(target)));
    }

    @Override
    public void deleteGame(GameInfo info) throws IOException {
        deleteGame(info, null);
    }

    @Override
    public void deleteGame(GameInfo info, ProgressListener listener) throws IOException {
        Objects.requireNonNull(info, "info");
        ensureWritable();

        TwoCbhGameInfo target;
        if (info instanceof TwoCbhGameInfo tgi && entries.contains(tgi)) {
            target = tgi;
        } else {
            target = findEntryById(info.getId());
        }
        if (target == null) {
            throw new IllegalArgumentException("Game not found in 2CBH database: " + info.getId());
        }

        long recOffset = TWO_CBH_HEADER_SIZE + ((long) target.getRawIndex() * TWO_CBH_RECORD_SIZE);
        byte[] flagBuf = new byte[8];
        long flags;
        if (readUpToAt(twoCbhChannel, recOffset, flagBuf, 8) == 8) {
            flags = CbCommon.readI64LE(flagBuf, 0) | 0x80L;
        } else {
            flags = 0x01010081L;
        }
        CbCommon.writeI64LE(flagBuf, 0, flags);
        writeAllAt(twoCbhChannel, recOffset, flagBuf);

        entries.remove(target);
        searchResults.remove(target);
        revision++;
        if (listener != null) {
            listener.onProgress(100);
        }
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_DELETED, this, revision, List.of(target)));
    }

    private void ensureWritable() throws IOException {
        if (!open || twoCbhChannel == null) {
            throw new IOException("2CBH database is not open");
        }
        if (isReadOnly()) {
            throw new UnsupportedOperationException("2CBH database is read-only");
        }
    }

    private void ensureAnnotatorFallbackChecked() throws IOException {
        if (annotatorFallbackChecked) {
            return;
        }
        annotatorFallbackChecked = true;
        if (namebase.getEntryCount(3) > 0L || twoCbhChannel == null) {
            annotatorUsesSlot0Fallback = false;
            return;
        }
        long fileSize = twoCbhChannel.size();
        if (fileSize <= TWO_CBH_HEADER_SIZE) {
            annotatorUsesSlot0Fallback = false;
            return;
        }
        long playerCount = namebase.getEntryCount(0);
        int totalRecords = (int) ((fileSize - TWO_CBH_HEADER_SIZE) / TWO_CBH_RECORD_SIZE);
        byte[] buf = new byte[Math.min(SCAN_BATCH_RECORDS, Math.max(1, totalRecords)) * TWO_CBH_RECORD_SIZE];
        long offset = TWO_CBH_HEADER_SIZE;
        int rawIdx = 0;
        while (rawIdx < totalRecords) {
            int batch = Math.min(SCAN_BATCH_RECORDS, totalRecords - rawIdx);
            int read = readUpToAt(twoCbhChannel, offset, buf, batch * TWO_CBH_RECORD_SIZE);
            int recs = read / TWO_CBH_RECORD_SIZE;
            if (recs <= 0) {
                break;
            }
            for (int i = 0; i < recs; i++) {
                int off = i * TWO_CBH_RECORD_SIZE;
                long flags = CbCommon.readI64LE(buf, off);
                if ((flags & 0x01L) != 0L && (flags & 0x02L) == 0L && (flags & 0x80L) == 0L) {
                    long annoId = CbCommon.readI64LE(buf, off + 0x30);
                    if (annoId >= 0L && annoId < playerCount) {
                        annotatorUsesSlot0Fallback = true;
                        return;
                    }
                }
            }
            rawIdx += recs;
            offset += (long) recs * TWO_CBH_RECORD_SIZE;
        }
        annotatorUsesSlot0Fallback = false;
    }

    private int nextRawIndex() throws IOException {
        long size = twoCbhChannel.size();
        if (size <= TWO_CBH_HEADER_SIZE) {
            return 0;
        }
        int onDisk = (int) ((size - TWO_CBH_HEADER_SIZE) / TWO_CBH_RECORD_SIZE);
        return Math.max(totalRawRecords, onDisk);
    }

    private void updateTwoCbhHeaderRecordCount() throws IOException {
        byte[] cnt = new byte[8];
        CbCommon.writeI64LE(cnt, 0, totalRawRecords + 1L);
        writeAllAt(twoCbhChannel, 0x10L, cnt);
    }

    private void ensureTwoCbgWritableChannel() throws IOException {
        if (twoCbgChannel == null) {
            if (twoCbgPath == null) {
                twoCbgPath = Path.of(basePath.toString() + ".2cbg");
            }
            if (!Files.exists(twoCbgPath)) {
                Files.write(twoCbgPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }
            twoCbgChannel = FileChannel.open(twoCbgPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
        }
    }

    private void ensureTwoCbaWritableChannel() throws IOException {
        if (twoCbaChannel == null) {
            if (twoCbaPath == null) {
                twoCbaPath = Path.of(basePath.toString() + ".2cba");
            }
            if (!Files.exists(twoCbaPath) || Files.size(twoCbaPath) < TWO_CBA_HEADER_SIZE) {
                Files.write(twoCbaPath, new byte[TWO_CBA_HEADER_SIZE],
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
            twoCbaChannel = FileChannel.open(twoCbaPath, StandardOpenOption.READ, StandardOpenOption.WRITE);
        } else if (twoCbaChannel.size() < TWO_CBA_HEADER_SIZE) {
            writeAllAt(twoCbaChannel, 0L, new byte[TWO_CBA_HEADER_SIZE]);
        }
    }

    private long appendTwoCbgBlock(byte[] block) throws IOException {
        ensureTwoCbgWritableChannel();
        long offset = twoCbgChannel.size();
        writeAllAt(twoCbgChannel, offset, block);
        return offset;
    }

    private long appendTwoCbaBlock(byte[] block) throws IOException {
        ensureTwoCbaWritableChannel();
        long offset = Math.max(TWO_CBA_HEADER_SIZE, twoCbaChannel.size());
        writeAllAt(twoCbaChannel, offset, block);
        return offset;
    }

    private long writeOrReplaceTwoCbgBlock(long oldOffset, byte[] newBlock) throws IOException {
        ensureTwoCbgWritableChannel();
        if (oldOffset >= 0L && oldOffset + 24L <= twoCbgChannel.size()) {
            byte[] hdr = new byte[24];
            if (readUpToAt(twoCbgChannel, oldOffset, hdr, 24) == 24) {
                long magic = CbCommon.readI64LE(hdr, 0);
                int oldPayLen = CbCommon.readI32LE(hdr, 8);
                if (magic == TWO_CBG_MAGIC && oldPayLen >= 0 && newBlock.length <= 24L + oldPayLen) {
                    writeAllAt(twoCbgChannel, oldOffset, newBlock);
                    return oldOffset;
                }
            }
        }
        return appendTwoCbgBlock(newBlock);
    }

    private long writeOrReplaceTwoCbaBlock(long oldOffset, byte[] newBlock) throws IOException {
        ensureTwoCbaWritableChannel();
        if (oldOffset > TWO_CBA_EMPTY_SENTINEL && oldOffset + 24L <= twoCbaChannel.size()) {
            byte[] hdr = new byte[24];
            if (readUpToAt(twoCbaChannel, oldOffset, hdr, 24) == 24) {
                long magic = CbCommon.readI64LE(hdr, 0);
                int oldPayLen = CbCommon.readI32LE(hdr, 8);
                if (magic == TWO_CBG_MAGIC && oldPayLen >= 0 && newBlock.length <= 24L + oldPayLen) {
                    writeAllAt(twoCbaChannel, oldOffset, newBlock);
                    return oldOffset;
                }
            }
        }
        return appendTwoCbaBlock(newBlock);
    }

    private TwoCbhGameInfo writeEncodedGameAt(Game game, int rawIndex) throws IOException {
        TwoCbhMoveEncoder.EncodeResult enc = TwoCbhMoveEncoder.encode(game);

        int whiteId = namebase.findOrAddPlayer(game.getHeader("White"));
        int blackId = namebase.findOrAddPlayer(game.getHeader("Black"));
        int tournamentId = namebase.findOrAddTournament(game.getHeader("Event"), game.getHeader("Site"));
        int annotatorId = namebase.findOrAddAnnotator(game.getHeader("Annotator"), annotatorUsesSlot0Fallback);
        int sourceId = namebase.findOrAddSource(game.getHeader("Source"));

        long cbgOffset = appendTwoCbgBlock(enc.twoCbgBlock);
        long cbaOffset = TWO_CBA_EMPTY_SENTINEL;
        if (enc.twoCbaBlock != null) {
            cbaOffset = appendTwoCbaBlock(enc.twoCbaBlock);
        }

        int packedDate = CbCommon.encodeDate(game.getHeader("Date"));
        int resultByte = CbCommon.encodeResult(game.getHeader("Result"), game.getResult());
        int[] rs = CbCommon.parseRoundAndSubround(game.getHeader("Round"));
        int whiteElo = CbCommon.parseElo(game.getHeader("WhiteElo"));
        int blackElo = CbCommon.parseElo(game.getHeader("BlackElo"));
        int ecoRaw = CbCommon.encodeEco(game.getHeader("ECO"));

        byte[] rec = buildTwoCbhRecord(
                0x01010001L,
                cbgOffset,
                cbaOffset,
                whiteId,
                blackId,
                tournamentId,
                annotatorId,
                sourceId,
                resultByte,
                rs[0],
                rs[1],
                whiteElo,
                blackElo,
                ecoRaw,
                packedDate
        );
        long recOffset = TWO_CBH_HEADER_SIZE + ((long) rawIndex * TWO_CBH_RECORD_SIZE);
        writeAllAt(twoCbhChannel, recOffset, rec);

        return new TwoCbhGameInfo(
                namebase,
                rawIndex,
                cbgOffset,
                cbaOffset,
                whiteId,
                blackId,
                tournamentId,
                packedDate,
                resultByte,
                rs[0],
                rs[1],
                whiteElo,
                blackElo,
                ecoRaw
        );
    }

    private static byte[] buildTwoCbhRecord(
            long flags,
            long cbgOffset,
            long cbaOffset,
            long whiteId,
            long blackId,
            long tournamentId,
            long annotatorId,
            long sourceId,
            int resultByte,
            int round,
            int subround,
            int whiteElo,
            int blackElo,
            int ecoRaw,
            int packedDate) {
        byte[] rec = new byte[TWO_CBH_RECORD_SIZE];
        CbCommon.writeI64LE(rec, 0x00, flags);
        CbCommon.writeI64LE(rec, 0x08, cbgOffset);
        CbCommon.writeI64LE(rec, 0x10, cbaOffset);
        CbCommon.writeI64LE(rec, 0x18, whiteId);
        CbCommon.writeI64LE(rec, 0x20, blackId);
        CbCommon.writeI64LE(rec, 0x28, tournamentId);
        CbCommon.writeI64LE(rec, 0x30, annotatorId);
        CbCommon.writeI64LE(rec, 0x38, sourceId);
        rec[0x58] = (byte) (resultByte & 0xFF);
        rec[0x5A] = (byte) (round & 0xFF);
        rec[0x5C] = (byte) (subround & 0xFF);
        CbCommon.writeU16LE(rec, 0x60, whiteElo);
        CbCommon.writeU16LE(rec, 0x70, blackElo);
        CbCommon.writeU16LE(rec, 0x80, ecoRaw);
        CbCommon.writeI32LE(rec, 0xBC, packedDate);
        return rec;
    }

    private static byte[] readRawMagicBlock(FileChannel ch, long offset) throws IOException {
        long size = ch.size();
        if (offset < 0L || offset + 24L > size) {
            return null;
        }
        byte[] hdr = new byte[24];
        if (readUpToAt(ch, offset, hdr, 24) < 24) {
            return null;
        }
        long magic = CbCommon.readI64LE(hdr, 0);
        int payloadLen = CbCommon.readI32LE(hdr, 8);
        if (magic != TWO_CBG_MAGIC || payloadLen < 4 || payloadLen > 16_777_216) {
            return null;
        }
        int totalLen = 24 + payloadLen;
        if (offset + totalLen > size) {
            return null;
        }
        byte[] full = new byte[totalLen];
        if (readUpToAt(ch, offset, full, totalLen) < totalLen) {
            return null;
        }
        return full;
    }

    private static void reportImportProgress(ProgressListener listener, int index, int total) {
        if (listener != null && (index % 50 == 0 || index == total - 1)) {
            int pct = (int) (((long) (index + 1) * 100L) / total);
            listener.onProgress(pct);
        }
    }

    private static void writeAllAt(FileChannel ch, long offset, byte[] src) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(src);
        long pos = offset;
        while (bb.hasRemaining()) {
            int n = ch.write(bb, pos);
            pos += n;
        }
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

        if (searchHeader) {
            try {
                if (pattern.getWhiteName() != null && !pattern.getWhiteName().isEmpty()) {
                    whiteMask = namebase.buildPlayerMatchMask(pattern.getWhiteName().toLowerCase(Locale.ROOT));
                }
                if (pattern.getBlackName() != null && !pattern.getBlackName().isEmpty()) {
                    blackMask = namebase.buildPlayerMatchMask(pattern.getBlackName().toLowerCase(Locale.ROOT));
                }
                if (pattern.getEvent() != null && !pattern.getEvent().isEmpty()) {
                    eventMask = namebase.buildTournamentMatchMask(pattern.getEvent().toLowerCase(Locale.ROOT), false);
                }
                if (pattern.getSite() != null && !pattern.getSite().isEmpty()) {
                    siteMask = namebase.buildTournamentMatchMask(pattern.getSite().toLowerCase(Locale.ROOT), true);
                }
            } catch (IOException ignored) {
            }
        }

        if (!searchPos) {
            int total = entries.size();
            for (int i = 0; i < total; i++) {
                if (listener != null && listener.isCancelled()) {
                    break;
                }
                TwoCbhGameInfo e = entries.get(i);
                if (!searchHeader || matchesTwoCbhHeader(e, pattern, whiteMask, blackMask, eventMask, siteMask, ignoreColor)) {
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
                TwoCbgPositionScanner posScanner = new TwoCbgPositionScanner();
                FileChannel myCbg = null;
                if (twoCbgPath != null && Files.exists(twoCbgPath)) {
                    myCbg = FileChannel.open(twoCbgPath, StandardOpenOption.READ);
                }
                int localCount = 0;
                try {
                    for (int i = start; i < end; i++) {
                        if (listener != null && listener.isCancelled()) {
                            break;
                        }
                        TwoCbhGameInfo entry = entries.get(i);
                        if (searchHeader && !matchesTwoCbhHeader(entry, pattern, wM, bM, eM, sM, ignoreColor)) {
                            localCount++;
                            continue;
                        }
                        if (myCbg != null && posScanner.scanTwoCbg(myCbg, entry.getCbgOffset(), matcher)) {
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

    private static boolean matchesTwoCbhHeader(
            TwoCbhGameInfo info,
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
     * Zero-allocation per-thread position replay scanner for `.2cbg` streams using
     * {@link ScidPosition} and {@link Scid5FastSearchScanner#matches(ScidPosition)}.
     */
    private static final class TwoCbgPositionScanner {
        private final byte[] hdr = new byte[24];
        private byte[] buf = new byte[16384];
        private final ScidPosition scidPos = new ScidPosition();
        private final ScidPosition prevPos = new ScidPosition();
        private final ScidPosition[] posStack = new ScidPosition[64];
        private final String[] fenOut = new String[1];

        TwoCbgPositionScanner() {
            for (int i = 0; i < posStack.length; i++) {
                posStack[i] = new ScidPosition();
            }
        }

        boolean scanTwoCbg(FileChannel channel, long cbgOffset, Scid5FastSearchScanner matcher) throws IOException {
            if (cbgOffset < 0) {
                return false;
            }
            if (readUpToAt(channel, cbgOffset, hdr, 24) < 24) {
                return false;
            }
            long magic = CbCommon.readI64LE(hdr, 0);
            int payloadLen = CbCommon.readI32LE(hdr, 8);
            if (magic != TWO_CBG_MAGIC || payloadLen < 4 || payloadLen > 16_777_216) {
                return false;
            }
            if (buf.length < payloadLen) {
                buf = new byte[Math.max(payloadLen, buf.length * 2)];
            }
            int read = readUpToAt(channel, cbgOffset + 24L, buf, payloadLen);
            int wordCount = read / 2;
            if (wordCount < 2) {
                return false;
            }
            int w0 = CbCommon.readU16LE(buf, 0);
            int w1 = CbCommon.readU16LE(buf, 2);
            int wIdx;
            if (w0 == 0x0001 && w1 == 0xFFFC) {
                scidPos.stdStart();
                wIdx = 2;
            } else if (w0 == 0x0001 && w1 == 0xFFFB) {
                wIdx = TwoCbhMoveTable.decodeSetup(buf, read, fenOut);
                if (fenOut[0] == null) {
                    return false;
                }
                scidPos.setupFen(fenOut[0]);
            } else {
                return false;
            }

            if (matcher.matches(scidPos)) {
                return true;
            }

            prevPos.copyFrom(scidPos);
            boolean hasPrev = false;
            int depth = 0;
            while (wIdx < wordCount) {
                int w = CbCommon.readU16LE(buf, wIdx * 2);
                wIdx++;
                if (w == 0xFFFD) {
                    if (hasPrev && depth < posStack.length) {
                        posStack[depth].copyFrom(prevPos);
                        depth++;
                    }
                    continue;
                }
                if (w == 0xFFFF) {
                    if (depth == 0) {
                        break;
                    }
                    depth--;
                    scidPos.copyFrom(posStack[depth]);
                    hasPrev = false;
                    continue;
                }
                prevPos.copyFrom(scidPos);
                hasPrev = true;
                if (w == 0xFFFA) {
                    scidPos.toMove = 1 - scidPos.toMove;
                } else if (w >= 1 && w <= TwoCbhMoveTable.TABLE_SIZE) {
                    int packed = TwoCbhMoveTable.MOVES[w];
                    int fromSq = TwoCbhMoveTable.fromSq64(packed);
                    int toSq = TwoCbhMoveTable.toSq64(packed);
                    int promo = CbhChessDatabase.toScidPromo(TwoCbhMoveTable.promoConst(packed));
                    boolean isKing = scidPos.listPos[fromSq] == 0;
                    boolean castleK = false;
                    boolean castleQ = false;
                    if (isKing) {
                        if (scidPos.toMove == ScidPosition.WHITE) {
                            if (fromSq == 4 && toSq == 6) {
                                castleK = true;
                            } else if (fromSq == 4 && toSq == 2) {
                                castleQ = true;
                            }
                        } else {
                            if (fromSq == 60 && toSq == 62) {
                                castleK = true;
                            } else if (fromSq == 60 && toSq == 58) {
                                castleQ = true;
                            }
                        }
                    }
                    scidPos.doMove(fromSq, toSq, promo, castleK, castleQ);
                } else {
                    continue;
                }
                if (matcher.matches(scidPos)) {
                    return true;
                }
            }
            return false;
        }
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
