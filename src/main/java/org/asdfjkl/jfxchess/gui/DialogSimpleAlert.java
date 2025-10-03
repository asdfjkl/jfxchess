/* JFXChess - A Chess Graphical User Interface
 * Copyright (C) 2020-2025 Dominik Klein
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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


public class DialogSimpleAlert {

    private final Stage dialog;
    private final Label messageLabel;
    private final ImageView iconView;

    public DialogSimpleAlert(Stage owner, Alert.AlertType alertType, String title, String message) {
        dialog = new Stage(StageStyle.UTILITY);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Icon
        if(alertType == Alert.AlertType.WARNING) {
            iconView = new ImageView( new Image("icons/dialog-warning.png"));
        } else { // default to info
            iconView = new ImageView( new Image("icons/dialog-information.png"));
        }
        iconView.setFitWidth(48);
        iconView.setFitHeight(48);
        iconView.setPreserveRatio(true);

        // Message
        messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300); // Constrain so wrapping happens
        messageLabel.setPrefWidth(300);

        HBox contentBox = new HBox(10, iconView, messageLabel);
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setPadding(new Insets(10));

        // Buttons
        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setOnAction(e -> dialog.close());

        HBox buttonBox = new HBox(10, okButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(10, 10, 10, 10));

        // Layout
        BorderPane root = new BorderPane();
        root.setCenter(contentBox);
        root.setBottom(buttonBox);

        Scene scene = new Scene(root, 350, 150);
        dialog.setScene(scene);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }

    public void setIcon(Image icon) {
        iconView.setImage(icon);
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

}