#include "messagebox.h"
#include <QMessageBox>

MessageBox::MessageBox()
{
}

void MessageBox::showMessage(QString title, QString content) {

    QMessageBox *msgBox = new QMessageBox();
    msgBox->setWindowTitle("Jerry");
    msgBox->setIcon(QMessageBox::Information);
    msgBox->setText(title+"\n"+content);
    msgBox->exec();
    delete msgBox;

}

