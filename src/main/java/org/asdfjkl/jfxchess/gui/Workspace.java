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
import org.asdfjkl.jfxchess.lib.PgnPrinter;
import org.asdfjkl.jfxchess.lib.PgnReader;

import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Workspace {

    private final PropertyChangeSupport propertyChangeSupport =
            new PropertyChangeSupport(this);
    private final ArrayList<GameSession> sessions = new ArrayList<>();
    private final Map<Path, PgnDocument> documents = new HashMap<>();

    private GameSession activeSession;

    public GameSession createSession(Game game) {
        GameSession session = new GameSession(copyGame(game));
        sessions.add(session);
        propertyChangeSupport.firePropertyChange("sessionAdded", null, session);
        setActiveSession(session);
        return session;
    }

    public void closeSession(GameSession session) {
        Objects.requireNonNull(session, "session");
        if (!sessions.remove(session)) {
            throw new IllegalArgumentException("session is not in this workspace");
        }
        session.close();
        propertyChangeSupport.firePropertyChange("sessionRemoved", session, null);
        if (session == activeSession) {
            GameSession replacement = sessions.isEmpty()
                    ? null
                    : sessions.get(sessions.size() - 1);
            setActiveSession(replacement);
        }
    }

    public List<GameSession> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public GameSession getActiveSession() {
        return activeSession;
    }

    public void setActiveSession(GameSession session) {
        if (session != null && !sessions.contains(session)) {
            throw new IllegalArgumentException("session is not in this workspace");
        }
        GameSession oldSession = activeSession;
        activeSession = session;
        propertyChangeSupport.firePropertyChange(
                "activeSessionChanged",
                oldSession,
                activeSession
        );
    }

    public GameSession getSession(UUID sessionId) {
        for (GameSession session : sessions) {
            if (session.getId().equals(sessionId)) {
                return session;
            }
        }
        return null;
    }

    public PgnDocument getOrCreateDocument(Path path) {
        Path canonicalPath = Objects.requireNonNull(path, "path")
                .toAbsolutePath()
                .normalize();
        PgnDocument document = documents.get(canonicalPath);
        if (document == null) {
            document = new PgnDocument(canonicalPath, new PgnReader());
            document.addListener(this::onDocumentChanged);
            documents.put(canonicalPath, document);
        }
        return document;
    }

    public List<PgnDocument> getDocuments() {
        return List.copyOf(documents.values());
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    private Game copyGame(Game game) {
        Objects.requireNonNull(game, "game");
        PgnPrinter printer = new PgnPrinter();
        PgnReader reader = new PgnReader();
        return reader.readGame(printer.printGame(game));
    }

    private void onDocumentChanged(PgnDocumentEvent event) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> onDocumentChanged(event));
            return;
        }
        for (GameSession session : sessions) {
            PgnSourceReference source = session.getPgnSourceReference();
            if (source == null ||
                    !source.getDocumentPath().equals(event.getDocument().getPath()) ||
                    !event.getAffectedGameIds().contains(source.getGameId())) {
                continue;
            }
            if (session.isDirty()) {
                session.markStale();
                continue;
            }
            try {
                session.setGame(event.getDocument().loadGame(source.getGameId()));
                session.setPgnSourceReference(new PgnSourceReference(
                        event.getDocument().getPath(),
                        source.getGameId(),
                        event.getRevision()
                ));
            } catch (IOException | IllegalArgumentException exception) {
                session.markStale();
            }
        }
    }
}
