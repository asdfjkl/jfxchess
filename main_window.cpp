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

#include "main_window.h"
#include <QApplication>
#include <QDesktopWidget>
#include <QDebug>
#include <QPainter>
#include <QStyle>
//#include <QPrintDialog>
#include <QTextEdit>
#include <QFileDialog>
#include <QHBoxLayout>
#include <QLabel>
#include <QStatusBar>
#include <QMenu>
#include <QPrinter>
#include "viewController/boardviewcontroller.h"
#include "viewController/moveviewcontroller.h"
#include "controller/edit_controller.h"
#include "controller/mode_controller.h"
#include "uci/uci_controller.h"
#include "model/game_model.h"
#include "chess/game_node.h"
#include "uci/uci_controller.h"
#include <QPushButton>
#include "viewController/on_off_button.h"
#include <QComboBox>
#include <QCheckBox>
#include <QShortcut>
#include <QToolBar>
#include <QSplitter>
#include <QDesktopServices>
#include "chess/pgn_reader.h"
#include "viewController/engineview.h"
#include "dialogs/dialog_about.h"
#include "various/resource_finder.h"
#include "various/messagebox.h"
#include "ribbon/ribbon_button.h"

#include "funct.h"

#include "chess/ecocode.h"

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent)
{

    // set working dir to executable work directory
    QDir::setCurrent(QCoreApplication::applicationDirPath());

    QString resDir = ResourceFinder::getPath();

    chess::FuncT *f = new chess::FuncT();
    //f->run_pgn_speedtest();
    //f->run_polyglot();
    /*
    f->run_pgnt();
    f->run_pgn_scant();
    */

    // reconstruct gameModel
    this->gameModel = new GameModel();
    this->gameModel->restoreGameState();
    this->gameModel->getGame()->setTreeWasChanged(true);

    this->boardViewController = new BoardViewController(gameModel, this);
    this->moveViewController = new MoveViewController(gameModel, this);
    this->moveViewController->setFocus();
    this->engineViewController = new EngineView(gameModel, this);
    engineViewController->setFocusPolicy(Qt::NoFocus);
    moveViewController->setFocusPolicy(Qt::ClickFocus);

    this->name = new QLabel();
    name->setText("<b>Robert James Fisher - Reuben Fine</b><br/>New York(USA) 1963.03.??");
    name->setAlignment(Qt::AlignCenter);
    name->setBuddy(moveViewController);

    QHBoxLayout *hbox_name_editHeader = new QHBoxLayout();
    QPushButton *editHeader = new QPushButton();
    QPixmap pxEditHeader(*this->fromSvgToPixmap(editHeader->iconSize(),resDir + "/res/icons/document-properties.svg"));
    editHeader->setIcon(QIcon(pxEditHeader));

    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(this->name);
    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(editHeader);

    this->uciController = new UciController();

    this->modeController = new ModeController(gameModel, uciController, this);
    this->editController = new EditController(gameModel, this);
    this->fileController = new FileController(gameModel, this);

    QSize btnSize = QSize(this->height()/19, this->height()/19);
    QSize btnSizeLR = QSize(this->height()/14, this->height()/14);
    QPushButton *left = new QPushButton();
    QPushButton *right = new QPushButton();
    QPushButton *beginning = new QPushButton();
    QPushButton *end = new QPushButton();

    left->setIconSize(btnSizeLR);
    right->setIconSize(btnSizeLR);
    beginning->setIconSize(btnSize);
    end->setIconSize(btnSize);

    QPixmap pxRight(*this->fromSvgToPixmap(right->iconSize(),resDir + "/res/icons/go-next.svg"));
    QPixmap pxLeft(*this->fromSvgToPixmap(left->iconSize(),resDir + "/res/icons/go-previous.svg"));
    QPixmap pxBeginning(*this->fromSvgToPixmap(left->iconSize(),resDir + "/res/icons/go-first.svg"));
    QPixmap pxEnd(*this->fromSvgToPixmap(left->iconSize(),resDir + "/res/icons/go-last.svg"));

    right->setIcon(QIcon(pxRight));
    left->setIcon(QIcon(pxLeft));
    beginning->setIcon(QIcon(pxBeginning));
    end->setIcon(QIcon(pxEnd));

    QWidget *mainWidget = new QWidget();

    // setup the main window
    // consisting of:
    //
    // <-------menubar---------------------------->
    // <chess-     ->   <label w/ game data      ->
    // <board      ->   <moves_edit_view---------->
    // <view       ->   <engine output view      ->


    QSizePolicy *spLeft = new QSizePolicy();
    spLeft->setHorizontalStretch(1);

    QSizePolicy *spRight = new QSizePolicy();
    spRight->setHorizontalStretch(2);

    QHBoxLayout *hbox_buttons = new QHBoxLayout();
    hbox_buttons->addStretch(1);
    hbox_buttons->addWidget(beginning);
    hbox_buttons->addWidget(left);
    hbox_buttons->addWidget(right);
    hbox_buttons->addWidget(end);
    hbox_buttons->addStretch(1);

    QHBoxLayout *hbox_right_engine_buttons = new QHBoxLayout();

    this->pbEngineOnOff = new OnOffButton(this); //new QPushButton("OFF");
    this->lblMultiPv = new QLabel(this->tr("Lines:"), this);
    this->spinMultiPv = new QSpinBox(this);
    this->spinMultiPv->setRange(1,4);
    this->spinMultiPv->setValue(1);
    this->lblMultiPv->setBuddy(this->spinMultiPv);

    QPushButton *editEngines = new QPushButton();
    QPixmap pxEditEngines(*this->fromSvgToPixmap(editEngines->iconSize(),resDir + "/res/icons/document-properties.svg"));
    editEngines->setIcon(QIcon(pxEditEngines));

    hbox_right_engine_buttons->addWidget(pbEngineOnOff);
    hbox_right_engine_buttons->addWidget(this->lblMultiPv);
    hbox_right_engine_buttons->addWidget(this->spinMultiPv);
    hbox_right_engine_buttons->addStretch(1);
    hbox_right_engine_buttons->addWidget(editEngines);

    QVBoxLayout *vbox_right = new QVBoxLayout();
    vbox_right->addLayout(hbox_name_editHeader);
    vbox_right->addWidget(moveViewController);
    vbox_right->addLayout(hbox_buttons);
    //vbox_right->addLayout(hbox_right_engine_buttons);
    //vbox_right->addWidget(engineViewController);

    vbox_right->setStretch(0,1);
    vbox_right->setStretch(1,4);
    vbox_right->setStretch(2,1);
    //vbox_right->setStretch(3,1);
    //vbox_right->setStretch(4,4);

    QVBoxLayout *vbox_left = new QVBoxLayout();
    vbox_left->addWidget(boardViewController);

    //this->hbox = new QHBoxLayout();
    //hbox->addLayout(vbox_left);
    //hbox->addLayout(vbox_right);
    QWidget *lHboxWidget = new QWidget(this);
    lHboxWidget->setLayout(vbox_left);
    QWidget *rHboxWidget = new QWidget(this);
    rHboxWidget->setLayout(vbox_right);
    this->splitterLeftRight = new QSplitter(this);
    splitterLeftRight->addWidget(lHboxWidget);
    splitterLeftRight->addWidget(rHboxWidget);
    //splitterLeftRight->setStretchFactor(0,8);
    //splitterLeftRight->setStretchFactor(1,1);
    //int halfWidth = this->width() / 2;
    //splitterLeftRight->setSizes(QList<int>({halfWidth, halfWidth}));

    //hbox->setStretch(0,2);
    //hbox->setStretch(1,3);

    QSplitterHandle *handle = splitterLeftRight->handle(1);
    QHBoxLayout *layout = new QHBoxLayout(handle);
    layout->setSpacing(0);
    layout->setMargin(0);

    QFrame *line = new QFrame(handle);
    line->setFrameShape(QFrame::VLine);
    line->setFrameShadow(QFrame::Sunken);
    layout->addWidget(line);

    //completeLayout->addWidget(splitterLeftRight);

    this->splitterTopDown = new QSplitter(this);
    //QWidget* topWidget = new QWidget(this);
    //topWidget->setLayout(splitter)
    //splitterTopDown->add
    splitterTopDown->addWidget(splitterLeftRight);
    splitterTopDown->setOrientation(Qt::Vertical);

    QVBoxLayout *completeLayout = new QVBoxLayout();
    completeLayout->addLayout(hbox_right_engine_buttons);
    completeLayout->addWidget(engineViewController);
    QWidget* bottomWidget = new QWidget(this);
    bottomWidget->setLayout(completeLayout);
    splitterTopDown->addWidget(bottomWidget);

    //int fifthHeight = this->height() / 5;
    //splitterTopDown->setSizes(QList<int>({fifthHeight*4, fifthHeight}));

    //completeLayout->setStretch(0,6);
    //completeLayout->setStretch(1,1);
    //QLayout cLayout = new QLayout();
    QHBoxLayout *cLayout = new QHBoxLayout();
    cLayout->addWidget(splitterTopDown);


    QSplitterHandle *handle2 = splitterTopDown->handle(1);
    QHBoxLayout *layout2 = new QHBoxLayout(handle2);
    layout2->setSpacing(0);
    layout2->setMargin(0);

    QFrame *line2 = new QFrame(handle2);
    line2->setFrameShape(QFrame::HLine);
    line2->setFrameShadow(QFrame::Sunken);
    layout2->addWidget(line2);


    QSize iconSize = QSize(ICON_SIZE_LARGE * this->devicePixelRatio(), ICON_SIZE_LARGE* this->devicePixelRatio());
    QSize iconSizeSmall = QSize(ICON_SIZE_SMALL * this->devicePixelRatio(), ICON_SIZE_SMALL* this->devicePixelRatio());

    // FILE
    // Game
    QAction* actionNewGame = this->createAction("document-new", this->tr("New Game"), iconSize);
    QAction* actionOpen = this->createAction("document-open", this->tr("Open File"), iconSize);
    QAction* actionSaveAs = this->createAction("document-save", this->tr("Save Current\nGame As"), iconSize);
    // Print
    QAction* actionPrintGame = this->createAction("document-print", this->tr("Print Game"), iconSize);
    QAction* actionPrintPosition = this->createAction("document-print", this->tr("Print Position"), iconSize);
    // Layout
    QAction* actionColorStyle = this->createAction("applications-graphics", this->tr("Board Style"), iconSize);
    QAction* actionResetLayout = this->createAction("applications-graphics", this->tr("Reset Layout"), iconSize);
    // Quit
    QAction* actionQuit = this->createAction("document-print", this->tr("Exit"), iconSize);
    // Homepage
    QAction* actionHomepage = this->createAction("help-browser", this->tr("Homepage"), iconSize);
    // Help (About)
    QAction* actionAbout = this->createAction("help-browser", this->tr("About"), iconSize);

    // START
    // Game
    QAction* actionPaste = this->createAction("edit-paste", this->tr("Paste\nGame/Position"), iconSize);
    QAction* actionCopyGame = this->createAction("edit-copy-pgn", this->tr("Copy Game"), iconSizeSmall);
    QAction* actionCopyPosition = this->createAction("edit-copy-fen", this->tr("Copy Position"), iconSizeSmall);
    // Edit
    QAction* actionEditGameData = this->createAction("edit-copy-fen", this->tr("Edit\nMeta Data"), iconSize);
    QAction* actionEnterPosition = this->createAction("document-enter-position", this->tr("Setup\nNew Position"), iconSize);
    QAction* actionFlipBoard = this->createAction("view-refresh", this->tr("Flip Board"), iconSize);
    QAction* actionShowSearchInfo = this->createAction("view-refresh", this->tr("Show\nSearch Info"), iconSize);
    // Mode
    QAction* actionAnalysis = this->createAction("view-refresh", this->tr("Infinite\nAnalysis"), iconSize);
    QAction* actionPlayWhite = this->createAction("view-refresh", this->tr("Play\nWhite"), iconSize);
    QAction* actionPlayBlack = this->createAction("view-refresh", this->tr("Play\nBlack"), iconSize);
    QAction* actionEnterMoves = this->createAction("view-refresh", this->tr("Enter\nMoves"), iconSize);
    // Analysis
    QAction* actionFullGameAnalysis = this->createAction("emblem-system", this->tr("Full\nGame Analysis"), iconSize);
    QAction* actionEnginePlayout = this->createAction("emblem-system", this->tr("Engine\nPlayout"), iconSize);
    // Database
    QAction* actionDatabaseWindow = this->createAction("database", this->tr("Show\nDatabase"), iconSize);
    QAction* actionLoadPreviousGame = this->createAction("go-previous", this->tr("Previous Game"), iconSizeSmall);
    QAction* actionLoadNextGame = this->createAction("go-previous", this->tr("Next Game"), iconSizeSmall);



    this->ribbon = new RibbonWidget(this);
    this->addToolBar(this->ribbon);

    RibbonTab *gameTab = this->ribbon->addRibbonTab(this->tr(" File"));

    RibbonPane *filePane = gameTab->addRibbonPane(this->tr("Game"));
    filePane->addRibbonWidget(new RibbonButton(actionNewGame, true, this));
    filePane->addRibbonWidget(new RibbonButton(actionOpen, true, this));
    filePane->addRibbonWidget(new RibbonButton(actionSaveAs, true, this));

    RibbonPane *printPane = gameTab->addRibbonPane(this->tr("Print"));
    printPane->addRibbonWidget(new RibbonButton(actionPrintGame, true, this));
    printPane->addRibbonWidget(new RibbonButton(actionPrintPosition, true, this));

    RibbonPane *stylePane = gameTab->addRibbonPane(this->tr("Style"));
    stylePane->addRibbonWidget(new RibbonButton(actionColorStyle, true, this));
    stylePane->addRibbonWidget(new RibbonButton(actionResetLayout, true, this));

    RibbonPane *applicationPane = gameTab->addRibbonPane(this->tr("Application"));
    applicationPane->addRibbonWidget(new RibbonButton(actionQuit, true, this));
    applicationPane->addRibbonWidget(new RibbonButton(actionHomepage, true, this));
    applicationPane->addRibbonWidget(new RibbonButton(actionAbout, true, this));

    RibbonTab *startTab = this->ribbon->addRibbonTab(this->tr(" Start"));

    RibbonPane *editPane = startTab->addRibbonPane(this->tr("Edit"));
    editPane->addRibbonWidget(new RibbonButton(actionPaste, true, this));
    QFontMetrics fontMetrics = ribbon->fontMetrics();
    int btnSmallWidth = fontMetrics.width(actionCopyGame->text()) + ICON_SIZE_SMALL;
    QGridLayout *copyGrid = editPane->addGridWidget(btnSmallWidth * 1.8);
    copyGrid->addWidget(new RibbonButton(actionCopyGame, false, this),0,0);
    copyGrid->addWidget(new RibbonButton(actionCopyPosition, false, this),1,0);
    editPane->addRibbonWidget(new RibbonButton(actionEditGameData, true, this));

    RibbonPane *gamePane = startTab->addRibbonPane(this->tr("Game"));
    gamePane->addRibbonWidget(new RibbonButton(actionEnterPosition, true, this));
    gamePane->addRibbonWidget(new RibbonButton(actionFlipBoard, true, this));
    gamePane->addRibbonWidget(new RibbonButton(actionShowSearchInfo, true, this));

    RibbonPane *modePane = startTab->addRibbonPane(this->tr("Mode"));
    modePane->addRibbonWidget(new RibbonButton(actionAnalysis, true, this));
    modePane->addRibbonWidget(new RibbonButton(actionPlayWhite, true, this));
    modePane->addRibbonWidget(new RibbonButton(actionPlayBlack, true, this));
    modePane->addRibbonWidget(new RibbonButton(actionEnterMoves, true, this));

    RibbonTab *analysisTab = this->ribbon->addRibbonTab(this->tr(" Analysis"));
    RibbonPane *analysisPane = analysisTab->addRibbonPane(this->tr("Game Analysis"));
    analysisPane->addRibbonWidget(new RibbonButton(actionFullGameAnalysis, true, this));
    analysisPane->addRibbonWidget(new RibbonButton(actionEnginePlayout, true, this));

    RibbonPane *databasePane = analysisTab->addRibbonPane(this->tr("Database"));
    databasePane->addRibbonWidget(new RibbonButton(actionDatabaseWindow, true, this));
    int gridDbWidth = fontMetrics.width(actionLoadPreviousGame->text()) + (ICON_SIZE_SMALL * this->devicePixelRatio());
    QGridLayout *dbGrid = databasePane->addGridWidget(gridDbWidth*1.3);
    dbGrid->addWidget(new RibbonButton(actionLoadPreviousGame, false, this),0,0);
    dbGrid->addWidget(new RibbonButton(actionLoadNextGame, false, this),1,0);

    ribbon->setActive(" Start");

    mainWidget->setLayout(cLayout);

    this->setCentralWidget(mainWidget);
    QStatusBar *statusbar = this->statusBar();
    statusbar->showMessage("");
    this->setContextMenuPolicy(Qt::NoContextMenu);

    // SIGNALS AND SLOTS
    // ribbon entries

    connect(actionNewGame, &QAction::triggered, this->fileController, &FileController::newGame);
    connect(actionOpen, &QAction::triggered, this->fileController, &FileController::openGame);
    connect(actionSaveAs, &QAction::triggered, this->fileController, &FileController::saveAsNewGame);
    connect(actionPrintGame, &QAction::triggered, this->fileController, &FileController::printGame);
    connect(actionPrintPosition, &QAction::triggered, this->fileController, &FileController::printPosition);

    connect(actionColorStyle, &QAction::triggered, modeController, &ModeController::onOptionsClicked);
    connect(actionResetLayout, &QAction::triggered, this, &MainWindow::resetLayout);
    connect(actionQuit, &QAction::triggered, this, &QCoreApplication::quit);
    connect(actionHomepage, &QAction::triggered, this, &MainWindow::goToHomepage);
    connect(actionAbout, &QAction::triggered, this, &MainWindow::showAbout);

    connect(actionPaste, &QAction::triggered, this->editController, &EditController::paste);
    connect(actionCopyGame, &QAction::triggered, this->editController, &EditController::copyGameToClipBoard);
    connect(actionCopyPosition, &QAction::triggered, this->editController, &EditController::copyPositionToClipBoard);

    connect(actionEditGameData, &QAction::triggered, editController, &EditController::editHeaders);
    connect(actionEnterPosition, &QAction::triggered, editController, &EditController::enterPosition);
    connect(actionFlipBoard, &QAction::triggered, this->boardViewController, &BoardViewController::flipBoard);
    connect(actionShowSearchInfo, &QAction::triggered, this->engineViewController, &EngineView::flipShowEval);

    connect(actionAnalysis, &QAction::triggered, modeController, &ModeController::onActivateAnalysisMode);
    connect(actionPlayWhite, &QAction::triggered, modeController, &ModeController::onActivatePlayWhiteMode);
    connect(actionPlayBlack, &QAction::triggered, modeController, &ModeController::onActivatePlayBlackMode);
    connect(actionFullGameAnalysis, &QAction::triggered, modeController, &ModeController::onActivateGameAnalysisMode);
    connect(actionEnginePlayout, &QAction::triggered, modeController, &ModeController::onActivatePlayoutPositionMode);

    connect(actionDatabaseWindow, &QAction::triggered, fileController, &FileController::openDatabase);
    connect(actionLoadNextGame, &QAction::triggered, fileController, &FileController::toolbarNextGameInPGN);
    connect(actionLoadPreviousGame, &QAction::triggered, fileController, &FileController::toolbarPrevGameInPGN);

    // other signals
    connect(gameModel, &GameModel::stateChange, this, &MainWindow::onStateChange);

    connect(gameModel, &GameModel::stateChange, this->boardViewController, &BoardViewController::onStateChange);
    connect(gameModel, &GameModel::stateChange, this->moveViewController, &MoveViewController::onStateChange);
    connect(gameModel, &GameModel::stateChange, this->modeController, &ModeController::onStateChange);

    connect(right, &QPushButton::clicked, this->moveViewController, &MoveViewController::onForwardClick);
    connect(left, &QPushButton::clicked, this->moveViewController, &MoveViewController::onBackwardClick);

    connect(editEngines, &QPushButton::clicked, this->modeController, &ModeController::onSetEnginesClicked);
    connect(pbEngineOnOff, &QPushButton::toggled, this, &MainWindow::onEngineToggle);
    connect(editHeader, &QPushButton::clicked, editController, &EditController::editHeaders);

    //connect(enter_moves, &QAction::triggered, modeController, &ModeController::onActivateEnterMovesMode);

    connect(uciController, &UciController::bestmove, modeController, &ModeController::onBestMove);
    connect(uciController, &UciController::updateInfo, this->engineViewController, &EngineView::onNewInfo);
    connect(uciController, &UciController::bestPv, modeController, &ModeController::onBestPv);
    connect(uciController, &UciController::mateDetected, modeController, &ModeController::onMateDetected);
    connect(uciController, &UciController::eval, modeController, &ModeController::onEval);

    connect(fileController, &FileController::newGameEnterMoves, modeController, &ModeController::onActivateEnterMovesMode);
    connect(fileController, &FileController::newGamePlayBlack, modeController, &ModeController::onActivatePlayBlackMode);
    connect(fileController, &FileController::newGamePlayWhite, modeController, &ModeController::onActivatePlayWhiteMode);   

    connect(beginning, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekToBeginning);
    connect(end, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekToEnd);

    connect(this->spinMultiPv, qOverload<int>(&QSpinBox::valueChanged), this->modeController, &ModeController::onMultiPVChanged);

    this->gameModel->setMode(MODE_ENTER_MOVES);
    // enter_moves->setChecked(true);
    gameModel->triggerStateChange();

    this->resetLayout();

}

