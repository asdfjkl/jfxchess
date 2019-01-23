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
#include <QTest>

DialogDatabase::DialogDatabase(GameModel *gameModel, QWidget* parent) :
    QDialog(parent)
{
    this->gameModel = gameModel;

    this->selectedIndex = -1;

    this->currentOpenDBType = -1;

    this->resizeTo(0.9);

    QToolBar *toolbar = new QToolBar(this);
    QSize iconSize = toolbar->iconSize() * this->devicePixelRatio()*2;

    QString resDir = ResourceFinder::getPath();

    QString stringNew(resDir + "/res/icons/document-save.svg");
    QPixmap *tbNew = Helper::fromSvgToPixmap(iconSize,stringNew, this->devicePixelRatio());
    QAction *tbActionSave = toolbar->addAction(QIcon(*tbNew), this->tr("New"));
    toolbar->addSeparator();

    QString stringOpen(resDir + "/res/icons/document-open.svg");
    QPixmap *tbOpen = Helper::fromSvgToPixmap(iconSize,stringOpen, this->devicePixelRatio());
    QAction *tbActionOpen = toolbar->addAction(QIcon(*tbNew), this->tr("Open"));

    QString stringSearch(resDir + "/res/icons/system-search.svg");
    QPixmap *tbSearch = Helper::fromSvgToPixmap(iconSize,stringSearch, this->devicePixelRatio());
    QAction *tbActionSearch = toolbar->addAction(QIcon(*tbSearch), this->tr("Search"));

    toolbar->addSeparator();

    QString stringAddCurrent(resDir + "/res/icons/text-x-generic_with_pencil.svg");
    QPixmap *tbAddCurrent = Helper::fromSvgToPixmap(iconSize,stringAddCurrent, this->devicePixelRatio());
    QAction *tbActionAddCurrent = toolbar->addAction(QIcon(*tbAddCurrent), this->tr("Add Current Game"));

    QWidget* spacer = new QWidget(this);
    spacer->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
    toolbar->addWidget(spacer);

    QString stringHelp(resDir + "/res/icons/help-browser.svg");
    QPixmap *tbHelp = Helper::fromSvgToPixmap(iconSize, stringHelp, this->devicePixelRatio());
    QAction *tbActionHelp = toolbar->addAction(QIcon(*tbHelp), this->tr("About"));

    toolbar->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);
    toolbar->setIconSize(iconSize);


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
    this->gameTable->resizeColumnsToContents();;
    this->gameTable->horizontalHeader()->setStretchLastSection(true);
    this->gameTable->selectRow(0);


    this->indexModel = new DatabaseIndexModel(this);
    this->indexModel->setDatabase(&this->gameModel->PgnDatabase);

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

    int idx = 0; // this->gameModel->PgnDatabase->currentOpenGameIdx;
    if(idx > 0) {
        this->tableView->selectRow(idx);
    } else {
        this->tableView->selectRow(0);
    }

    tableView->setWindowTitle(QObject::tr("Games"));
    //tableView->resizeColumnsToContents();  //don't resize, instead set to stretch (see above)
    tableView->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
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
    //connect(tbActionSearch, &QAction::triggered, this, &DialogDatabase::onClickSearch);
    //connect(tbActionExport, &QAction::triggered, this, &DialogDatabase::onClickExport);

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

void DialogDatabase::onClickSearch() {

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
            this->gameModel->PgnDatabase.setParentWidget(this);
            //this->gameModel->dciDatabase->reset();
            //qDebug() << "dialog 2";
            this->gameModel->PgnDatabase.open(filename);
            //qDebug() << "dialog 3";
            this->indexModel->setDatabase(&this->gameModel->PgnDatabase);
            //qDebug() << "dialog 4";
            this->indexModel->layoutChanged();
            //qDebug() << "dialog 5";
            //this->tableView->resizeColumnsToContents();

            if(this->gameModel->PgnDatabase.countGames() > 0) {
                this->tableView->selectRow(0);
            }
            //this->setWindowTitle(this->gameModel->dciDatabase->getFilename());
            //this->currentOpenDBType = DATABASE_TYPE_DCI;

            //this->tbActionDeleteGame->setDisabled(true);

        }
    }

    //this->gameModel->database->open(this);


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

    qDebug() << "dialog: on row changed";
    QTest::qWait(2000);
    QItemSelectionModel *select = this->tableView->selectionModel();
    if(select->hasSelection()) {
        QModelIndexList selected_rows = select->selectedRows();
        if(selected_rows.size() > 0) {
            int row_index = selected_rows.at(0).row();
            this->selectedIndex = row_index;
        }
    }
}
