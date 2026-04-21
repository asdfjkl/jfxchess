package org.asdfjkl.jfxchess.lib;

public interface ProgressListener {
    void onProgress(int percent);
    boolean isCancelled();
}
