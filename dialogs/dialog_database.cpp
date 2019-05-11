#include "dialog_database.h"
#include <QDesktopWidget>
#include <QApplication>
#include <QPushButton>
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QToolBar>
#include <QDialogButtonBox>
#include <QHeaderView>
#include <QAction>
#include "various/resource_finder.h"
#include "various/helper.h"
#include "viewController/database_index_model.h"
#include <QFileDialog>
#include <QDebug>
//#include <QTest>
#include <iostream>
#include "various/messagebox.h"
#include "dialogs/dialog_database_help.h"
#include "dialogs/dialog_search.h"

DialogDatabase::DialogDatabase(GameModel *gameModel, QWidget* parent) :
    QDialog(parent) {
    this->gameModel = gameModel;

    this->selectedIndex = -1;

    this->currentOpenDBType = -1;

    this->resizeTo(0.9);

    QToolBar *toolbar = new QToolBar(this);
    //QSize iconSize = toolbar->iconSize() * this->devicePixelRatio();
    //toolbar->setIconSize(iconSize);


    QAction *tbActionNew = toolbar->addAction(QIcon(":/res/icons/document-new.svg"), this->tr("New"));
    QAction *tbActionOpen = toolbar->addAction(QIcon(":/res/icons/document-open.svg"), this->tr("Open"));

    toolbar->addSeparator();

    QAction *tbActionSearch = toolbar->addAction(QIcon(":/res/icons/system-search.svg"), this->tr("Search"));

    QAction *tbActionReset = toolbar->addAction(QIcon(":/res/icons/view-refresh.svg"), this->tr("Reset Search"));

    toolbar->addSeparator();

    QAction *tbActionAddCurrent = toolbar->addAction(QIcon(":/res/icons/text-pencil.svg"), this->tr("Add Current Game"));

    QWidget* spacer = new QWidget(this);
    spacer->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    toolbar->addWidget(spacer);

    QAction *tbActionHelp = toolbar->addAction(QIcon(":/res/icons/help-browser.svg"), this->tr("About"));

    toolbar->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);



    int rows = 10;
    int columns = 7;
    this->gameTable = new QTableWidget(rows, columns);
    this->gameTable->setEditTriggers(QAbstractItemView::NoEditTriggers);
    this->gameTable->setSelectionBehavior(QAbstractItemView::SelectRows);
    this->gameTable->setSelectionMode(QAbstractItemView::SingleSelection);
    this->gameTableHeaders = new QStringList();
    this->gameTableHeaders->append("No.");
    this->gameTableHeaders->append("White");
    this->gameTableHeaders->append("Black");
    this->gameTableHeaders->append("Result");
    this->gameTableHeaders->append("Date");
    this->gameTableHeaders->append("ECO");
    this->gameTableHeaders->append("Site");
    this->gameTable->verticalHeader()->hide();
    this->gameTable->setShowGrid(false);
    //this->drawAllItems();
    //this->gameTable->resizeColumnsToContents();;
    this->gameTable->horizontalHeader()->setStretchLastSection(true);
    this->gameTable->horizontalHeader()->setSectionResizeMode(QHeaderView::ResizeToContents);

    this->indexModel = new DatabaseIndexModel(this);
    this->indexModel->setDatabase(&this->gameModel->database);

    this->tableView = new QTableView(this);
    this->tableView->setModel(indexModel);

    this->tableView->setEditTriggers(QAbstractItemView::NoEditTriggers);
    this->tableView->setSelectionBehavior(QAbstractItemView::SelectRows);
    this->tableView->setSelectionMode(QAbstractItemView::SingleSelection);
    this->tableView->verticalHeader()->hide();
    //myTableWidget->verticalHeader()->setVisible(false);
    this->tableView->setShowGrid(false);
    this->tableView->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
    this->tableView->horizontalHeader()->setDefaultAlignment(Qt::AlignLeft);

    // set index to currently open game
    // if a game is currently opened
    int idx = this->gameModel->database.getLastSelectedIndex();
    if(idx > 0) {
        this->tableView->selectRow(idx);
    } else {
        this->tableView->selectRow(0);
    }

    tableView->setWindowTitle(QObject::tr("Games"));
    //tableView->resizeColumnsToContents();  //don't resize, instead set to stretch (see above)
    tableView->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
    //tableView->horizontalHeader()->setSectionResizeMode(QHeaderView::ResizeToContents);
    tableView->show();

    QDialogButtonBox *buttonBox = new QDialogButtonBox(Qt::Horizontal, this);

    this->btnOpenGame = new QPushButton(this);
    this->btnCancel = new QPushButton(this);

    this->btnOpenGame->setText(this->tr("Open Game"));
    this->btnCancel->setText(this->tr("Cancel"));

    buttonBox->addButton(btnOpenGame, QDialogButtonBox::AcceptRole);
    buttonBox->addButton(btnCancel, QDialogButtonBox::RejectRole);

    // putting it all together

    QVBoxLayout *layoutAll = new QVBoxLayout(this);
    layoutAll->addWidget(toolbar);
    layoutAll->addWidget(this->tableView);

    //layoutAll->addStretch();

    layoutAll->addWidget(buttonBox);

    this->setLayout(layoutAll);

    connect(tbActionOpen, &QAction::triggered, this, &DialogDatabase::onClickOpen);
    connect(tbActionNew, &QAction::triggered, this, &DialogDatabase::onClickNew);
    connect(tbActionAddCurrent, &QAction::triggered, this, &DialogDatabase::onClickAppend);

    connect(tbActionHelp, &QAction::triggered, this, &DialogDatabase::showHelp);

    connect(tbActionSearch, &QAction::triggered, this, &DialogDatabase::onClickSearch);
    connect(tbActionReset, &QAction::triggered, this, &DialogDatabase::onClickReset);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &QDialog::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &QDialog::reject);

    QItemSelectionModel *model = this->tableView->selectionModel();
    connect(model, &QItemSelectionModel::selectionChanged,
            this, &DialogDatabase::onRowChanged);

}

