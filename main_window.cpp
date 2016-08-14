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
#include <QPrintDialog>
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
#include <QDesktopServices>
#include "chess/pgn_reader.h"
#include "viewController/engineview.h"
#include "dialogs/dialog_about.h"
#include "various/resource_finder.h"
#include "various/messagebox.h"

#include "funct.h"

#include "chess/ecocode.h"

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent)
{

    // set working dir to executable work directory
    QDir::setCurrent(QCoreApplication::applicationDirPath());

    QString resDir = ResourceFinder::getPath();

    //chess::FuncT *f = new chess::FuncT();
    //f->run_polyglot();
    /*
    f->run_pgnt();
    f->run_pgn_scant();
    */

    // chess::EcoCode *e = new chess::EcoCode();

    // test: nakmura game
    // chess::PgnReader *reader = new chess::PgnReader();
    //chess::Game* g_test = reader->readGame("/home/user/workspace/jerryv3/data/nakamura_rybka_ascii_linux.pgn");
    //chess::Game* g_test = reader->readGame("/home/user/workspace/jerryv3/data/complex.pgn");

    //chess::Game* g_test = reader->readGameFromFile("/home/user/pgns/spieler8_vs_ttconnor153_2016_02_08.pgn");

    //chess::Board *b = new chess::Board(QString("7k/7P/8/6K1/8/8/8/8 w - - 0 1"));
    //chess::GameNode *gn = new chess::GameNode();
    //gn->setBoard(b);
    //g_test->setCurrent(gn);
    //g_test->setRoot(gn);

    // reconstruct gameModel
    this->gameModel = new GameModel();
    //this->gameModel->setGame(g_test);
    this->gameModel->restoreGameState();
    this->gameModel->getGame()->treeWasChanged = true;

    //chess::GameNode *root = this->gameModel->getGame()->getRootNode();
    //root->setBoard(new chess::Board(true));
    //root->setBoard(b);

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

    QPixmap pxEditHeader(resDir + "/res/icons/document-properties.png");
    editHeader->setIcon(QIcon(pxEditHeader));

    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(this->name);
    hbox_name_editHeader->addStretch(1);
    hbox_name_editHeader->addWidget(editHeader);
    //hbox_right_engine_buttons->addStretch(1);



    this->uciController = new UciController();

    this->modeController = new ModeController(gameModel, uciController, this);
    this->editController = new EditController(gameModel, this);
    this->fileController = new FileController(gameModel, this);

    QSize btnSize = QSize(this->height()/19, this->height()/19);
    QSize btnSizeLR = QSize(this->height()/14, this->height()/14);
    QPushButton *left = new QPushButton();
    QPushButton *right = new QPushButton();
    QPushButton *stepleft = new QPushButton();
    QPushButton *beginning = new QPushButton();
    QPushButton *stepright = new QPushButton();
    QPushButton *end = new QPushButton();

    QPixmap pxRight(resDir + "/res/icons/go-next.png");
    QPixmap pxLeft(resDir + "/res/icons/go-previous.png");
    QPixmap pxStepRight(resDir + "/res/icons/media-seek-forward.png");
    QPixmap pxStepLeft(resDir + "/res/icons/media-seek-backward.png");
    QPixmap pxBeginning(resDir + "/res/icons/go-first.png");
    QPixmap pxEnd(resDir + "/res/icons/go-last.png");

    left->setIcon(QIcon(pxLeft));
    left->setIconSize(btnSizeLR);
    right->setIcon(QIcon(pxRight));
    right->setIconSize(btnSizeLR);
    stepleft->setIcon(QIcon(pxStepLeft));
    stepleft->setIconSize(btnSize);
    stepright->setIcon(QIcon(pxStepRight));
    stepright->setIconSize(btnSize);
    beginning->setIcon(QIcon(pxBeginning));
    beginning->setIconSize(btnSize);
    end->setIcon(QIcon(pxEnd));
    end->setIconSize(btnSize);

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
    //hbox_buttons->addWidget(stepleft);
    hbox_buttons->addWidget(left);
    hbox_buttons->addWidget(right);
    //hbox_buttons->addWidget(stepright);
    hbox_buttons->addWidget(end);
    hbox_buttons->addStretch(1);

    QHBoxLayout *hbox_right_engine_buttons = new QHBoxLayout();
    //this->pbEngineOnOff = new SwitchButton(); //new QPushButton("OFF");
    //this->pbEngineOnOff->setColors(Qt::darkGray, Qt::darkGray);
    this->pbEngineOnOff = new OnOffButton(this); //new QPushButton("OFF");
    QPushButton *editEngines = new QPushButton();

    QPixmap pxEditPgnHeader(resDir + "/res/icons/document-properties.png");
    editEngines->setIcon(QIcon(pxEditPgnHeader));
    //editPgnHeader->setIconSize(pbEngineOnOff->size());

    hbox_right_engine_buttons->addWidget(pbEngineOnOff);
    hbox_right_engine_buttons->addStretch(1);
    hbox_right_engine_buttons->addWidget(editEngines);

    QVBoxLayout *vbox_right = new QVBoxLayout();
    //vbox_right->addWidget(name);
    vbox_right->addLayout(hbox_name_editHeader);
    vbox_right->addWidget(moveViewController);
    vbox_right->addLayout(hbox_buttons);
    vbox_right->addLayout(hbox_right_engine_buttons);
    vbox_right->addWidget(engineViewController);

    vbox_right->setStretch(0,1);
    vbox_right->setStretch(1,4);
    vbox_right->setStretch(2,1);
    vbox_right->setStretch(3,1);
    vbox_right->setStretch(4,4);

    QVBoxLayout *vbox_left = new QVBoxLayout();
    vbox_left->addWidget(boardViewController);

    //vbox_left->addLayout(hbox_buttons);

    this->hbox = new QHBoxLayout();
    hbox->addLayout(vbox_left);
    hbox->addLayout(vbox_right);
    hbox->setStretch(0,1);
    hbox->setStretch(1,2);

    QToolBar *toolbar = addToolBar("main toolbar");

    QPixmap *tbNew = new QPixmap(resDir + "/res/icons/document-new.png");
    QAction *tbActionNew = toolbar->addAction(QIcon(*tbNew), this->tr("New Game"));

    QPixmap *tbOpen = new QPixmap(resDir + "/res/icons/document-open.png");
    QAction *tbActionOpen = toolbar->addAction(QIcon(*tbOpen), this->tr("Open Game"));

    QPixmap *tbSave = new QPixmap(resDir + "/res/icons/document-save.png");
    QAction *tbActionSave = toolbar->addAction(QIcon(*tbSave), this->tr("Save Game"));

    //QPixmap *tbPrint = new QPixmap("res/icons/document-print.png");
    //QAction *tbActionPrint = toolbar->addAction(QIcon(*tbPrint), this->tr("Print Game"));

    toolbar->addSeparator();

    QPixmap *tbFlip = new QPixmap(resDir + "/res/icons/view-refresh.png");
    QAction *tbActionFlip = toolbar->addAction(QIcon(*tbFlip), this->tr("Flip Board"));

    toolbar->addSeparator();

    QPixmap *tbCopyGame = new QPixmap(resDir + "/res/icons/edit-copy.png");
    QAction *tbActionCopyGame = toolbar->addAction(QIcon(*tbCopyGame), this->tr("Copy Game"));

    QPixmap *tbCopyPosition = new QPixmap(resDir + "/res/icons/edit-copy-fen.png");
    QAction *tbActionCopyPosition = toolbar->addAction(QIcon(*tbCopyPosition), this->tr("Copy Position"));

    QPixmap *tbPaste = new QPixmap(resDir + "/res/icons/edit-paste.png");
    QAction *tbActionPaste = toolbar->addAction(QIcon(*tbPaste), this->tr("Paste Game/Position"));

    QPixmap *tbEnterPosition = new QPixmap(resDir + "/res/icons/document-new-board.png");
    QAction *tbActionEnterPosition = toolbar->addAction(QIcon(*tbEnterPosition), this->tr("Enter Position"));

    toolbar->addSeparator();

    QPixmap *tbAnalysis = new QPixmap(resDir + "/res/icons/dialog-information-on.png");
    QAction *tbActionAnalysis = toolbar->addAction(QIcon(*tbAnalysis), this->tr("Full Game Analysis"));

    toolbar->addSeparator();

    QPixmap *tbStyle = new QPixmap(resDir + "/res/icons/applications-graphics.png");
    QAction *tbActionStyle = toolbar->addAction(QIcon(*tbStyle), this->tr("Colorstyle"));

    QWidget* spacer = new QWidget();
    spacer->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    // toolBar is a pointer to an existing toolbar
    toolbar->addWidget(spacer);

    QPixmap *tbHelp = new QPixmap(resDir + "/res/icons/help-browser.png");
    QAction *tbActionHelp = toolbar->addAction(QIcon(*tbHelp), this->tr("About"));

    mainWidget->setLayout(hbox);

    this->setCentralWidget(mainWidget);
    QStatusBar *statusbar = this->statusBar();
    statusbar->showMessage("");


    // setting up the menus
    // GAME MENU
    QMenu *m_game = this->menuBar()->addMenu(this->tr("Game"));
    QAction *new_game = m_game->addAction(this->tr("New..."));
    QAction *open_game = m_game->addAction(this->tr("Open..."));
    this->save_game = m_game->addAction(this->tr("Save..."));
    QAction *save_game_as = m_game->addAction(this->tr("Save As..."));

    m_game->addSeparator();
    QAction *save_diagram = m_game->addAction(this->tr("Save Position as Image..."));
    QAction *print_game = m_game->addAction(this->tr("Print Game"));
    QAction *print_position = m_game->addAction(this->tr("Print FEN"));

    m_game->addSeparator();
    QAction *quit = m_game->addAction(this->tr("Quit"));

    new_game->setShortcut(QKeySequence::New);
    open_game->setShortcut(QKeySequence::Open);
    save_game->setShortcut(QKeySequence::Save);
    save_game_as->setShortcut(QKeySequence::SaveAs);
    print_game->setShortcut(QKeySequence::Print);
    quit->setShortcut(QKeySequence::Quit);

    //QShortcut *sc_new_game = new QShortcut(QKeySequence::New, this);
    //sc_new_game->setContext(Qt::ApplicationShortcut);

    //QShortcut *sc_open_game = new QShortcut(QKeySequence::Open, this);
    //sc_open_game->setContext(Qt::ApplicationShortcut);

    //QShortcut *sc_save_game = new QShortcut(QKeySequence::Save, this);
    //sc_save_game->setContext(Qt::ApplicationShortcut);

    //QShortcut *sc_save_game_as = new QShortcut(QKeySequence::SaveAs, this);
    //sc_save_game_as->setContext(Qt::ApplicationShortcut);

    //QShortcut *sc_print_game = new QShortcut(QKeySequence::Print, this);
    //sc_print_game->setContext(Qt::ApplicationShortcut);

    //QShortcut *sc_quit = new QShortcut(QKeySequence::Quit, this);
    //sc_quit->setContext(Qt::ApplicationShortcut);

    connect(quit, &QAction::triggered, this, &QWidget::close);
    //connect(sc_quit, &QShortcut::activated, quit, &QAction::trigger);

    // EDIT MENU
    QMenu *m_edit = this->menuBar()->addMenu(this->tr("Edit"));
    QAction *copy_game = m_edit->addAction(this->tr("Copy Game"));
    QAction *copy_position = m_edit->addAction(this->tr("Copy Position"));
    QAction *paste = m_edit->addAction(this->tr("Paste"));

    m_edit->addSeparator();

    QAction *edit_headers = m_edit->addAction(this->tr("Edit Game Data"));
    QAction *enter_position = m_edit->addAction(this->tr("&Enter Position"));
    //QAction *reset_to_initial = m_edit->addAction(this->tr("Reset to Initial"));

    m_edit->addSeparator();
    QAction *flip_board = m_edit->addAction(this->tr("&Flip Board"));
    this->show_info = m_edit->addAction(this->tr("Show Search &Info"));

    //m_edit->addSeparator();
    //QAction *offer_draw = m_edit->addAction(this->tr("Offer Draw"));
    //QAction *resign = m_edit->addAction(this->tr("Resign"));

    copy_game->setShortcut(QKeySequence::Copy);
    paste->setShortcut(QKeySequence::Paste);
    enter_position->setShortcut('e');

    flip_board->setCheckable(true);
    flip_board->setChecked(false);
    show_info->setCheckable(true);
    show_info->setChecked(true);

    //offer_draw->setEnabled(false);
    //resign->setEnabled(false);

    //QShortcut *sc_copy = new QShortcut(QKeySequence::Copy, this);
    //sc_copy->setContext(Qt::ApplicationShortcut);
    //QShortcut *sc_paste = new QShortcut(QKeySequence::Paste, this);
    //sc_paste->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_flip = new QShortcut(QKeySequence(Qt::Key_F), this);
    sc_flip->setContext(Qt::ApplicationShortcut);
    QShortcut *sc_enter_pos = new QShortcut(QKeySequence(Qt::Key_E), this);
    sc_enter_pos->setContext(Qt::ApplicationShortcut);

    flip_board->setShortcut('f');

    // MODE MENU
    QMenu *m_mode = this->menuBar()->addMenu(this->tr("Mode"));
    QActionGroup *mode_actions = new QActionGroup(this);
    this->analysis_mode = mode_actions->addAction(this->tr("&Analysis Mode"));
    this->play_white = mode_actions->addAction(this->tr("Play as &White"));
    this->play_black = mode_actions->addAction(this->tr("Play as &Black"));
    this->enter_moves = mode_actions->addAction(this->tr("Enter &Moves"));

    analysis_mode->setCheckable(true);
    play_white->setCheckable(true);
    play_black->setCheckable(true);
    enter_moves->setCheckable(true);

    mode_actions->setExclusive(true);

    m_mode->addAction(analysis_mode);
    m_mode->addAction(play_white);
    m_mode->addAction(play_black);
    m_mode->addAction(enter_moves);
    m_mode->addSeparator();

    QAction *game_analysis = m_mode->addAction(this->tr("Full Game Analysis"));
    QAction *playout_position = m_mode->addAction(this->tr("Playout Position"));

    m_mode->addSeparator();
    QAction *set_engines = m_mode->addAction(this->tr("Engines..."));
    QAction *options = m_mode->addAction(this->tr("Options..."));

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


    // HELP MENU
    QMenu *m_help = this->menuBar()->addMenu(this->tr("Help "));

    QAction *about = m_help->addAction(this->tr("About"));
    QAction *homepage = m_help->addAction(this->tr("Jerry-Homepage"));

    connect(save_diagram, &QAction::triggered, this, &MainWindow::saveImage);

    // SIGNALS AND SLOTS
    connect(sc_flip, &QShortcut::activated, flip_board, &QAction::trigger);

    connect(copy_game, &QAction::triggered, this->editController, &EditController::copyGameToClipBoard);
    //connect(sc_copy, &QShortcut::activated, copy_game, &QAction::trigger);
    connect(tbActionCopyGame, &QAction::triggered, copy_game, &QAction::trigger);
    connect(copy_position, &QAction::triggered, this->editController, &EditController::copyPositionToClipBoard);
    connect(tbActionCopyPosition, &QAction::triggered, copy_position, &QAction::trigger);

    connect(paste, &QAction::triggered, this->editController, &EditController::paste);
    //connect(sc_paste, &QShortcut::activated, paste, &QAction::trigger);
    connect(tbActionPaste, &QAction::triggered, paste, &QAction::trigger);

    connect(print_game, &QAction::triggered, this->fileController, &FileController::printGame);
    //connect(sc_print_game, &QShortcut::activated, print_game, &QAction::trigger);

    connect(print_position, &QAction::triggered, this->fileController, &FileController::printPosition);
    //connect(tbActionPrint, &QAction::triggered, print_position, &QAction::trigger);

    connect(new_game, &QAction::triggered, this->fileController, &FileController::newGame);
    //connect(sc_new_game, &QShortcut::activated, new_game, &QAction::trigger);
    connect(tbActionNew, &QAction::triggered, new_game, &QAction::trigger);

    connect(open_game, &QAction::triggered, this->fileController, &FileController::openGame);
    //connect(sc_open_game, &QShortcut::activated, open_game, &QAction::trigger);
    connect(tbActionOpen, &QAction::triggered, open_game, &QAction::trigger);

    connect(save_game, &QAction::triggered, this->fileController, &FileController::saveGame);
    //connect(sc_save_game, &QShortcut::activated, save_game, &QAction::trigger);

    connect(save_game_as, &QAction::triggered, this->fileController, &FileController::saveAsNewGame);
    //connect(sc_save_game_as, &QShortcut::activated, open_game, &QAction::trigger);
    connect(tbActionSave, &QAction::triggered, this->fileController, &FileController::toolbarSaveGame);

    connect(gameModel, &GameModel::stateChange, this, &MainWindow::onStateChange);

    connect(flip_board, &QAction::triggered, this->boardViewController, &BoardViewController::flipBoard);
    connect(tbActionFlip, &QAction::triggered, flip_board, &QAction::trigger);

    connect(show_info, &QAction::triggered, this->engineViewController, &EngineView::flipShowEval);
    connect(gameModel, &GameModel::stateChange, this->boardViewController, &BoardViewController::onStateChange);
    connect(gameModel, &GameModel::stateChange, this->moveViewController, &MoveViewController::onStateChange);
    connect(gameModel, &GameModel::stateChange, this->modeController, &ModeController::onStateChange);

    connect(right, &QPushButton::clicked, this->moveViewController, &MoveViewController::onForwardClick);
    connect(left, &QPushButton::clicked, this->moveViewController, &MoveViewController::onBackwardClick);

    connect(options, &QAction::triggered, modeController, &ModeController::onOptionsClicked);
    connect(tbActionStyle, &QAction::triggered, modeController, &ModeController::onOptionsClicked);

    connect(set_engines, &QAction::triggered, modeController, &ModeController::onSetEnginesClicked);
    connect(editEngines, &QPushButton::clicked, set_engines, &QAction::trigger);
    connect(analysis_mode, &QAction::triggered, modeController, &ModeController::onActivateAnalysisMode);
    connect(play_white, &QAction::triggered, modeController, &ModeController::onActivatePlayWhiteMode);
    connect(play_black, &QAction::triggered, modeController, &ModeController::onActivatePlayBlackMode);
    connect(sc_analysis_mode, &QShortcut::activated, analysis_mode, &QAction::trigger);
    connect(game_analysis, &QAction::triggered, modeController, &ModeController::onActivateGameAnalysisMode);
    connect(tbActionAnalysis, &QAction::triggered, game_analysis, &QAction::trigger);
    connect(pbEngineOnOff, &QPushButton::toggled, this, &MainWindow::onEngineToggle);

    connect(edit_headers, &QAction::triggered, editController, &EditController::editHeaders);
    connect(editHeader, &QPushButton::clicked, editController, &EditController::editHeaders);
    connect(enter_position, &QAction::triggered, editController, &EditController::enterPosition);
    connect(sc_enter_pos, &QShortcut::activated, enter_position, &QAction::trigger);
    connect(tbActionEnterPosition, &QAction::triggered, enter_position, &QAction::trigger);

    connect(enter_moves, &QAction::triggered, modeController, &ModeController::onActivateEnterMovesMode);
    connect(sc_enter_move_mode_esc, &QShortcut::activated, enter_moves, &QAction::trigger);
    connect(sc_enter_move_mode_m, &QShortcut::activated, enter_moves, &QAction::trigger);

    connect(playout_position, &QAction::triggered, modeController, &ModeController::onActivatePlayoutPositionMode);

    connect(about, &QAction::triggered, this, &MainWindow::showAbout);
    connect(tbActionHelp, &QAction::triggered, about, &QAction::trigger);

    connect(homepage, &QAction::triggered, this, &MainWindow::goToHomepage);

    connect(uciController, &UciController::bestmove, modeController, &ModeController::onBestMove);
    connect(uciController, &UciController::updateInfo, this->engineViewController, &EngineView::onNewInfo);

    connect(uciController, &UciController::bestPv, modeController, &ModeController::onBestPv);
    connect(uciController, &UciController::mateDetected, modeController, &ModeController::onMateDetected);
    connect(uciController, &UciController::eval, modeController, &ModeController::onEval);

    connect(fileController, &FileController::newGameEnterMoves, modeController, &ModeController::onActivateEnterMovesMode);
    connect(fileController, &FileController::newGamePlayBlack, modeController, &ModeController::onActivatePlayBlackMode);
    connect(fileController, &FileController::newGamePlayWhite, modeController, &ModeController::onActivatePlayWhiteMode);

    //connect(stepleft, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekBackwardClick);
    //connect(stepright, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekForwardClick);
    connect(beginning, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekToBeginning);
    connect(end, &QPushButton::pressed, this->moveViewController, &MoveViewController::onSeekToEnd);

    this->gameModel->setMode(MODE_ENTER_MOVES);
    enter_moves->setChecked(true);
    gameModel->triggerStateChange();

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
        this->analysis_mode->setChecked(true);
        this->modeController->onActivateAnalysisMode();
        //this->pbEngineOnOff->setText("ON");
        //this->pbEngineOnOff->setChecked(true);
    } else {
        this->enter_moves->setChecked(true);
        this->modeController->onActivateEnterMovesMode();
        //this->pbEngineOnOff->setText("OFF");
        //this->pbEngineOnOff->setStyleSheet("background-color: red");
        //this->pbEngineOnOff->setChecked(false);
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
    QString white = this->gameModel->getGame()->headers->value("White");
    QString black = this->gameModel->getGame()->headers->value("Black");
    QString site = this->gameModel->getGame()->headers->value("Site");
    QString date = this->gameModel->getGame()->headers->value("Date");
    QString line1 = QString("<b>").append(white).append(QString(" - ")).append(black).append(QString("</b><br/>"));
    QString line2 = site.append(QString(" ")).append(date);
    this->name->setText(line1.append(line2));
    if(this->gameModel->wasSaved) {
        this->save_game->setEnabled(true);
    } else {
        this->save_game->setEnabled(false);
    }
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
    chess::EcoInfo* ei = this->gameModel->getGame()->getEcoInfo();
    QString code = ei->code;
    QString info = ei->info;
    this->statusBar()->showMessage(code.append(" ").append(info));
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

