#include "dialog_browseheaders.h"
#include <QHBoxLayout>
#include <QVBoxLayout>
#include <QDialogButtonBox>
#include <QHeaderView>
#include <QDebug>
#include <QDesktopWidget>
#include <QApplication>

DialogBrowseHeaders::DialogBrowseHeaders(QList<chess::HeaderOffset> *header_offsets,
                                         QString &filename, QWidget *parent) :
    QDialog(parent), header_offsets(header_offsets)
{

    this->resizeTo(0.7);

    this->header_offsets = header_offsets;
    this->setWindowTitle(filename);

    this->gameOffset = 0;
    this->gameIdx = 0;

    int rows = header_offsets->size();
    int columns = 7;
    this->table = new QTableWidget(rows, columns, this);

    this->table->setEditTriggers(QAbstractItemView::NoEditTriggers);
    this->table->setSelectionBehavior(QAbstractItemView::SelectRows);
    this->table->setSelectionMode(QAbstractItemView::SingleSelection);

    this->tableHeaders = QStringList();
    tableHeaders.append("No.");
    tableHeaders.append("White");
    tableHeaders.append("Black");
    tableHeaders.append("Result");
    tableHeaders.append("Date");
    tableHeaders.append("ECO");
    tableHeaders.append("Site");

    this->table->verticalHeader()->hide();
    this->table->setShowGrid(false);

    this->drawAllItems();
    this->table->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);

    this->table->resizeColumnsToContents();;
    //this->table->horizontalHeader()->setStretchLastSection(true);
    this->table->selectRow(0);

/*
    f = this->fontMetrics();
    rec = QApplication.desktop().screenGeometry()
    self.resize(min(650,rec.width()-100),min(rows*20+130,rec.height()-200))
*/

    this->searchField = new QLineEdit(this);
    this->btnSearch = new QPushButton(tr("Search"), this);
    this->btnReset = new QPushButton(tr("Reset"), this);

    QHBoxLayout *hbox_lbl = new QHBoxLayout();
    hbox_lbl->addWidget(this->searchField);
    hbox_lbl->addSpacing(20);
    //hbox_lbl->addSpacerItem(new QSpacerItem(20, 1));
    hbox_lbl->addWidget(this->btnSearch);
    hbox_lbl->addWidget(this->btnReset);

    QVBoxLayout *vbox = new QVBoxLayout();
    vbox->addLayout(hbox_lbl);
    vbox->addWidget(this->table);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(Qt::Horizontal, this);
    buttonBox->addButton(QDialogButtonBox::Ok);
    buttonBox->addButton(QDialogButtonBox::Cancel);

    vbox->addWidget(buttonBox);

    this->setLayout(vbox);

    this->searchField->setFocus();

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogBrowseHeaders::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogBrowseHeaders::reject);
    //self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
    //self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
    //self.button_search.clicked.connect(self.on_search)
    //this->button_reset.clicked.connect(self.draw_all_items)

    int dialogWidth = this->table->horizontalHeader()->length() * 1.1;
    //dialogHeight= tw.verticalHeader().length()   + 24
            this->setFixedWidth(dialogWidth);
    //w.setFixedSize(dialogWidth, dialogHeight)
    connect(this->table, &QTableWidget::itemSelectionChanged, this, &DialogBrowseHeaders::onItemSelectionChanged);

    connect(this->btnSearch, &QPushButton::clicked, this, &DialogBrowseHeaders::onSearch);
    connect(this->btnReset, &QPushButton::clicked, this, &DialogBrowseHeaders::onResetSearch);

}

void DialogBrowseHeaders::resizeTo(float ratio) {

    int height = 0;
    int width = 0;
    if(this->parentWidget() != 0) {
        int w_height = this->parentWidget()->size().height();
        height = w_height * ratio;
        //width = (w_width * ratio);
        width = height * 1.1;
    } else {
        QDesktopWidget *desktop = qApp->desktop();
        QSize availableSize = desktop->availableGeometry().size();
        int w_height = availableSize.height();
        height = w_height * (ratio*0.6);
        //width = w_width * (ratio*0.6);
        width = height * 1.1;
    }
    QSize newSize( width, height );
    this->resize(newSize);
}

