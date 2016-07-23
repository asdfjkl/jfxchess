#ifndef ON_OFF_BUTTON_H
#define ON_OFF_BUTTON_H

#include <QPushButton>

class OnOffButton : public QPushButton
{
    Q_OBJECT
public:
    explicit OnOffButton(QWidget* parent = 0);

protected:
    void paintEvent(QPaintEvent* paint);

signals:

public slots:

};

#endif // ON_OFF_BUTTON_H
