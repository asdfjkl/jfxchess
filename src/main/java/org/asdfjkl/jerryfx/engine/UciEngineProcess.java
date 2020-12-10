package org.asdfjkl.jerryfx.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class UciEngineProcess implements AutoCloseable {

    public static final int MAX_LINES = 100;
    private final Process _process;
    private final BufferedWriter _bro;
    private final BufferedReader _bri;

    public UciEngineProcess(final File path)
            throws IOException {
        _process = Runtime.getRuntime().exec(path.getAbsolutePath());
        _bri = new BufferedReader(new InputStreamReader(_process.getInputStream()));
        _bro = new BufferedWriter(new OutputStreamWriter(_process.getOutputStream()));
    }

    public void send(final String cmd)
            throws IOException {
        _bro.write(cmd + "\n");
        _bro.flush();
    }

    /**
     * Blocking command that expects a reply.
     *
     * @param cmd UCI command to send.
     *
     * @return A list of the lines the engine sent back.
     *
     * @throws IOException
     */
    // TODO: Need to check end of stream handling.
    public List<String> sendSynchronous(final String cmd)
            throws IOException {
        send(cmd);
        List<String> res = new ArrayList<>();
        String line;
        do {
            line = _bri.readLine();
            if (line == null) {
                return res;
            }
            if (!line.isEmpty()) {
                res.add(line);
            }
        } while (!line.endsWith("ok") && res.size() < MAX_LINES);
        return res;
    }

    public List<String> receive()
            throws IOException {
        List<String> res = new ArrayList<>();
        while (_bri.ready() && res.size() < MAX_LINES) {
            String line = _bri.readLine();
            if (!line.isEmpty()) {
                res.add(line);
            }
        }
        return res;
    }

    @Override
    public void close() {
        closeStream(_bro);
        closeStream(_bri);
        _process.destroy();
    }

    private void closeStream(final Closeable stream) {
        try {
            stream.close();
        }
        catch (IOException e) {
            System.out.println("Error closing stream: " + stream);
        }
    }

}
