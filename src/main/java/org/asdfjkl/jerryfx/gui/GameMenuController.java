package org.asdfjkl.jerryfx.gui;

import javafx.embed.swing.SwingFXUtils;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.asdfjkl.jerryfx.lib.Game;
import org.asdfjkl.jerryfx.lib.OptimizedRandomAccessFile;
import org.asdfjkl.jerryfx.lib.PgnPrinter;
import org.asdfjkl.jerryfx.lib.PgnReader;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class GameMenuController {

    final FileChooser fileChooser = new FileChooser();
    final PgnReader reader = new PgnReader();
    GameModel gameModel;

    public GameMenuController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public void handleBrowseDatabase() {
        DialogDatabase dlg = new DialogDatabase();
        boolean accepted = dlg.show(gameModel, false);
        if(accepted) {
            int gameIndex = dlg.table.getSelectionModel().getSelectedIndex();
            gameModel.currentPgnDatabaseIdx = gameIndex;
            Game g = gameModel.getPgnDatabase().loadGame(gameIndex);
            gameModel.setGame(g);
            g.setTreeWasChanged(true);
            gameModel.triggerStateChange();
        }
    }

    public void handleOpenGame() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            // for files >= 20 kb, always open in db window
            if((file.length() / 1024) < 20) {
                ArrayList<Long> indices = reader.scanPgn(file.getAbsolutePath());
                if(indices.size() == 0) {
                    return;
                }
                if(indices.size() == 1) {
                    Game g = null;
                    if(reader.isIsoLatin1(file.getAbsolutePath())) {
                        reader.setEncodingIsoLatin1();
                    } else {
                        reader.setEncodingUTF8();
                    }
                    OptimizedRandomAccessFile raf = null;
                    try {
                        raf = new OptimizedRandomAccessFile(file.getAbsolutePath(), "r");
                        g = reader.readGame(raf);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if(raf!=null) {
                            try {
                                raf.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(g != null && (g.getRootNode().hasChild() || !g.getRootNode().getBoard().isInitialPosition())) {
                        g.setTreeWasChanged(true);
                        gameModel.setGame(g);
                        gameModel.triggerStateChange();
                        return;
                    }
                } else {  // indices > 1, show db dialog
                    DialogDatabase dlg = new DialogDatabase();
                    gameModel.getPgnDatabase().filename = file.getAbsolutePath();
                    //gameModel.getPgnDatabase().open(file.getAbsolutePath());
                    boolean accepted = dlg.show(gameModel, true);
                    if(accepted) {
                        int gameIndex = dlg.table.getSelectionModel().getSelectedIndex();
                        gameModel.currentPgnDatabaseIdx = gameIndex;
                        Game g = gameModel.getPgnDatabase().loadGame(gameIndex);
                        gameModel.setGame(g);
                        g.setTreeWasChanged(true);
                        gameModel.triggerStateChange();
                    }
                }
            } else { // for larger files, we always assume there is more than one game
                // then show database dialog
                DialogDatabase dlg = new DialogDatabase();
                gameModel.getPgnDatabase().filename = file.getAbsolutePath();
                //gameModel.getPgnDatabase().open(file.getAbsolutePath());
                boolean accepted = dlg.show(gameModel, true);
                if(accepted) {
                    int gameIndex = dlg.table.getSelectionModel().getSelectedIndex();
                    gameModel.currentPgnDatabaseIdx = gameIndex;
                    Game g = gameModel.getPgnDatabase().loadGame(gameIndex);
                    gameModel.setGame(g);
                    g.setTreeWasChanged(true);
                    gameModel.triggerStateChange();
                }
            }


            /*
            System.out.println(file.getAbsolutePath());
            gameModel.getPgnDatabase().filename = file.getAbsolutePath();
            gameModel.getPgnDatabase().open();
            System.out.println("games in db: "+gameModel.getPgnDatabase().getNrGames());
            if(gameModel.getPgnDatabase().getNrGames() == 1) {
                gameModel.currentPgnDatabaseIdx = 0;
                System.out.println("loading game 0");
                Game g = gameModel.getPgnDatabase().loadGame(0);
                gameModel.setGame(g);
                g.setTreeWasChanged(true);
                gameModel.triggerStateChange();
            } else {
                System.out.println("no games in database");
                if(gameModel.getPgnDatabase().getNrGames() == 0) {
                    gameModel.getPgnDatabase().filename = "";
                } else {
                    System.out.println("too many games, needs browsing");
                    handleBrowseDatabase();
                }
            }*/
        }
        /*
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            PgnReader reader = new PgnReader();
            try {
                OptimizedRandomAccessFile raf = new OptimizedRandomAccessFile(file, "r");
                raf.seek(0);
                Game g = reader.readGame(raf);
                g.setTreeWasChanged(true);
                gameModel.setGame(g);
                gameModel.triggerStateChange();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }*/
    }

    public void handleSaveCurrentGame() {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            PgnPrinter printer = new PgnPrinter();
            printer.writeGame(gameModel.getGame(), file.getAbsolutePath());
        }
    }

    public void handleSaveBoardPicture(Chessboard chessboard) {

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        WritableImage image = chessboard.snapshot(new SnapshotParameters(), null);
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (IOException e) {
                System.out.println(e.getStackTrace());
            }
        }
    }

    public void handlePrintGame(Stage owner) {

        PrinterJob job = PrinterJob.createPrinterJob();
        boolean ok = job.showPrintDialog(owner);

        if(ok) {
            PgnPrinter pgnPrinter = new PgnPrinter();
            String pgnGame = pgnPrinter.printGame(gameModel.getGame());
            TextFlow printArea = new TextFlow(new Text(pgnGame));
            boolean printed = job.printPage(printArea);
            if(printed) {
                job.endJob();
            }
        }
    }

    public void handleNextGame() {
        int nextIdx = gameModel.currentPgnDatabaseIdx + 1;
        if(nextIdx < gameModel.getPgnDatabase().getNrGames()) {
            Game g = gameModel.getPgnDatabase().loadGame(nextIdx);
            g.setTreeWasChanged(true);
            gameModel.setGame(g);
            gameModel.currentPgnDatabaseIdx = nextIdx;
            gameModel.triggerStateChange();
        }
    }

    public void handlePrevGame() {
        int nextIdx = gameModel.currentPgnDatabaseIdx - 1;
        if(nextIdx >= 0) {
            Game g = gameModel.getPgnDatabase().loadGame(nextIdx);
            g.setTreeWasChanged(true);
            gameModel.setGame(g);
            gameModel.currentPgnDatabaseIdx = nextIdx;
            gameModel.triggerStateChange();
        }
    }

    public void handlePrintPosition(Stage owner) {

        PrinterJob job = PrinterJob.createPrinterJob();
        boolean ok = job.showPrintDialog(owner);

        if(ok) {
            String fenCurrentPos = gameModel.getGame().getCurrentNode().getBoard().fen();
            TextFlow printArea = new TextFlow(new Text(fenCurrentPos));
            boolean printed = job.printPage(printArea);
            if(printed) {
                job.endJob();
            }
        }
    }

}
