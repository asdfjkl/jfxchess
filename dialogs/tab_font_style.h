#ifndef TAB_FONT_STYLE_H
#define TAB_FONT_STYLE_H

#include <QWidget>
#include <QRadioButton>
#include <QFontComboBox>
#include <QComboBox>
#include "model/font_style.h"

class TabFontStyle : public QWidget
{
    Q_OBJECT
public:
    explicit TabFontStyle(FontStyle *fontStyle, QWidget *parent = nullptr);

private:
    QRadioButton *radioGameNotationDefaultSize;
    QRadioButton *radioGameNotationcustomSize;
    QComboBox *sizeBoxGameNotation;

    QRadioButton *radioEngineOutDefaultSize;
    QRadioButton *radioEngineOutcustomSize;
    QComboBox *sizeBoxEngineOut;

    FontStyle *fontStyle;

signals:

public slots:

    void onSelectDefaultGameNotationFontSize();
    void onSelectCustomGameNotationFontSize();
    void onSizeBoxGameNotationChange(const QString &text);

    void onSelectDefaultEngineOutFontSize();
    void onSelectCustomEngineOutFontSize();
    void onSizeBoxEngineOutChange(const QString &text);

};

#endif // TAB_FONT_STYLE_H
