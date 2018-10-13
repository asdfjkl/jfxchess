#ifndef TABCOMMENTSEARCH_H
#define TABCOMMENTSEARCH_H

#include <QWidget>
#include <QCheckBox>
#include <QLineEdit>

class TabCommentSearch : public QWidget
{
    Q_OBJECT
public:
    explicit TabCommentSearch(QWidget *parent = 0);

    QLineEdit* text1;
    QLineEdit* text2;

    QCheckBox *wholeWord;
    QCheckBox *caseSensitive;
    QCheckBox *notInitialPos;

    //QCheckBox *arrows;
    //QCheckBox *colorFields;

signals:

public slots:
};

#endif // TABCOMMENTSEARCH_H
