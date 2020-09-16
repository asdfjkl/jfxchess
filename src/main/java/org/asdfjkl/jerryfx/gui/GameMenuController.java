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

public class GameMenuController {

    final FileChooser fileChooser = new FileChooser();

    GameModel gameModel;

    public GameMenuController(GameModel gameModel) {
        this.gameModel = gameModel;
    }

    public void handleOpenGame() {

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
        }
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
