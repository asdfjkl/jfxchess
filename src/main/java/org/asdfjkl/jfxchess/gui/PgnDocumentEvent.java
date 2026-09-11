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

import java.util.List;
import java.util.Objects;

public final class PgnDocumentEvent {

    public enum Type {
        DOCUMENT_RELOADED,
        GAME_REPLACED,
        GAME_DELETED,
        GAMES_INSERTED
    }

    private final Type type;
    private final PgnDocument document;
    private final long revision;
    private final List<PgnGameId> affectedGameIds;

    public PgnDocumentEvent(Type type,
                            PgnDocument document,
                            long revision,
                            List<PgnGameId> affectedGameIds) {
        this.type = Objects.requireNonNull(type, "type");
        this.document = Objects.requireNonNull(document, "document");
        this.revision = revision;
        this.affectedGameIds = List.copyOf(affectedGameIds);
    }

    public Type getType() {
        return type;
    }

    public PgnDocument getDocument() {
        return document;
    }

    public long getRevision() {
        return revision;
    }

    public List<PgnGameId> getAffectedGameIds() {
        return affectedGameIds;
    }
}