void MainWindow::setupStandardLayout() {
    this->setLayout(this->hbox);
}

void MainWindow::centerAndResize() {
    QDesktopWidget *desktop = qApp->desktop();
    QSize availableSize = desktop->availableGeometry().size();
    int width = availableSize.width();
    int height = availableSize.height();
    width = 0.7*width;
    height = 0.7*height;
    QSize newSize( width, height );

    setGeometry(
        QStyle::alignedRect(
            Qt::LeftToRight,
            Qt::AlignCenter,
            newSize,
            qApp->desktop()->availableGeometry()
        )
    );
}



void MainWindow::showAbout() {
    DialogAbout *dlg = new DialogAbout(this, JERRY_VERSION);
    dlg->exec();
    delete dlg;
}

void MainWindow::goToHomepage() {
    QDesktopServices::openUrl(QUrl("http://github.com/asdfjkl/jerry/"));
}

void MainWindow::onEngineToggle() {
    if(this->gameModel->getMode() == MODE_ENTER_MOVES) {
        //this->analysis_mode->setChecked(true);
        this->modeController->onActivateAnalysisMode();
    } else {
        //this->enter_moves->setChecked(true);
        this->modeController->onActivateEnterMovesMode();
    }
}

void MainWindow::saveImage() {
    QPixmap p = QPixmap::grabWindow(this->boardViewController->winId());
    QString filename = QFileDialog::getSaveFileName(this, tr("Save Image"),"" ,"JPG (*.jpg)");
    if(!filename.isEmpty()) {
        p.save(filename,"jpg");
    }
}

