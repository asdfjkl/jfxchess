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

package org.asdfjkl.jfxchess.gui;

import org.asdfjkl.jfxchess.lib.Game;
import org.asdfjkl.jfxchess.lib.OptimizedRandomAccessFile;
import org.asdfjkl.jfxchess.lib.PgnGameInfo;
import org.asdfjkl.jfxchess.lib.PgnPrinter;
import org.asdfjkl.jfxchess.lib.PgnReader;
import org.asdfjkl.jfxchess.lib.ProgressListener;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.AccessDeniedException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PgnDocument {

    private final Path path;
    private final PgnReader reader;
    private final Map<PgnGameId, PgnGameInfo> entries = new LinkedHashMap<>();
    private final Map<PgnGameId, String> fingerprints = new HashMap<>();
    private final ArrayList<PgnDocumentListener> listeners = new ArrayList<>();
    private long revision;

    public PgnDocument(Path path, PgnReader reader) {
        this.path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    public Path getPath() {
        return path;
    }

    public long getRevision() {
        return revision;
    }

    public List<PgnGameId> getGameIds() {
        return List.copyOf(entries.keySet());
    }

    public ArrayList<PgnGameInfo> getEntries() {
        return new ArrayList<>(entries.values());
    }

    public PgnGameId getGameIdAt(int index) {
        if (index < 0 || index >= entries.size()) {
            throw new IndexOutOfBoundsException("game index is outside this document");
        }
        return getGameIds().get(index);
    }

    public PgnGameId getGameId(PgnGameInfo gameInfo) {
        Objects.requireNonNull(gameInfo, "gameInfo");
        for (Map.Entry<PgnGameId, PgnGameInfo> entry : entries.entrySet()) {
            if (entry.getValue() == gameInfo || entry.getValue().equals(gameInfo)) {
                return entry.getKey();
            }
        }
        throw new IllegalArgumentException("game is not in this document");
    }

    public int indexOf(PgnGameId gameId) {
        return getGameIds().indexOf(gameId);
    }

    public void reload() {
        reload(null);
    }

    public void reload(ProgressListener progressListener) {
        rebuild(PgnDocumentEvent.Type.DOCUMENT_RELOADED, null, null, progressListener);
    }

    public Game loadGame(PgnGameId gameId) throws IOException {
        PgnGameInfo entry = entries.get(gameId);
        if (entry == null) {
            throw new IllegalArgumentException("game is not in this document");
        }
        return loadGame(entry);
    }

    public PgnGameId appendGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        String gamePgn = new PgnPrinter().printGame(game);
        Files.writeString(path, "\n\n" + gamePgn + "\n\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        List<PgnGameId> previousIds = getGameIds();
        rebuild(PgnDocumentEvent.Type.GAMES_INSERTED, null, previousIds, null);
        if (entries.size() <= previousIds.size()) {
            throw new IOException("appended game could not be found while rescanning " + path);
        }
        return getGameIdAt(entries.size() - 1);
    }

    public PgnGameId writeSingleGame(Game game) throws IOException {
        Objects.requireNonNull(game, "game");
        Files.writeString(path, new PgnPrinter().printGame(game) + "\n",
                StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        rebuild(PgnDocumentEvent.Type.DOCUMENT_RELOADED, null, null, null);
        if (entries.size() != 1) {
            throw new IOException("saved game could not be found while rescanning " + path);
        }
        return getGameIdAt(0);
    }

    public void replaceGame(PgnGameId gameId, String gamePgn) throws IOException {
        Objects.requireNonNull(gamePgn, "gamePgn");
        int index = indexOfRequired(gameId);
        PgnGameInfo currentEntry = entries.get(gameId);
        long startOffset = currentEntry.getOffset();
        long endOffset = index + 1 < entries.size()
                ? getEntries().get(index + 1).getOffset()
                : Files.size(path);
        replaceRange(startOffset, endOffset, "\n" + gamePgn + "\n\n");
        rebuild(PgnDocumentEvent.Type.GAME_REPLACED, gameId, null, null);
    }

    public void deleteGame(PgnGameId gameId) throws IOException {
        int index = indexOfRequired(gameId);
        PgnGameInfo currentEntry = entries.get(gameId);
        long startOffset = currentEntry.getOffset();
        long endOffset = index + 1 < entries.size()
                ? getEntries().get(index + 1).getOffset()
                : Files.size(path);
        replaceRange(startOffset, endOffset, "");
        rebuild(PgnDocumentEvent.Type.GAME_DELETED, gameId, null, null);
    }

    public void addListener(PgnDocumentListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(PgnDocumentListener listener) {
        listeners.remove(listener);
    }

    private Game loadGame(PgnGameInfo entry) throws IOException {
        OptimizedRandomAccessFile file =
                new OptimizedRandomAccessFile(path.toString(), "r");
        try {
            file.seek(entry.getOffset());
            return new PgnReader().readGame(file);
        } finally {
            file.close();
        }
    }

    private int indexOfRequired(PgnGameId gameId) {
        int index = indexOf(gameId);
        if (index < 0) {
            throw new IllegalArgumentException("game is not in this document");
        }
        return index;
    }

    private void replaceRange(long startOffset, long endOffset, String replacement)
            throws IOException {
        long fileSize = Files.size(path);
        if (startOffset < 0 || endOffset < startOffset || endOffset > fileSize) {
            throw new IllegalArgumentException("invalid PGN game range");
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

    private void rebuild(PgnDocumentEvent.Type type,
                         PgnGameId forcedAffectedId,
                         List<PgnGameId> knownUnchangedIds,
                         ProgressListener progressListener) {
        ArrayList<PgnGameInfo> scannedEntries = reader.scanPgn(path.toString(), progressListener);
        int forcedIndex = type == PgnDocumentEvent.Type.GAME_REPLACED
                ? indexOf(forcedAffectedId)
                : -1;
        ArrayList<String> scannedFingerprints = new ArrayList<>();
        for (PgnGameInfo entry : scannedEntries) {
            scannedFingerprints.add(fingerprint(entry));
        }

        Map<String, ArrayDeque<PgnGameId>> existingIdsByFingerprint = new HashMap<>();
        for (PgnGameId existingId : entries.keySet()) {
            if (existingId.equals(forcedAffectedId)) {
                continue;
            }
            existingIdsByFingerprint
                    .computeIfAbsent(fingerprints.get(existingId), ignored -> new ArrayDeque<>())
                    .addLast(existingId);
        }

        Map<PgnGameId, PgnGameInfo> rebuiltEntries = new LinkedHashMap<>();
        Map<PgnGameId, String> rebuiltFingerprints = new HashMap<>();
        for (int index = 0; index < scannedEntries.size(); index++) {
            String fingerprint = scannedFingerprints.get(index);
            ArrayDeque<PgnGameId> matchingIds =
                    existingIdsByFingerprint.get(fingerprint);
            PgnGameId id;
            if (index == forcedIndex) {
                id = forcedAffectedId;
            } else {
                id = matchingIds == null || matchingIds.isEmpty()
                        ? PgnGameId.create()
                        : matchingIds.removeFirst();
            }
            rebuiltEntries.put(id, scannedEntries.get(index));
            rebuiltFingerprints.put(id, fingerprint);
        }

        List<PgnGameId> affectedIds = determineAffectedIds(rebuiltEntries,
                forcedAffectedId, knownUnchangedIds);
        entries.clear();
        entries.putAll(rebuiltEntries);
        fingerprints.clear();
        fingerprints.putAll(rebuiltFingerprints);
        revision++;
        publish(new PgnDocumentEvent(type, this, revision, affectedIds));
    }

    private List<PgnGameId> determineAffectedIds(Map<PgnGameId, PgnGameInfo> rebuiltEntries,
                                                  PgnGameId forcedAffectedId,
                                                  List<PgnGameId> knownUnchangedIds) {
        ArrayList<PgnGameId> affectedIds = new ArrayList<>();
        if (forcedAffectedId != null) {
            affectedIds.add(forcedAffectedId);
            return affectedIds;
        }
        if (knownUnchangedIds != null) {
            for (PgnGameId id : rebuiltEntries.keySet()) {
                if (!knownUnchangedIds.contains(id)) {
                    affectedIds.add(id);
                }
            }
            return affectedIds;
        }
        for (PgnGameId id : entries.keySet()) {
            if (!rebuiltEntries.containsKey(id)) {
                affectedIds.add(id);
            }
        }
        for (PgnGameId id : rebuiltEntries.keySet()) {
            if (!entries.containsKey(id)) {
                affectedIds.add(id);
            }
        }
        return affectedIds;
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

    private void publish(PgnDocumentEvent event) {
        for (PgnDocumentListener listener : List.copyOf(listeners)) {
            listener.onDocumentChanged(event);
        }
    }
}
