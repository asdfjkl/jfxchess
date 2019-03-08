#include "ribbon_button.h"
#include "various/stylesheet.h"
#include <QDebug>
#include <QFontMetrics>
#include <algorithm>

RibbonButton::RibbonButton(QAction *action, bool isLargeIcon, QWidget *parent) :
    QToolButton (parent)
{
    qreal ratio = this->devicePixelRatio();

    this->actionOwner = action;
    this->onActionChanged();

    connect(this, &RibbonButton::clicked, this->actionOwner, &QAction::trigger);
    connect(this->actionOwner, &QAction::changed, this, &RibbonButton::onActionChanged);

    if(isLargeIcon) {
        this->setMaximumWidth(80*ratio);
        this->setMinimumWidth(50*ratio);
        this->setMinimumHeight(75*ratio);
        this->setMaximumHeight(80*ratio);
        QString strRibbonButton("ribbonButton");
        QString style = StyleSheet::getInstance().getStylesheet(strRibbonButton);
        this->setStyleSheet(style);
        this->setToolButtonStyle(Qt::ToolButtonTextUnderIcon);
        this->setIconSize(QSize(ICON_SIZE_LARGE * ratio, ICON_SIZE_LARGE*ratio));
    } else {
        this->setToolButtonStyle(Qt::ToolButtonTextBesideIcon);
        this->setMaximumWidth(120*ratio);
        this->setIconSize(QSize(ICON_SIZE_SMALL*ratio, ICON_SIZE_SMALL*ratio));        
        QString strRibbonSmallButton("ribbonSmallButton");
        this->setStyleSheet(StyleSheet::getInstance().getStylesheet(strRibbonSmallButton));
    }
}



void RibbonButton::onActionChanged() {

    this->setText(this->actionOwner->text());
    this->setStatusTip(this->statusTip());
    this->setToolTip(this->actionOwner->toolTip());
    this->setIcon(this->actionOwner->icon());
    this->setEnabled(this->actionOwner->isEnabled());
    this->setCheckable((this->actionOwner->isCheckable()));
    this->setChecked(this->actionOwner->isChecked());

}