void MainWindow::onStateChange() {
    QString white = this->gameModel->getGame()->getHeader("White");
    QString black = this->gameModel->getGame()->getHeader("Black");
    QString site = this->gameModel->getGame()->getHeader("Site");
    QString date = this->gameModel->getGame()->getHeader("Date");
    QString line1 = QString("<b>").append(white).append(QString(" - ")).append(black).append(QString("</b><br/>"));
    QString line2 = site.append(QString(" ")).append(date);
    this->name->setText(line1.append(line2));
    /*
    if(this->gameModel->wasSaved) {
        this->save_game->setEnabled(true);
    } else {
        this->save_game->setEnabled(false);
    }*/
    if(this->gameModel->getMode() == MODE_ENTER_MOVES) {
        this->pbEngineOnOff->blockSignals(true);
        this->pbEngineOnOff->setChecked(false);
        this->pbEngineOnOff->blockSignals(false);
    } else if(this->gameModel->getMode() != MODE_ENTER_MOVES) {
        this->pbEngineOnOff->blockSignals(true);
        this->pbEngineOnOff->setChecked(true);
        this->pbEngineOnOff->blockSignals(false);
    }
    if(!this->gameModel->getGame()->wasEcoClassified) {
        this->gameModel->getGame()->findEco();
    }
    chess::EcoInfo ei = this->gameModel->getGame()->getEcoInfo();
    QString code = ei.code;
    QString info = ei.info;
    this->statusBar()->showMessage(code.append(" ").append(info));
    /*
    if(this->gameModel->getMode() == MODE_ANALYSIS) {
        this->analysis_mode->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_ENTER_MOVES) {
        this->enter_moves->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        this->enter_moves->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_ENTER_MOVES) {
        this->enter_moves->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_PLAY_WHITE) {
        this->play_white->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_PLAY_BLACK) {
        this->play_black->setChecked(true);
    }
    if(this->gameModel->showEval) {
        this->show_info->setChecked(true);
    } else {
        this->show_info->setChecked(false);
    }
    */
    this->update();
}