void DialogBrowseHeaders::drawAllItems() {
    this->table->clear();
    this->table->setHorizontalHeaderLabels(this->tableHeaders);
    this->table->setRowCount(this->header_offsets->size());
    for(int i=0;i<this->header_offsets->size();i++) {

        QString white = this->header_offsets->at(i).headers.value("White");
        QString black = this->header_offsets->at(i).headers.value("Black");
        QString result = this->header_offsets->at(i).headers.value("Result");
        QString date = this->header_offsets->at(i).headers.value("Date");
        QString eco = this->header_offsets->at(i).headers.value("ECO");
        QString site = this->header_offsets->at(i).headers.value("Site");
        this->table->setItem(i,0,new QTableWidgetItem(QString::number(i+1)));
        this->table->setItem(i,1,new QTableWidgetItem(white));
        this->table->setItem(i,2,new QTableWidgetItem(black));
        this->table->setItem(i,3,new QTableWidgetItem(result));
        this->table->setItem(i,4,new QTableWidgetItem(date));
        this->table->setItem(i,5,new QTableWidgetItem(eco));
        this->table->setItem(i,6,new QTableWidgetItem(site));

    }
    this->table->resizeColumnsToContents();
}

void DialogBrowseHeaders::onResetSearch() {
    this->drawAllItems();
    this->table->selectRow(0);
}

void DialogBrowseHeaders::onSearch() {
    this->table->clear();
    this->table->clearContents();
    QString searchTerm = this->searchField->text().toLower();
    QRegularExpression searchRegExp = QRegularExpression(searchTerm);
    int idx = 0;
    for(int i=0;i<this->header_offsets->size();i++) {
        QString white = this->header_offsets->at(i).headers.value("White");
        QString black = this->header_offsets->at(i).headers.value("Black");
        QString result = this->header_offsets->at(i).headers.value("Result");
        QString date = this->header_offsets->at(i).headers.value("Date");
        QString eco = this->header_offsets->at(i).headers.value("ECO");
        QString site = this->header_offsets->at(i).headers.value("Site");

        QRegularExpressionMatch m_white = searchRegExp.match(white.toLower());
        QRegularExpressionMatch m_black = searchRegExp.match(black.toLower());
        QRegularExpressionMatch m_result = searchRegExp.match(result);
        QRegularExpressionMatch m_date = searchRegExp.match(date);
        QRegularExpressionMatch m_eco = searchRegExp.match(eco.toLower());
        QRegularExpressionMatch m_site = searchRegExp.match(site.toLower());

        if(m_white.hasMatch() || m_black.hasMatch() || m_result.hasMatch() ||
                m_date.hasMatch() || m_eco.hasMatch() || m_site.hasMatch()) {
            this->table->setItem(idx,0,new QTableWidgetItem(QString::number(i+1)));
            this->table->setItem(idx,1,new QTableWidgetItem(white));
            this->table->setItem(idx,2,new QTableWidgetItem(black));
            this->table->setItem(idx,3,new QTableWidgetItem(result));
            this->table->setItem(idx,4,new QTableWidgetItem(date));
            this->table->setItem(idx,5,new QTableWidgetItem(eco));
            this->table->setItem(idx,6,new QTableWidgetItem(site));
            idx++;
        }
    }
    // if nothing contained, then reset to first game
    if(this->table->rowCount() == 0) {
        this->gameOffset = 0;
        this->gameIdx = 0;
    }
    this->table->setRowCount(idx);
}

void DialogBrowseHeaders::onItemSelectionChanged() {
    QList<QTableWidgetItem *> selectedItems = this->table->selectedItems();
    if(selectedItems.size() > 0) {
        int idx = (selectedItems.at(0)->text()).toInt() - 1;
        this->gameOffset = this->header_offsets->at(idx).offset;
        this->gameIdx = idx;
    }
}
