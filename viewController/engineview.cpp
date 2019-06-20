#include <QTextEdit>
#include "engineview.h"
#include "uci/engine_info.h"
#include <QDebug>


EngineView::EngineView(GameModel *gameModel, QWidget *parent) :
    QTextEdit(parent)
{
    this->lastInfo = QString("");
    this->gameModel = gameModel;

    QFontMetrics f = this->fontMetrics();
    int minH = int((f.height()) * 3.1);
    this->setMinimumHeight(minH);
    this->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);

}

void EngineView::onStateChange() {
    QString size = this->gameModel->fontStyle->engineOutFontSize;
    QFont f;
    if(!size.isEmpty()) {
        if(!size.isEmpty()) {
            f.setPointSize(size.toInt());
        }
        this->setFont(f);
    } else {
        f.setFamily(f.defaultFamily());
        this->setFont(f);
    }
}

void EngineView::onNewInfo(QString info) {
    this->lastInfo = info;
    if(this->gameModel->showEval) {   
        this->setText(info);
    }
}

void EngineView::flipShowEval() {

    this->gameModel->showEval = !this->gameModel->showEval;
    this->setText("");
    this->onNewInfo(this->lastInfo);
}
