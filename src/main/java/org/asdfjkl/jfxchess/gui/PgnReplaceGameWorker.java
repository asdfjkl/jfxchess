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

package org.asdfjkl.jfxchess.gui;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class PgnReplaceGameWorker extends SwingWorker<String, Integer> {

    private final String filePath;
    private final String text;
    private final long offset1;
    private final long offset2;
    private final PgnReplaceListener listener;

    public PgnReplaceGameWorker(String filePath, String text, long offset1, long offset2,
                             PgnReplaceListener listener) {
        this.filePath = filePath;
        this.text = text;
        this.offset1 = offset1;
        this.offset2 = offset2;
        this.listener = listener;
    }

    @Override
    protected String doInBackground() {
        Path originalPath = Paths.get(filePath);

        try {
            if (!Files.exists(originalPath)) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            long fileSize = Files.size(originalPath);

            if (offset1 < 0 || offset2 < 0 || offset1 > offset2 || offset2 > fileSize) {
                throw new IllegalArgumentException(
                        "Invalid offsets: offset1=" + offset1 + ", offset2=" + offset2
                );
            }

            Path tempPath = Files.createTempFile(originalPath.getParent(), "replace_tmp_", ".tmp");

            try (
                    InputStream in = Files.newInputStream(originalPath);
                    OutputStream out = Files.newOutputStream(tempPath, StandardOpenOption.WRITE)
            ) {
                byte[] buffer = new byte[8192];
                long totalProcessed = 0;

                // Copy [0, offset1)
                while (totalProcessed < offset1) {
                    if (isCancelled()) return "CANCELLED";

                    int toRead = (int) Math.min(buffer.length, offset1 - totalProcessed);
                    int read = in.read(buffer, 0, toRead);
                    if (read == -1) break;

                    out.write(buffer, 0, read);
                    totalProcessed += read;
                    updateProgress(totalProcessed, fileSize);
                }

                // Skip
                long bytesToSkip = offset2 - offset1;
                while (bytesToSkip > 0) {
                    if (isCancelled()) return "CANCELLED";

                    long skipped = in.skip(bytesToSkip);
                    if (skipped <= 0) {
                        int read = in.read(buffer, 0, (int)Math.min(buffer.length, bytesToSkip));
                        if (read == -1) break;
                        skipped = read;
                    }
                    bytesToSkip -= skipped;
                    totalProcessed += skipped;
                    updateProgress(totalProcessed, fileSize);
                }

                // Insert
                String insertion = "\n" + text + "\n\n";
                out.write(insertion.getBytes(StandardCharsets.UTF_8));

                // Copy rest
                int read;
                while ((read = in.read(buffer)) != -1) {
                    if (isCancelled()) return "CANCELLED";

                    out.write(buffer, 0, read);
                    totalProcessed += read;
                    updateProgress(totalProcessed, fileSize);
                }
            }

            Files.move(
                    tempPath,
                    originalPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

            return "SUCCESS";

        } catch (Exception e) {
            return stackTraceToString(e);
        }
    }

    private void updateProgress(long processed, long total) {
        int progress = (int) ((processed * 100) / total);
        setProgress(progress);
    }

    @Override
    protected void done() {
        try {
            String result = get(); // safe: no exception escapes
            if (listener != null) {
                listener.onReplaceFinished(result);
            }
        } catch (Exception e) {
            // This should rarely happen now, but just in case:
            if (listener != null) {
                listener.onReplaceFinished(stackTraceToString(e));
            }
        }
    }

    private String stackTraceToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

}