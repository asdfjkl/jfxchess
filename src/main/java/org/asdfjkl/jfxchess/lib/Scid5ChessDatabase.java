/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2026 Dominik Klein
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
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Scid5ChessDatabase implements ChessDatabase {

    private Path basePath;
    private Path si5Path;
    private Path sn5Path;
    private Path sg5Path;
    private boolean open;

    private final ArrayList<Scid5GameInfo> allEntries = new ArrayList<>();
    private final ArrayList<Scid5GameInfo> entries = new ArrayList<>();
    private final ArrayList<GameInfo> searchResults = new ArrayList<>();
    private boolean searchActive;
    private boolean includeDeleted = false;

    private final Scid5Namebase namebase = new Scid5Namebase();
    private final List<ChessDatabaseListener> listeners = new ArrayList<>();
    private long revision;

    public Scid5ChessDatabase() {
    }

    public Scid5ChessDatabase(String filename) throws IOException {
        open(filename);
    }

    private Path resolveBasePath(String filename) {
        Objects.requireNonNull(filename, "filename");
        Path p = Path.of(filename).toAbsolutePath().normalize();
        String name = p.getFileName().toString();
        String lower = name.toLowerCase();
        if (lower.endsWith(".si5") || lower.endsWith(".sn5") || lower.endsWith(".sg5")) {
            String baseName = name.substring(0, name.length() - 4);
            Path parent = p.getParent();
            return (parent != null) ? parent.resolve(baseName) : Path.of(baseName);
        }
        return p;
    }

    @Override
    public void open(String filename) throws IOException {
        Path base = resolveBasePath(filename);
        Path si5 = Path.of(base + ".si5");
        Path sn5 = Path.of(base + ".sn5");
        Path sg5 = Path.of(base + ".sg5");

        if (!Files.exists(si5)) {
            throw new FileNotFoundException("SCID5 index file not found: " + si5);
        }

        this.basePath = base;
        this.si5Path = si5;
        this.sn5Path = sn5;
        this.sg5Path = sg5;
        this.open = true;

        this.allEntries.clear();
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.namebase.clear();
        this.revision = 0;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void createNew(String filename) throws IOException {
        Path base = resolveBasePath(filename);
        Path parent = base.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path si5 = Path.of(base + ".si5");
        Path sn5 = Path.of(base + ".sn5");
        Path sg5 = Path.of(base + ".sg5");

        Files.write(si5, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(sn5, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.write(sg5, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        this.basePath = base;
        this.si5Path = si5;
        this.sn5Path = sn5;
        this.sg5Path = sg5;
        this.open = true;

        this.allEntries.clear();
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.namebase.clear();
        this.revision = 0;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        this.allEntries.clear();
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.namebase.clear();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getFilename() {
        return si5Path == null ? "" : si5Path.toString();
    }

    @Override
    public Path getPath() {
        return si5Path;
    }

    public Path getBasePath() {
        return basePath;
    }

    public Path getSi5Path() {
        return si5Path;
    }

    public Path getSn5Path() {
        return sn5Path;
    }

    public Path getSg5Path() {
        return sg5Path;
    }

    public Scid5Namebase getNamebase() {
        return namebase;
    }

    @Override
    public void scanGames() {
        scanGames(null);
    }

    @Override
    public void scanGames(ProgressListener listener) {
        if (!open || si5Path == null) {
            return;
        }
        try {
            if (sn5Path != null && Files.exists(sn5Path)) {
                namebase.read(sn5Path);
            }
            ArrayList<Scid5GameInfo> scanned = Scid5Index.readAll(si5Path, namebase, listener);
            allEntries.clear();
            allEntries.addAll(scanned);
            entries.clear();
            for (Scid5GameInfo info : scanned) {
                if (includeDeleted || !info.isDeleted()) {
                    entries.add(info);
                }
            }
            revision++;
            publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.INDEX_REBUILT, this, revision, null));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan SCID5 games: " + e.getMessage(), e);
        }
    }

    public void setIncludeDeleted(boolean include) {
        if (this.includeDeleted != include) {
            this.includeDeleted = include;
            entries.clear();
            for (Scid5GameInfo info : allEntries) {
                if (includeDeleted || !info.isDeleted()) {
                    entries.add(info);
                }
            }
        }
    }

    public boolean isIncludeDeleted() {
        return includeDeleted;
    }

    public ArrayList<GameInfo> getAllEntries() {
        return new ArrayList<>(allEntries);
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
            throw new IOException("SCID5 database is not open");
        }
        if (info == null) {
            throw new IllegalArgumentException("GameInfo cannot be null");
        }
        Scid5GameInfo scidInfo;
        if (info instanceof Scid5GameInfo sgi) {
            scidInfo = sgi;
        } else {
            scidInfo = findEntryById(info.getId());
            if (scidInfo == null) {
                throw new IOException("Game not found in database: " + info.getId());
            }
        }

        if (scidInfo.isModified() && scidInfo.getModifiedGame() != null) {
            return scidInfo.getModifiedGame();
        }

        if (sg5Path == null || !Files.exists(sg5Path)) {
            throw new IOException("SCID5 game data file (.sg5) not found: " + sg5Path);
        }

        long offset = scidInfo.getSg5Offset();
        int length = scidInfo.getSg5Length();
        byte[] blob = new byte[length];

        try (RandomAccessFile raf = new RandomAccessFile(sg5Path.toFile(), "r")) {
            raf.seek(offset);
            raf.readFully(blob);
        }

        return Scid5MoveDecoder.decode(blob, 0, length, scidInfo);
    }

    private Scid5GameInfo findEntryById(UUID id) {
        if (id == null) return null;
        for (Scid5GameInfo entry : allEntries) {
            if (id.equals(entry.getId())) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public GameInfo appendGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        if (!open) {
            throw new IOException("SCID5 database is not open");
        }

        // 1. Resolve names in namebase and append to .sn5 if new
        String white = Objects.toString(game.getHeader("White"), "");
        String black = Objects.toString(game.getHeader("Black"), "");
        String event = Objects.toString(game.getHeader("Event"), "");
        String site = Objects.toString(game.getHeader("Site"), "");
        String round = Objects.toString(game.getHeader("Round"), "");
        String date = game.getHeader("Date");
        String eventDate = game.getHeader("EventDate");
        String result = game.getHeader("Result");
        String eco = game.getHeader("ECO");
        String whiteElo = game.getHeader("WhiteElo");
        String blackElo = game.getHeader("BlackElo");

        int whiteId = namebase.findOrAdd(sn5Path, white, Scid5Namebase.NAME_PLAYER);
        int blackId = namebase.findOrAdd(sn5Path, black, Scid5Namebase.NAME_PLAYER);
        int eventId = namebase.findOrAdd(sn5Path, event, Scid5Namebase.NAME_EVENT);
        int siteId = namebase.findOrAdd(sn5Path, site, Scid5Namebase.NAME_SITE);
        int roundId = namebase.findOrAdd(sn5Path, round, Scid5Namebase.NAME_ROUND);

        // 2. Encode game moves, tags, variations, and comments to .sg5 blob
        Scid5EncodeResult enc = Scid5MoveEncoder.encode(game);
        byte[] blob = enc.data;
        int dataSize = enc.dataSize;

        // 3. Determine .sg5 offset respecting 128 KB block boundaries
        long currentOffset = Files.exists(sg5Path) ? Files.size(sg5Path) : 0;
        long currentBlock = currentOffset / 131072L;
        long endBlock = (currentOffset + dataSize - 1) / 131072L;
        long actualOffset = currentOffset;

        if (endBlock != currentBlock) {
            long nextBlockOffset = (currentBlock + 1) * 131072L;
            int pad = (int) (nextBlockOffset - currentOffset);
            byte[] padBytes = new byte[pad];
            Files.write(sg5Path, padBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            actualOffset = nextBlockOffset;
        }

        Files.write(sg5Path, Arrays.copyOf(blob, dataSize), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // 4. Construct Scid5GameInfo record
        int newGameNumber = allEntries.size();
        Scid5GameInfo info = new Scid5GameInfo();
        info.setGameNumber(newGameNumber);
        info.setSg5Offset(actualOffset);
        info.setSg5Length(dataSize);

        info.setWhiteId(whiteId);
        info.setWhite(white);
        info.setBlackId(blackId);
        info.setBlack(black);
        info.setEventId(eventId);
        info.setEvent(event);
        info.setSiteId(siteId);
        info.setSite(site);
        info.setRoundId(roundId);
        info.setRound(round);

        info.setWhiteEloVal(parseElo(whiteElo));
        info.setBlackEloVal(parseElo(blackElo));
        info.setGameDatePacked(Scid5GameInfo.encodeDate(date));
        info.setEventDatePacked(Scid5GameInfo.encodeDate(eventDate));

        info.setResult(result != null ? result : "*");
        info.setEcoCode(Scid5GameInfo.encodeEco(eco));

        info.setHalfMoves(enc.halfMoves);
        info.setFlags(enc.flags);
        info.setCommentRating(enc.commentRating);
        info.setVariationRating(enc.variationRating);
        info.setNagRating(enc.nagRating);
        info.setFinalMatSig(enc.finalMatSig);
        info.setHomePawnCount(enc.homePawnCount);
        info.setHomePawnData(enc.homePawnData);
        info.markValid();

        // 5. Append 56-byte record to .si5
        Scid5Index.appendEntry(si5Path, info);

        // 6. Update in-memory collections
        allEntries.add(info);
        entries.add(info);
        revision++;

        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_APPENDED, this, revision, List.of(info)));
        return info;
    }

    public GameInfo writeSingleGame(Game game) throws IOException {
        createNew(getFilename());
        return appendGame(game);
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo) throws IOException {
        replaceGame(newGame, currentGameInfo, null);
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo, ProgressListener listener) throws IOException {
        Objects.requireNonNull(newGame, "newGame");
        Objects.requireNonNull(currentGameInfo, "currentGameInfo");
        if (!open) {
            throw new IOException("SCID5 database is not open");
        }

        Scid5GameInfo target;
        if (currentGameInfo instanceof Scid5GameInfo sgi && allEntries.contains(sgi)) {
            target = sgi;
        } else {
            target = findEntryById(currentGameInfo.getId());
        }
        if (target == null) {
            throw new IllegalArgumentException("Game not found in database: " + currentGameInfo.getId());
        }

        // 1. Resolve names in namebase
        String white = Objects.toString(newGame.getHeader("White"), "");
        String black = Objects.toString(newGame.getHeader("Black"), "");
        String event = Objects.toString(newGame.getHeader("Event"), "");
        String site = Objects.toString(newGame.getHeader("Site"), "");
        String round = Objects.toString(newGame.getHeader("Round"), "");
        String date = newGame.getHeader("Date");
        String eventDate = newGame.getHeader("EventDate");
        String result = newGame.getHeader("Result");
        String eco = newGame.getHeader("ECO");
        String whiteElo = newGame.getHeader("WhiteElo");
        String blackElo = newGame.getHeader("BlackElo");

        int whiteId = namebase.findOrAdd(sn5Path, white, Scid5Namebase.NAME_PLAYER);
        int blackId = namebase.findOrAdd(sn5Path, black, Scid5Namebase.NAME_PLAYER);
        int eventId = namebase.findOrAdd(sn5Path, event, Scid5Namebase.NAME_EVENT);
        int siteId = namebase.findOrAdd(sn5Path, site, Scid5Namebase.NAME_SITE);
        int roundId = namebase.findOrAdd(sn5Path, round, Scid5Namebase.NAME_ROUND);

        // 2. Encode new game
        Scid5EncodeResult enc = Scid5MoveEncoder.encode(newGame);
        byte[] blob = enc.data;
        int dataSize = enc.dataSize;

        // 3. Write blob to .sg5: in-place if dataSize <= target.getSg5Length(), else append
        long actualOffset = target.getSg5Offset();
        if (dataSize <= target.getSg5Length()) {
            try (RandomAccessFile raf = new RandomAccessFile(sg5Path.toFile(), "rw")) {
                raf.seek(actualOffset);
                raf.write(blob, 0, dataSize);
            }
        } else {
            long currentOffset = Files.size(sg5Path);
            long currentBlock = currentOffset / 131072L;
            long endBlock = (currentOffset + dataSize - 1) / 131072L;
            actualOffset = currentOffset;

            if (endBlock != currentBlock) {
                long nextBlockOffset = (currentBlock + 1) * 131072L;
                int pad = (int) (nextBlockOffset - currentOffset);
                byte[] padBytes = new byte[pad];
                Files.write(sg5Path, padBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                actualOffset = nextBlockOffset;
            }

            Files.write(sg5Path, Arrays.copyOf(blob, dataSize), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }

        // 4. Update target fields
        target.setSg5Offset(actualOffset);
        target.setSg5Length(dataSize);

        target.setWhiteId(whiteId);
        target.setWhite(white);
        target.setBlackId(blackId);
        target.setBlack(black);
        target.setEventId(eventId);
        target.setEvent(event);
        target.setSiteId(siteId);
        target.setSite(site);
        target.setRoundId(roundId);
        target.setRound(round);

        target.setWhiteEloVal(parseElo(whiteElo));
        target.setBlackEloVal(parseElo(blackElo));
        target.setGameDatePacked(Scid5GameInfo.encodeDate(date));
        target.setEventDatePacked(Scid5GameInfo.encodeDate(eventDate));

        target.setResult(result != null ? result : "*");
        target.setEcoCode(Scid5GameInfo.encodeEco(eco));

        target.setHalfMoves(enc.halfMoves);
        target.setFlags(enc.flags);
        target.setCommentRating(enc.commentRating);
        target.setVariationRating(enc.variationRating);
        target.setNagRating(enc.nagRating);
        target.setFinalMatSig(enc.finalMatSig);
        target.setHomePawnCount(enc.homePawnCount);
        target.setHomePawnData(enc.homePawnData);

        // Reset in-memory buffer if present
        target.setModified(false);
        target.setModifiedGame(null);

        // 5. Update .si5 record on disk
        Scid5Index.updateEntry(si5Path, target.getGameNumber(), target);

        // 6. Notify
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
        if (!open) {
            throw new IOException("SCID5 database is not open");
        }

        Scid5GameInfo target;
        if (info instanceof Scid5GameInfo sgi && allEntries.contains(sgi)) {
            target = sgi;
        } else {
            target = findEntryById(info.getId());
        }
        if (target == null) {
            throw new IllegalArgumentException("Game not found in database: " + info.getId());
        }

        // Set deleted flag in record
        target.setDeleted(true);

        // Update record on disk in .si5
        Scid5Index.updateEntry(si5Path, target.getGameNumber(), target);

        // Remove from active entries if not including deleted
        if (!includeDeleted) {
            entries.remove(target);
        }

        revision++;
        if (listener != null) {
            listener.onProgress(100);
        }
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.GAME_DELETED, this, revision, List.of(target)));
    }

    private static int parseElo(String elo) {
        if (elo == null || elo.isBlank()) return 0;
        try {
            int val = Integer.parseInt(elo.trim());
            return Math.max(0, Math.min(4000, val));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void search(SearchPattern pattern) {
        search(pattern, null);
    }

    @Override
    public void search(SearchPattern pattern, ProgressListener listener) {
        Objects.requireNonNull(pattern, "pattern");
        searchResults.clear();
        for (int i = 0; i < entries.size(); i++) {
            if (listener != null && listener.isCancelled()) {
                break;
            }
            Scid5GameInfo entry = entries.get(i);
            if (pattern.matchesHeader(entry)) {
                searchResults.add(entry);
            }
            if (listener != null && i % 1000 == 0 && !entries.isEmpty()) {
                int percent = (int) ((long) i * 100 / entries.size());
                listener.onProgress(percent);
            }
        }
        if (listener != null && !listener.isCancelled()) {
            listener.onProgress(100);
        }
        searchActive = true;
    }

    @Override
    public void addListener(ChessDatabaseListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(ChessDatabaseListener listener) {
        listeners.remove(listener);
    }

    public long getRevision() {
        return revision;
    }

    private void publish(ChessDatabaseEvent event) {
        for (ChessDatabaseListener listener : List.copyOf(listeners)) {
            listener.onDatabaseChanged(event);
        }
    }
}
