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

import java.nio.file.Path;
import java.util.Objects;

public final class PgnSourceReference {

    private final Path documentPath;
    private final PgnGameId gameId;
    private final long documentRevision;

    public PgnSourceReference(Path documentPath,
                              PgnGameId gameId,
                              long documentRevision) {
        this.documentPath = Objects.requireNonNull(documentPath, "documentPath");
        this.gameId = Objects.requireNonNull(gameId, "gameId");
        this.documentRevision = documentRevision;
    }

    public PgnSourceReference(Path documentPath,
                              java.util.UUID gameId,
                              long documentRevision) {
        this.documentPath = Objects.requireNonNull(documentPath, "documentPath");
        this.gameId = new PgnGameId(Objects.requireNonNull(gameId, "gameId"));
        this.documentRevision = documentRevision;
    }

    public Path getDocumentPath() {
        return documentPath;
    }

    public PgnGameId getGameId() {
        return gameId;
    }

    public long getDocumentRevision() {
        return documentRevision;
    }
}
