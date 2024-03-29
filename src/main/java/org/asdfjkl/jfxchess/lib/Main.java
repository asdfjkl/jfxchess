/* JerryFX - A Chess Graphical User Interface
 * Copyright (C) 2020 Dominik Klein
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

import java.io.IOException;
import java.util.ArrayList;


public class Main {

    public static void main(String[] args) {

        if (args.length > 0) {
            String filename = args[0];
            if (filename.equals("run-all-tests")) {
                TestCases cases = new TestCases();
                cases.fenTest();
                cases.pgnReadGameTest();
                cases.runPerfT();
                cases.pgnReadMiddleGTest();
                cases.pgnScanTest();
                cases.runBitSetTest();
                cases.runPgnPrintTest();
                cases.runSanTest();
                cases.runZobristTest();
                cases.pgnReadSingleEntryTestOpenClose();
                cases.pgnReadSingleEntryTestSeekWithinRAF();
                cases.pgnReadAllMillBaseTest();



            } else {

                boolean printOutput = true;
                if (args.length > 1) {
                    if(args[1].equals("noout")) {
                        printOutput = false;
                    }
                }

                PgnReader reader = new PgnReader();
                reader.setEncodingIsoLatin1();
                PgnPrinter printer = new PgnPrinter();
                ArrayList<Long> offsets = reader.scanPgn(filename);

                for (int i = 0; i < offsets.size(); i++) {
                    OptimizedRandomAccessFile raf = null;
                    try {
                        raf = new OptimizedRandomAccessFile(filename, "r");
                        raf.seek(offsets.get(i));
                        Game g = reader.readGame(raf);
                        if(printOutput) {
                            System.out.println(printer.printGame(g));
                            System.out.println("\n");
                        }
                        raf.close();
                    } catch (IOException e) {
                        System.out.println("error reading: " + filename);
                        System.out.println("game nr......: " + i);
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
                }
            } }
        else {
            System.out.println("java -jar program.jar pgnfile OR java -jar program.jar run-all-tests");
        }

    }
}
