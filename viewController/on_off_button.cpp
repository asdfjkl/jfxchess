#include "on_off_button.h"

OnOffButton::OnOffButton(QWidget* parent) :
    QPushButton(parent)
{
    this->setCheckable(true);
    QFontMetrics f = this->fontMetrics();
    int l = f.width(this->tr("xxOFFxx"));
    this->setFixedWidth(l);
}


void OnOffButton::paintEvent(QPaintEvent* paint) {
    if(this->isChecked()) {
        this->setText("ON");
    } else {
        this->setText("OFF");
    }
    QPushButton::paintEvent(paint);
}
