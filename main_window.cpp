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
#include <QButtonGroup>
#include <QDesktopServices>
#include "chess/pgn_reader.h"
#include "viewController/engineview.h"
#include "dialogs/dialog_about.h"
#include "various/resource_finder.h"
#include "various/messagebox.h"

#include "funct.h"

#include "chess/ecocode.h"

#ifdef __APPLE__
    const bool SHOW_ICON_TEXT = false;
#else
    const bool SHOW_ICON_TEXT = true;
#endif

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent) {

    // set working dir to executable work directory
    QDir::setCurrent(QCoreApplication::applicationDirPath());

    //chess::FuncT *f = new chess::FuncT();
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
    editHeader->setIcon(QIcon(":/res/icons/document-properties.svg"));

    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(this->name);
    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(editHeader);

    this->uciController     = new UciController();
    this->modeController    = new ModeController(gameModel, uciController, this);
    this->editController    = new EditController(gameModel, this);
    this->fileController    = new FileController(gameModel, this);

    QSize btnSize           = QSize(this->height()/19, this->height()/19);
    QSize btnSizeLR         = QSize(this->height()/14, this->height()/14);
    QPushButton *left       = new QPushButton();
    QPushButton *right      = new QPushButton();
    QPushButton *beginning  = new QPushButton();
    QPushButton *end        = new QPushButton();

    left->setIconSize(btnSizeLR);
    right->setIconSize(btnSizeLR);
    beginning->setIconSize(btnSize);
    end->setIconSize(btnSize);

    right->setIcon(QIcon(":/res/icons/go-next.svg"));
    left->setIcon(QIcon(":/res/icons/go-previous.svg"));
    beginning->setIcon(QIcon(":/res/icons/go-first.svg"));
    end->setIcon(QIcon(":/res/icons/go-last.svg"));

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
    this->lblMultiPv    = new QLabel(this->tr("Lines:"), this);
    this->spinMultiPv   = new QSpinBox(this);
    this->spinMultiPv->setRange(1,4);
    this->spinMultiPv->setValue(1);
    this->lblMultiPv->setBuddy(this->spinMultiPv);

    QPushButton *editEngines = new QPushButton();
    editEngines->setIcon(QIcon(":/res/icons/document-properties.svg"));

    hbox_right_engine_buttons->addWidget(pbEngineOnOff);
    hbox_right_engine_buttons->addWidget(this->lblMultiPv);
    hbox_right_engine_buttons->addWidget(this->spinMultiPv);
    hbox_right_engine_buttons->addStretch(1);
    hbox_right_engine_buttons->addWidget(editEngines);

    QVBoxLayout *vbox_right = new QVBoxLayout();
    vbox_right->addLayout(hbox_name_editHeader);
    vbox_right->addWidget(moveViewController);
    vbox_right->addLayout(hbox_buttons);

    vbox_right->setStretch(0,1);
    vbox_right->setStretch(1,4);
    vbox_right->setStretch(2,1);

    QVBoxLayout *vbox_left = new QVBoxLayout();
    vbox_left->addWidget(boardViewController);

    QWidget *lHboxWidget = new QWidget(this);
    lHboxWidget->setLayout(vbox_left);
    QWidget *rHboxWidget = new QWidget(this);
    rHboxWidget->setLayout(vbox_right);
    this->splitterLeftRight = new QSplitter(this);
    splitterLeftRight->addWidget(lHboxWidget);
    splitterLeftRight->addWidget(rHboxWidget);

    QSplitterHandle *handleLeftRight = splitterLeftRight->handle(1);
    QHBoxLayout *layoutSplitterLeftRight = new QHBoxLayout(handleLeftRight);
    layoutSplitterLeftRight->setSpacing(0);
    layoutSplitterLeftRight->setMargin(0);

    QFrame *frameLeftRight = new QFrame(handleLeftRight);
    frameLeftRight->setFrameShape(QFrame::VLine);
    frameLeftRight->setFrameShadow(QFrame::Sunken);
    layoutSplitterLeftRight->addWidget(frameLeftRight);


    this->splitterTopDown = new QSplitter(this);
    splitterTopDown->addWidget(splitterLeftRight);
    splitterTopDown->setOrientation(Qt::Vertical);

    QVBoxLayout *completeLayout = new QVBoxLayout();
    completeLayout->addLayout(hbox_right_engine_buttons);
    completeLayout->addWidget(engineViewController);
    QWidget* bottomWidget = new QWidget(this);
    bottomWidget->setLayout(completeLayout);
    splitterTopDown->addWidget(bottomWidget);

    QHBoxLayout *completeLayoutWithSplitter = new QHBoxLayout();
    completeLayoutWithSplitter->addWidget(splitterTopDown);

    QSplitterHandle *handleTopDown = splitterTopDown->handle(1);
    QHBoxLayout *layoutSplitterTopDown = new QHBoxLayout(handleTopDown);
    layoutSplitterTopDown->setSpacing(0);
    layoutSplitterTopDown->setMargin(0);

    QFrame *frameTopDown = new QFrame(handleTopDown);
    frameTopDown->setFrameShape(QFrame::HLine);
    frameTopDown->setFrameShadow(QFrame::Sunken);
    layoutSplitterTopDown->addWidget(frameTopDown);


    QMenu *m_game = this->menuBar()->addMenu(this->tr("Game"));
    QAction* actionNewGame = m_game->addAction(this->tr("New..."));
    QAction* actionOpen = m_game->addAction(this->tr("Open File"));
    QAction* actionSaveAs =  m_game->addAction("Save Current Game As");
    m_game->addSeparator();
    QAction* actionPrintGame = m_game->addAction(this->tr("Print Game"));
    QAction* actionPrintPosition = m_game->addAction(this->tr("Print Position"));
    QAction *save_diagram = m_game->addAction(this->tr("Save Position as Image..."));
    m_game->addSeparator();
    QAction *actionQuit = m_game->addAction(this->tr("Quit"));

    actionNewGame->setShortcut(QKeySequence::New);
    actionOpen->setShortcut(QKeySequence::Open);
    //save_game->setShortcut(QKeySequence::Save);
    actionSaveAs->setShortcut(QKeySequence::SaveAs);
    actionPrintGame->setShortcut(QKeySequence::Print);
    actionQuit->setShortcut(QKeySequence::Quit);

    // EDIT MENU
    QMenu *m_edit = this->menuBar()->addMenu(this->tr("Edit"));
    QAction *actionCopyGame = m_edit->addAction(this->tr("Copy Game"));
    QAction *actionCopyPosition = m_edit->addAction(this->tr("Copy Position"));
    QAction *actionPaste = m_edit->addAction(this->tr("Paste"));
    m_edit->addSeparator();
    QAction *actionEditGameData = m_edit->addAction(this->tr("Edit Game Data"));
    QAction *actionEnterPosition = m_edit->addAction(this->tr("&Enter Position"));
    m_edit->addSeparator();
    QAction *actionFlipBoard = m_edit->addAction(this->tr("&Flip Board"));
    this->actionShowSearchInfo = m_edit->addAction(this->tr("Show Search &Info"));
    QAction *actionColorStyle = m_edit->addAction(this->tr("Color Style..."));
    QAction *actionResetLayout = m_edit->addAction(this->tr("Reset Layout"));
    actionCopyGame->setShortcut(QKeySequence::Copy);
    actionPaste->setShortcut(QKeySequence::Paste);
    actionEnterPosition->setShortcut('e');
    actionFlipBoard->setCheckable(true);
    actionFlipBoard->setChecked(false);
    actionShowSearchInfo->setCheckable(true);
    actionShowSearchInfo->setChecked(true);

    // MODE MENU
    QMenu *m_mode = this->menuBar()->addMenu(this->tr("Mode"));
    QActionGroup *mode_actions = new QActionGroup(this);
    this->actionAnalysis = mode_actions->addAction(this->tr("&Analysis Mode"));
    this->actionPlayWhite = mode_actions->addAction(this->tr("Play as &White"));
    this->actionPlayBlack = mode_actions->addAction(this->tr("Play as &Black"));
    this->actionEnterMoves = mode_actions->addAction(this->tr("Enter &Moves"));
    actionAnalysis->setCheckable(true);
    actionPlayWhite->setCheckable(true);
    actionPlayBlack->setCheckable(true);
    actionEnterMoves->setCheckable(true);
    mode_actions->setExclusive(true);
    m_mode->addAction(actionAnalysis);
    m_mode->addAction(actionPlayWhite);
    m_mode->addAction(actionPlayBlack);
    m_mode->addAction(actionEnterMoves);
    m_mode->addSeparator();
    QAction* actionFullGameAnalysis = m_mode->addAction(this->tr("Full Game Analysis"));
    QAction* actionEnginePlayout = m_mode->addAction(this->tr("Playout Position"));
    m_mode->addSeparator();
    QAction *actionSetEngines = m_mode->addAction(this->tr("Engines..."));
    QShortcut *sc_analysis_mode = new QShortcut(QKeySequence(Qt::Key_A), this);
    sc_analysis_mode->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_play_white = new QShortcut(QKeySequence(Qt::Key_W), this);
    sc_play_white->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_play_black = new QShortcut(QKeySequence(Qt::Key_B), this);
    sc_play_black->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_enter_move_mode_m = new QShortcut(QKeySequence(Qt::Key_M), this);
    QShortcut *sc_enter_move_mode_esc = new QShortcut(QKeySequence(Qt::Key_Escape), this);
    sc_enter_move_mode_m->setContext(Qt::ApplicationShortcut);
    sc_enter_move_mode_esc->setContext(Qt::ApplicationShortcut);

    // DATABASE MENU
    QMenu *m_database = this->menuBar()->addMenu(this->tr("Database"));
    QAction* actionDatabaseWindow = m_database->addAction(this->tr("Browse Games"));
    QAction* actionLoadNextGame = m_database->addAction(this->tr("Next Game"));
    QAction* actionLoadPreviousGame = m_database->addAction(this->tr("Previous Game"));

    // HELP MENU
    QMenu *m_help = this->menuBar()->addMenu(this->tr("Help "));
    QAction *actionAbout = m_help->addAction(this->tr("About"));
    QAction *actionHomepage = m_help->addAction(this->tr("Jerry-Homepage"));


    // TOOLBAR
    this->toolbar = addToolBar("main toolbar");
    this->toolbar->setMovable(false);
    //this->toolbar->setFixedHeight(72);
    //this->toolbar->setIconSize(QSize(72,72));
    //QSize iconSize = toolbar->iconSize() * this->devicePixelRatio();
    //toolbar->setIconSize(iconSize);
    toolbar->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);
    QAction *tbActionNew = toolbar->addAction(QIcon(":/res/icons/document-new.svg"), this->tr("New"));
    QAction *tbActionOpen = toolbar->addAction(QIcon(":/res/icons/document-open.svg"), this->tr("Open"));
    QAction *tbActionSaveAs = toolbar->addAction(QIcon(":/res/icons/document-save.svg"), this->tr("Save As"));
    QAction *tbActionPrint = toolbar->addAction(QIcon(":/res/icons/document-print.svg"), this->tr("Print"));

    toolbar->addSeparator();

    QAction *tbActionFlip = toolbar->addAction(QIcon(":/res/icons/view-refresh.svg"), this->tr("Flip Board"));

    toolbar->addSeparator();

    QAction *tbActionCopyGame = toolbar->addAction(QIcon(":/res/icons/edit-copy-pgn.svg"), this->tr("Copy Game"));
    QAction *tbActionCopyPosition = toolbar->addAction(QIcon(":/res/icons/edit-copy-fen.svg"), this->tr("Copy Position"));
    QAction *tbActionPaste = toolbar->addAction(QIcon(":/res/icons/edit-paste.svg"), this->tr("Paste"));
    QAction *tbActionEnterPosition = toolbar->addAction(QIcon(":/res/icons/document-enter-position.svg"), this->tr("Enter Position"));

    toolbar->addSeparator();

    QAction *tbActionAnalysis = toolbar->addAction(QIcon(":/res/icons/emblem-system.svg"), this->tr("Full Analysis"));

    toolbar->addSeparator();

    QAction *tbActionDatabase = toolbar->addAction(QIcon(":/res/icons/database.svg"), this->tr("Browse Games"));
    QAction *tbActionPrevGame = toolbar->addAction(QIcon(":/res/icons/go-previous.svg"), this->tr("Prev. Game"));
    QAction *tbActionNextGame = toolbar->addAction(QIcon(":/res/icons/go-next.svg"), this->tr("Next Game"));

    QWidget* spacer = new QWidget();
    spacer->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    toolbar->addWidget(spacer);

    QAction *tbActionHelp = toolbar->addAction(QIcon(":/res/icons/help-browser.svg"), this->tr("About"));

    // toolbar shortcuts
    QShortcut *sc_flip = new QShortcut(QKeySequence(Qt::Key_F), this);
    sc_flip->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_enter_pos = new QShortcut(QKeySequence(Qt::Key_E), this);
    sc_enter_pos->setContext(Qt::ApplicationShortcut);


    /*
    // FILE
    // Game
    QAction* actionNewGame = this->createAction("document-new", this->tr("New Game"), iconSize);
    QAction* actionOpen = this->createAction("document-open", this->tr("Open File"), iconSize);
    QAction* actionSaveAs = this->createAction("document-save", this->tr("Save Current\nGame As"), iconSize);
    // Print
    QAction* actionPrintGame = this->createAction("document-print", this->tr("Print Game"), iconSize);
    QAction* actionPrintPosition = this->createAction("document-print-board", this->tr("Print Position"), iconSize);
    // Layout
    QAction* actionColorStyle = this->createAction("applications-graphics", this->tr("Board Style"), iconSize);
    QAction* actionResetLayout = this->createAction("preferences-system-session", this->tr("Reset Layout"), iconSize);
    // Quit
    QAction* actionQuit = this->createAction("system-log-out", this->tr("Exit"), iconSize);
    // Homepage
    QAction* actionHomepage = this->createAction("internet-web-browser", this->tr("Homepage"), iconSize);
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
    //QAction* actionShowSearchInfo = this->createAction("view-refresh", this->tr("Show\nSearch Info"), iconSize);
    // Mode
    QAction* actionAnalysis = this->createAction("edit-find", this->tr("Infinite\nAnalysis"), iconSize);
    QAction* actionPlayWhite = this->createAction("play-white", this->tr("Play\nWhite"), iconSize);
    QAction* actionPlayBlack = this->createAction("play-black", this->tr("Play\nBlack"), iconSize);
    QAction* actionEnterMoves = this->createAction("text-pencil", this->tr("Enter\nMoves"), iconSize);
    // Analysis
    QAction* actionFullGameAnalysis = this->createAction("edit-find-replace", this->tr("Full\nGame Analysis"), iconSize);
    QAction* actionEnginePlayout = this->createAction("dialog-information", this->tr("Engine\nPlayout"), iconSize);
    // Database
    QAction* actionDatabaseWindow = this->createAction("database", this->tr("Show\nDatabase"), iconSize);
    QAction* actionLoadPreviousGame = this->createAction("go-previous", this->tr("Previous Game"), iconSizeSmall);
    QAction* actionLoadNextGame = this->createAction("go-previous", this->tr("Next Game"), iconSizeSmall);
    */

    mainWidget->setLayout(completeLayoutWithSplitter);

    this->setCentralWidget(mainWidget);
    QStatusBar *statusbar = this->statusBar();
    statusbar->showMessage("");
    this->setContextMenuPolicy(Qt::NoContextMenu);

    // SIGNALS AND SLOTS

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
    //connect(actionShowSearchInfo, &QAction::triggered, this->engineViewController, &EngineView::flipShowEval);

    connect(actionShowSearchInfo, &QAction::triggered, this->engineViewController, &EngineView::flipShowEval);

    connect(actionAnalysis, &QAction::triggered, modeController, &ModeController::onActivateAnalysisMode);
    connect(actionEnterMoves, &QAction::triggered, modeController, &ModeController::onActivateEnterMovesMode);
    connect(actionPlayWhite, &QAction::triggered, modeController, &ModeController::onActivatePlayWhiteMode);
    connect(actionPlayBlack, &QAction::triggered, modeController, &ModeController::onActivatePlayBlackMode);
    connect(actionFullGameAnalysis, &QAction::triggered, modeController, &ModeController::onActivateGameAnalysisMode);
    connect(actionEnginePlayout, &QAction::triggered, modeController, &ModeController::onActivatePlayoutPositionMode);

    connect(actionSetEngines, &QAction::triggered, this->modeController, &ModeController::onSetEnginesClicked);

    connect(actionDatabaseWindow, &QAction::triggered, fileController, &FileController::openDatabase);
    connect(actionLoadNextGame, &QAction::triggered, fileController, &FileController::toolbarNextGameInPGN);
    connect(actionLoadPreviousGame, &QAction::triggered, fileController, &FileController::toolbarPrevGameInPGN);

    // toolbar buttons

    connect(sc_flip, &QShortcut::activated, actionFlipBoard, &QAction::trigger);

    connect(tbActionNew,  &QAction::triggered, actionNewGame, &QAction::trigger);
    connect(tbActionOpen,  &QAction::triggered, actionOpen, &QAction::trigger);
    connect(tbActionSaveAs,  &QAction::triggered, actionSaveAs, &QAction::trigger);
    connect(tbActionPrint,  &QAction::triggered, actionPrintGame, &QAction::trigger);
    connect(tbActionFlip,  &QAction::triggered, actionFlipBoard, &QAction::trigger);
    connect(tbActionCopyGame,  &QAction::triggered, actionCopyGame, &QAction::trigger);
    connect(tbActionCopyPosition,  &QAction::triggered, actionCopyPosition, &QAction::trigger);
    connect(tbActionPaste,  &QAction::triggered, actionPaste, &QAction::trigger);
    connect(tbActionEnterPosition,  &QAction::triggered, actionEnterPosition, &QAction::trigger);
    connect(tbActionAnalysis,  &QAction::triggered, actionFullGameAnalysis, &QAction::trigger);
    connect(tbActionDatabase,  &QAction::triggered, actionDatabaseWindow, &QAction::trigger);
    connect(tbActionPrevGame,  &QAction::triggered, actionLoadPreviousGame, &QAction::trigger);
    connect(tbActionNextGame,  &QAction::triggered, actionLoadNextGame, &QAction::trigger);
    connect(tbActionHelp,  &QAction::triggered, actionAbout, &QAction::trigger);

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
    this->actionEnterMoves->setChecked(true);
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
    width = 0.85*width;
    height = 0.85*height;
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
    QDesktopServices::openUrl(QUrl("http://asdfjkl.github.io/jerry/"));
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
    QString filename = QFileDialog::getSaveFileName(this, tr("Save Image"),"","JPG (*.jpg)");
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
    } else {
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

    if(this->gameModel->getMode() == MODE_ANALYSIS) {
        this->actionAnalysis->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_ENTER_MOVES) {
        this->actionEnterMoves->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_GAME_ANALYSIS) {
        this->actionEnterMoves->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_PLAY_WHITE) {
        this->actionPlayWhite->setChecked(true);
    } else if(this->gameModel->getMode() == MODE_PLAY_BLACK) {
        this->actionPlayBlack->setChecked(true);
    }

    if(this->gameModel->showEval) {
        this->actionShowSearchInfo->setChecked(Qt::Checked);
    } else {
        this->actionShowSearchInfo->setChecked(Qt::Unchecked);
    }
    this->update();
}

void MainWindow::aboutToQuit() {
    this->gameModel->saveGameState();
}

void MainWindow::wheelEvent(QWheelEvent *event) {
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

QPixmap* MainWindow::fromSvgToPixmap(const QSize &ImageSize, const QString &SvgFile) {
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


