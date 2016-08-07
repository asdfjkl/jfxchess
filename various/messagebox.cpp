#include "messagebox.h"
#include <QMessageBox>

MessageBox::MessageBox(QWidget *parent)
{
    this->parent = parent;
}

void MessageBox::showMessage( QString title, QString content) {

    QMessageBox *msgBox = new QMessageBox(this->parent);
    msgBox->setWindowTitle("Jerry");
    msgBox->setIcon(QMessageBox::Information);
    msgBox->setText(title+"\n"+content);
    msgBox->exec();
    delete msgBox;

}

