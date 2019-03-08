/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
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


#ifndef MAIN_WINDOW_H
#define MAIN_WINDOW_H

#include <QMainWindow>
#include <QMenuBar>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QLabel>
#include <QTextEdit>
#include <QPushButton>
#include <QSplitter>
#include <QSpinBox>
#include "viewController/on_off_button.h"
#include <QSignalMapper>
#include "viewController/boardviewcontroller.h"
#include "viewController/moveviewcontroller.h"
#include "model/game_model.h"
#include "viewController/engineview.h"
#include "uci/uci_controller.h"
#include "controller/mode_controller.h"
#include "controller/edit_controller.h"
#include "controller/file_controller.h"
#include <QShortcut>
#include <QComboBox>
#include "ribbon/ribbon_widget.h"

class MainWindow : public QMainWindow
{
    Q_OBJECT
public:
    explicit MainWindow(QWidget *parent = 0);
    void centerAndResize();

private:
    // QMenuBar *menu;
    // QToolBar *toolbar;
    GameModel *gameModel;
    QWidget *mainWidget;
    QHBoxLayout *hbox;
    QVBoxLayout *vbox;
    QLabel *name;
    BoardViewController *boardViewController;
    MoveViewController *moveViewController;
    EngineView *engineViewController;
    UciController *uciController;
    ModeController *modeController;
    EditController *editController;
    FileController *fileController;
    void setupStandardLayout();
    void setupVerticalLayout();
    QAction *flip_board;
    QSignalMapper*  signalMapper;
    QAction *save_game;
    QAction *show_info;
    //QPushButton *pbEngineOnOff;
    OnOffButton *pbEngineOnOff;
    //QAction *analysis_mode;
    //QAction *enter_moves;
    //QAction *play_black;
    //QAction *play_white;
    QPixmap* fromSvgToPixmap(const QSize &ImageSize, const QString &SvgFile);
    QSplitter* splitterTopDown;
    QSplitter* splitterLeftRight;
    QLabel *lblMultiPv;
    QSpinBox *spinMultiPv;

    RibbonWidget *ribbon;

    QAction* createAction(QString name, const QString &displayName, QSize &iconSize);


protected:
    void wheelEvent(QWheelEvent *event);

signals:

public slots:
    void showAbout();
    void goToHomepage();
    void saveImage();
    void onStateChange();
    void onEngineToggle();
    void aboutToQuit();
    void resetLayout();

};

#endif // MAIN_WINDOW_H
