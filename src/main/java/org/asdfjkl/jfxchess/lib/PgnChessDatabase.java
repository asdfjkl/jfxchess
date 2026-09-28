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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PgnChessDatabase implements ChessDatabase {

    private Path path;
    private boolean open;
    private final ArrayList<PgnGameInfo> entries = new ArrayList<>();
    private final ArrayList<GameInfo> searchResults = new ArrayList<>();
    private boolean searchActive;
    private final List<ChessDatabaseListener> listeners = new ArrayList<>();
    private final Map<UUID, String> fingerprints = new HashMap<>();
    private long revision;

    public PgnChessDatabase() {
    }

    public PgnChessDatabase(String filename) throws IOException {
        open(filename);
    }

    @Override
    public void open(String filename) throws IOException {
        Objects.requireNonNull(filename, "filename");
        Path candidatePath = Path.of(filename).toAbsolutePath().normalize();
        if (!Files.exists(candidatePath)) {
            throw new FileNotFoundException("PGN file not found: " + filename);
        }
        this.path = candidatePath;
        this.open = true;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.fingerprints.clear();
        this.revision = 0;
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void createNew(String filename) throws IOException {
        Objects.requireNonNull(filename, "filename");
        Path candidatePath = Path.of(filename).toAbsolutePath().normalize();
        Path parent = candidatePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(candidatePath, "", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        this.path = candidatePath;
        this.open = true;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.fingerprints.clear();
        this.revision = 0;
        publish(new ChessDatabaseEvent(ChessDatabaseEvent.Type.DATABASE_OPENED, this, revision, null));
    }

    @Override
    public void close() throws IOException {
        this.open = false;
        this.entries.clear();
        this.searchResults.clear();
        this.searchActive = false;
        this.fingerprints.clear();
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public String getFilename() {
        return path == null ? "" : path.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    @Override
    public void scanGames() {
        scanGames(null);
    }

    @Override
    public void scanGames(ProgressListener listener) {
        scanGamesInternal(listener, ChessDatabaseEvent.Type.INDEX_REBUILT, null, null);
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
        if (path == null) {
            throw new IOException("PGN database is not open");
        }
        PgnGameInfo pgnInfo;
        if (info instanceof PgnGameInfo pgn) {
            pgnInfo = pgn;
        } else {
            pgnInfo = findPgnGameInfo(info);
        }
        if (pgnInfo == null) {
            throw new IllegalArgumentException("Game not found in database: " + info.getId());
        }
        OptimizedRandomAccessFile raf = new OptimizedRandomAccessFile(path.toString(), "r");
        try {
            raf.seek(pgnInfo.getOffset());
            return new PgnReader().readGame(raf);
        } finally {
            raf.close();
        }
    }

    @Override
    public GameInfo appendGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        if (path == null) {
            throw new IOException("PGN database is not open");
        }
        String gamePgn = new PgnPrinter().printGame(game);
        Files.writeString(path, "\n\n" + gamePgn + "\n\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        List<GameInfo> previousEntries = new ArrayList<>(entries);
        scanGamesInternal(null, ChessDatabaseEvent.Type.GAME_APPENDED, null, previousEntries);
        if (entries.isEmpty()) {
            throw new IOException("Appended game could not be found while rescanning " + path);
        }
        return entries.get(entries.size() - 1);
    }

    public GameInfo writeSingleGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        if (path == null) {
            throw new IOException("PGN database is not open");
        }
        String gamePgn = new PgnPrinter().printGame(game);
        Files.writeString(path, gamePgn + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        scanGames();
        if (entries.isEmpty()) {
            throw new IOException("Saved game could not be found in " + path);
        }
        return entries.get(0);
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo) throws IOException {
        replaceGame(newGame, currentGameInfo, null);
    }

    @Override
    public void replaceGame(Game newGame, GameInfo currentGameInfo, ProgressListener listener)
            throws IOException {
        Objects.requireNonNull(newGame, "newGame");
        Objects.requireNonNull(currentGameInfo, "currentGameInfo");
        if (path == null) {
            throw new IOException("PGN database is not open");
        }
        PgnGameInfo pgnInfo = findPgnGameInfo(currentGameInfo);
        if (pgnInfo == null) {
            throw new IllegalArgumentException("Game not found in database: " + currentGameInfo.getId());
        }
        int index = entries.indexOf(pgnInfo);
        long startOffset = pgnInfo.getOffset();
        long endOffset = (index + 1 < entries.size())
                ? entries.get(index + 1).getOffset()
                : Files.size(path);

        String gamePgn = new PgnPrinter().printGame(newGame);
        replaceRange(startOffset, endOffset, "\n" + gamePgn + "\n\n");
        scanGamesInternal(listener, ChessDatabaseEvent.Type.GAME_REPLACED, pgnInfo.getId(), null);
    }

    @Override
    public void deleteGame(GameInfo info) throws IOException {
        deleteGame(info, null);
    }

    @Override
    public void deleteGame(GameInfo info, ProgressListener listener) throws IOException {
        Objects.requireNonNull(info, "info");
        if (path == null) {
            throw new IOException("PGN database is not open");
        }
        PgnGameInfo pgnInfo = findPgnGameInfo(info);
        if (pgnInfo == null) {
            throw new IllegalArgumentException("Game not found in database: " + info.getId());
        }
        int index = entries.indexOf(pgnInfo);
        long startOffset = pgnInfo.getOffset();
        long endOffset = (index + 1 < entries.size())
                ? entries.get(index + 1).getOffset()
                : Files.size(path);

        replaceRange(startOffset, endOffset, "");
        scanGamesInternal(listener, ChessDatabaseEvent.Type.GAME_DELETED, pgnInfo.getId(), null);
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
            PgnGameInfo entry = entries.get(i);
            if (pattern.matchesHeader(entry)) {
                searchResults.add(entry);
            }
            if (listener != null && i % 10000 == 0 && !entries.isEmpty()) {
                int percent = (int) ((long) i * 100 / entries.size());
                listener.onProgress(percent);
            }
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

    private void scanGamesInternal(ProgressListener listener,
                                    ChessDatabaseEvent.Type eventType,
                                    UUID forcedAffectedId,
                                    List<GameInfo> knownUnchangedGames) {
        if (path == null) {
            return;
        }

        ArrayList<PgnGameInfo> scannedEntries = new ArrayList<>();
        boolean inComment = false;
        long game_pos = -1;
        PgnGameInfo current = null;
        long last_pos = 0;
        String currentLine;
        OptimizedRandomAccessFile raf = null;

        File file = path.toFile();
        long fileSize = file.length();
        long gamesRead = 0;

        try {
            raf = new OptimizedRandomAccessFile(path.toString(), "r");
            while ((currentLine = raf.readLine()) != null) {
                if (listener != null && listener.isCancelled()) {
                    break;
                }
                if (currentLine.startsWith("%")) {
                    continue;
                }
                if (!inComment && currentLine.startsWith("[")) {
                    if (game_pos == -1) {
                        game_pos = last_pos;
                        current = new PgnGameInfo();
                    }
                    last_pos = raf.getFilePointer();
                    if (currentLine.length() > 4) {
                        int spaceOffset = currentLine.indexOf(' ');
                        int firstQuote = currentLine.indexOf('"');
                        int secondQuote = currentLine.indexOf('"', firstQuote + 1);
                        if (spaceOffset > 1 && secondQuote > firstQuote) {
                            String tag = currentLine.substring(1, spaceOffset);
                            String value = currentLine.substring(firstQuote + 1, secondQuote);
                            String valueEncoded = new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                            if (tag.equals("Event")) {
                                current.setEvent(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("Site")) {
                                current.setSite(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("Round")) {
                                current.setRound(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("White")) {
                                current.setWhite(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("Black")) {
                                current.setBlack(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("Result")) {
                                current.setResult(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("Date")) {
                                current.setDate(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("ECO")) {
                                current.setEco(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("WhiteElo")) {
                                current.setWhiteElo(valueEncoded);
                                current.markValid();
                            } else if (tag.equals("BlackElo")) {
                                current.setBlackElo(valueEncoded);
                                current.markValid();
                            }
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
                    gamesRead++;
                    if (gamesRead > 10000) {
                        if (listener != null && fileSize > 0) {
                            int percent = (int) (game_pos * 100 / fileSize);
                            listener.onProgress(percent);
                        }
                        gamesRead = 0;
                    }
                    if (current.isValid()) {
                        scannedEntries.add(current);
                    }
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

        rebuildEntries(scannedEntries, eventType, forcedAffectedId, knownUnchangedGames);
    }

    private void rebuildEntries(ArrayList<PgnGameInfo> scannedEntries,
                                ChessDatabaseEvent.Type eventType,
                                UUID forcedAffectedId,
                                List<GameInfo> knownUnchangedGames) {
        int forcedIndex = -1;
        if (forcedAffectedId != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).getId().equals(forcedAffectedId)) {
                    forcedIndex = i;
                    break;
                }
            }
        }

        ArrayList<String> scannedFingerprints = new ArrayList<>();
        for (PgnGameInfo entry : scannedEntries) {
            scannedFingerprints.add(fingerprint(entry));
        }

        Map<String, ArrayDeque<UUID>> existingIdsByFingerprint = new HashMap<>();
        for (PgnGameInfo existing : entries) {
            if (existing.getId().equals(forcedAffectedId)) {
                continue;
            }
            existingIdsByFingerprint
                    .computeIfAbsent(fingerprints.get(existing.getId()), ignored -> new ArrayDeque<>())
                    .addLast(existing.getId());
        }

        ArrayList<PgnGameInfo> rebuilt = new ArrayList<>();
        Map<UUID, String> rebuiltFingerprints = new HashMap<>();

        for (int i = 0; i < scannedEntries.size(); i++) {
            PgnGameInfo scanned = scannedEntries.get(i);
            String fp = scannedFingerprints.get(i);
            ArrayDeque<UUID> matchingIds = existingIdsByFingerprint.get(fp);

            UUID id;
            if (i == forcedIndex && forcedAffectedId != null) {
                id = forcedAffectedId;
            } else if (matchingIds != null && !matchingIds.isEmpty()) {
                id = matchingIds.removeFirst();
            } else {
                id = UUID.randomUUID();
            }

            PgnGameInfo rebuiltEntry = new PgnGameInfo(id);
            rebuiltEntry.setOffset(scanned.getOffset());
            rebuiltEntry.setEvent(scanned.getEvent());
            rebuiltEntry.setSite(scanned.getSite());
            rebuiltEntry.setDate(scanned.getDate());
            rebuiltEntry.setRound(scanned.getRound());
            rebuiltEntry.setWhite(scanned.getWhite());
            rebuiltEntry.setBlack(scanned.getBlack());
            rebuiltEntry.setResult(scanned.getResult());
            rebuiltEntry.setEco(scanned.getEco());
            rebuiltEntry.setWhiteElo(scanned.getWhiteElo());
            rebuiltEntry.setBlackElo(scanned.getBlackElo());
            if (scanned.isValid()) {
                rebuiltEntry.markValid();
            }

            rebuilt.add(rebuiltEntry);
            rebuiltFingerprints.put(id, fp);
        }

        List<GameInfo> affectedGames = determineAffectedGames(rebuilt, forcedAffectedId, knownUnchangedGames);
        entries.clear();
        entries.addAll(rebuilt);
        fingerprints.clear();
        fingerprints.putAll(rebuiltFingerprints);
        revision++;

        publish(new ChessDatabaseEvent(eventType, this, revision, affectedGames));
    }

    private List<GameInfo> determineAffectedGames(List<PgnGameInfo> rebuilt,
                                                  UUID forcedAffectedId,
                                                  List<GameInfo> knownUnchangedGames) {
        ArrayList<GameInfo> affected = new ArrayList<>();
        if (forcedAffectedId != null) {
            for (PgnGameInfo g : rebuilt) {
                if (g.getId().equals(forcedAffectedId)) {
                    affected.add(g);
                    return affected;
                }
            }
            for (PgnGameInfo oldG : entries) {
                if (oldG.getId().equals(forcedAffectedId)) {
                    affected.add(oldG);
                    return affected;
                }
            }
            return affected;
        }

        if (knownUnchangedGames != null) {
            Map<UUID, GameInfo> known = new HashMap<>();
            for (GameInfo info : knownUnchangedGames) {
                known.put(info.getId(), info);
            }
            for (PgnGameInfo g : rebuilt) {
                if (!known.containsKey(g.getId())) {
                    affected.add(g);
                }
            }
            return affected;
        }

        Map<UUID, PgnGameInfo> rebuiltMap = new HashMap<>();
        for (PgnGameInfo g : rebuilt) {
            rebuiltMap.put(g.getId(), g);
        }

        for (PgnGameInfo oldG : entries) {
            if (!rebuiltMap.containsKey(oldG.getId())) {
                affected.add(oldG);
            }
        }
        for (PgnGameInfo newG : rebuilt) {
            boolean existed = false;
            for (PgnGameInfo oldG : entries) {
                if (oldG.getId().equals(newG.getId())) {
                    existed = true;
                    break;
                }
            }
            if (!existed) {
                affected.add(newG);
            }
        }
        return affected;
    }

    private String fingerprint(PgnGameInfo entry) {
        try {
            return new PgnPrinter().printGame(loadGame(entry));
        } catch (IOException exception) {
            return headerSignature(entry);
        }
    }

    private String headerSignature(PgnGameInfo entry) {
        return String.join("\u0000",
                entry.getEvent(),
                entry.getSite(),
                entry.getDate(),
                entry.getRound(),
                entry.getWhite(),
                entry.getWhiteElo(),
                entry.getBlack(),
                entry.getBlackElo(),
                entry.getResult()
        );
    }

    private void replaceRange(long startOffset, long endOffset, String replacement)
            throws IOException {
        long fileSize = Files.size(path);
        if (startOffset < 0 || endOffset < startOffset || endOffset > fileSize) {
            throw new IllegalArgumentException("Invalid PGN game range: [" + startOffset + ", " + endOffset + "]");
        }
        Path parent = path.getParent();
        Path temporaryPath = Files.createTempFile(parent == null ? Path.of(".") : parent,
                "jfxchess-pgn-", ".tmp");
        try {
            byte[] content = Files.readAllBytes(path);
            try (var output = Files.newOutputStream(temporaryPath,
                    StandardOpenOption.TRUNCATE_EXISTING)) {
                output.write(content, 0, (int) startOffset);
                output.write(replacement.getBytes(StandardCharsets.UTF_8));
                output.write(content, (int) endOffset, content.length - (int) endOffset);
            }
            moveReplacement(temporaryPath);
        } finally {
            Files.deleteIfExists(temporaryPath);
        }
    }

    private void moveReplacement(Path temporaryPath) throws IOException {
        try {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (AccessDeniedException exception) {
            Files.copy(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private PgnGameInfo findPgnGameInfo(GameInfo info) {
        if (info instanceof PgnGameInfo pgnInfo && entries.contains(pgnInfo)) {
            return pgnInfo;
        }
        for (PgnGameInfo entry : entries) {
            if (entry.getId().equals(info.getId())) {
                return entry;
            }
        }
        return null;
    }

    private void publish(ChessDatabaseEvent event) {
        for (ChessDatabaseListener listener : List.copyOf(listeners)) {
            listener.onDatabaseChanged(event);
        }
    }
}