void DialogDatabase::resizeTo(float ratio) {

    int height = 0;
    int width = 0;
    if(this->parentWidget() != 0) {
        int w_height = this->parentWidget()->size().height();
        height = w_height * ratio;
        //width = (w_width * ratio);
        width = height * 1.8;
    } else {
        QDesktopWidget *desktop = qApp->desktop();
        QSize availableSize = desktop->availableGeometry().size();
        int w_height = availableSize.height();
        height = w_height * (ratio*0.8);
        //width = w_width * (ratio*0.6);
        width = height * 1.8;
    }
    QSize newSize( width, height );
    this->resize(newSize);
}

void DialogDatabase::onClickReset() {
    this->gameModel->database.resetSearch();
    this->indexModel->layoutChanged();
}

void DialogDatabase::onClickSearch() {

    DialogSearch dlg(this->gameModel, this);
    dlg.setPattern(this->gameModel->lastSeenSearchPattern);
    if(dlg.exec() == QDialog::Accepted) {
        SearchPattern pattern = dlg.getPattern();
        this->gameModel->lastSeenSearchPattern = pattern;
        //QTime myTimer;
        //myTimer.start();
        // do something..
        try {
            this->gameModel->database.search(pattern);
            //int nMilliseconds = myTimer.elapsed();
            this->gameModel->database.setLastSelectedIndex(0);
            if(this->gameModel->database.getRowCount() > 0) {
                this->tableView->selectRow(0);
            }
            this->indexModel->layoutChanged();
        } catch(std::invalid_argument e) {
            std::cerr << e.what() << std::endl;
        }
    }
}

void DialogDatabase::showHelp() {
    DialogDatabaseHelp dlg(this, JERRY_VERSION);
    dlg.exec();
}

void DialogDatabase::onClickNew() {
    QString filename = QFileDialog::getSaveFileName(this,
                       tr("Create New Database..."), this->gameModel->lastSaveDir, tr("PGN Files (*.pgn)"));
    if(!filename.isNull()) {
        QDir dir = QDir::root();
        QString path = dir.absoluteFilePath(filename);
        if(this->gameModel->database.createNew(filename) < 0) {
            MessageBox msg(this);
            msg.showMessage(tr("Operation Failed"), tr("Unable to create File: ")+filename);
        } else {
            this->setWindowTitle(filename);
        }
    }
}

void DialogDatabase::onClickAppend() {
    if(this->gameModel->database.isOpen()) {
        chess::Game *currentGame = this->gameModel->getGame();
        if(this->gameModel->database.appendCurrentGame(*currentGame) < 0) {
            MessageBox msg(this);
            msg.showMessage(tr("Operation Failed"), tr("Unable append current Game"));
        } else {
            // simpley workaround. actually not layout changes
            // but dataChanged() should be emitted, however
            // dataChanged() requires to determine the precise index,
            // and layoutChanged suffices to ensure that the freshly
            // added game is displayed
            this->tableView->selectRow(this->gameModel->database.countGames());
            this->indexModel->layoutChanged();
        }
    }
}

void DialogDatabase::onClickOpen() {

    QString filename = QFileDialog::getOpenFileName(this,
                       QApplication::tr("Open Database"),
                       this->gameModel->lastOpenDir,
                       QApplication::tr("*.pgn"));
    if(!filename.isNull()) {
        // todo: mor thoroughly check file type
        // i.e. look for magic bytes instead of
        // just relying on filename ending
        if(filename.endsWith(".pgn")) {
            //qDebug() << "dialog 1";
            this->gameModel->database.setParentWidget(this);
            //this->gameModel->dciDatabase->reset();
            //qDebug() << "dialog 2";
            this->gameModel->database.open(filename);
            //qDebug() << "dialog 3";
            this->indexModel->setDatabase(&this->gameModel->database);
            //qDebug() << "dialog 4";
            this->indexModel->layoutChanged();
            //qDebug() << "dialog 5";
            //this->tableView->resizeColumnsToContents();

            if(this->gameModel->database.countGames() > 0) {
                this->tableView->selectRow(0);
            }
            this->gameTable->resizeColumnsToContents();;

            //this->setWindowTitle(this->gameModel->dciDatabase->getFilename());
            //this->currentOpenDBType = DATABASE_TYPE_DCI;

            //this->tbActionDeleteGame->setDisabled(true);
            this->setWindowTitle(filename);
        }
    }

    //this->gameModel->database->open(this);

    this->gameTable->resizeColumnsToContents();;

}


void DialogDatabase::onClickExport() {
    /*
        DialogExportDatabase dlg;
        if(dlg.exec() == QDialog::Accepted) {
            qDebug() << "export to PGN " << dlg.radioFormatPgn->isChecked();

            QFileDialog dialog;
            dialog.setFileMode(QFileDialog::AnyFile);
            QString strFile = dialog.getSaveFileName(this, tr("New Database Filename"),"",tr("PGN Files (*.pgn)"));
            qDebug() << "saving to: " << strFile;
        }
        */
}

void DialogDatabase::onRowChanged() {

    //QTest::qWait(2000);
    QItemSelectionModel *select = this->tableView->selectionModel();
    if(select->hasSelection()) {
        QModelIndexList selected_rows = select->selectedRows();
        if(selected_rows.size() > 0) {
            int row_index = selected_rows.at(0).row();
            this->selectedIndex = row_index;
            this->gameModel->database.setLastSelectedIndex(this->selectedIndex);
        }
    }
    this->gameTable->resizeColumnsToContents();;
}