void MainWindow::aboutToQuit() {
    this->gameModel->saveGameState();
}

void MainWindow::wheelEvent(QWheelEvent *event)
{
    QPoint numPixels = event->pixelDelta();
    QPoint numDegrees = event->angleDelta() / 8;
    if (!numPixels.isNull()) {
        if(numPixels.y() > 10 && numPixels.y() < 200) {
            this->moveViewController->onScrollBack();
        } else if(numPixels.y() < 0) {
            this->moveViewController->onScrollForward();
        }
    } else if (!numDegrees.isNull()) {
        QPoint numSteps = numDegrees / 15;
        if(numSteps.y() >= 1) {
            this->moveViewController->onScrollBack();
        } else if(numSteps.y() < 0) {
            this->moveViewController->onScrollForward();
        }
    }

    event->accept();
}

void MainWindow::resetLayout() {
    int halfWidth = this->width() / 9;
    splitterLeftRight->setSizes(QList<int>({halfWidth*5, halfWidth*4}));
    int fifthHeight = this->height() / 5;
    splitterTopDown->setSizes(QList<int>({fifthHeight*4, fifthHeight}));


    this->update();
}

QPixmap* MainWindow::fromSvgToPixmap(const QSize &ImageSize, const QString &SvgFile)
{
 const qreal PixelRatio = this->devicePixelRatio();
 QSvgRenderer svgRenderer(SvgFile);
 QPixmap *img = new QPixmap(ImageSize*PixelRatio);
 QPainter Painter;

 img->fill(Qt::transparent);

 Painter.begin(img);
 svgRenderer.render(&Painter);
 Painter.end();

 img->setDevicePixelRatio(PixelRatio);

 return img;
}

QAction* MainWindow::createAction(QString name, const QString &displayName, QSize &iconSize) {

    QString resDir = ResourceFinder::getPath();
    resDir.append("/res/icons/");
    resDir.append(name);
    resDir.append(".svg");
    QPixmap *iconPixmap = this->fromSvgToPixmap(iconSize, resDir);
    QAction *action = new QAction(QIcon(*iconPixmap), displayName);
    return action;
}
