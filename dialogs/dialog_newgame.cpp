/* Jerry - A Chess Graphical User Interface
 * Copyright (C) 2014-2016 Dominik Klein
 * Copyright (C) 2015-2016 Karl Josef Klein
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

#include <QButtonGroup>
#include <QHBoxLayout>
#include <QFontMetrics>
#include <QDialogButtonBox>
#include <QDebug>
#include <assert.h>
#include "dialog_newgame.h"

DialogNewGame::DialogNewGame(bool customEngine, int currentStrength, int currentThinkTime, QWidget *parent) :
    QDialog(parent)
{

    this->customEngine = customEngine;

    this->setWindowTitle(this->tr("New Game"));

    this->rbEntersMoves = new QRadioButton(this->tr("Just Enter Moves"));
    this->rbPlaysComputer = new QRadioButton(this->tr("Computer"));

    this->rbEntersMoves->setChecked(true);

    QLabel* lblChooseEnemy = new QLabel(this->tr("Select Enemy:"));

    QButtonGroup *btnGroupEnemy = new QButtonGroup(this);
    btnGroupEnemy->addButton(this->rbEntersMoves);
    btnGroupEnemy->addButton(this->rbPlaysComputer);

    lblChooseEnemy->setAlignment(Qt::AlignBottom);

    QHBoxLayout *hboxEnemy = new QHBoxLayout();
    hboxEnemy->addWidget(this->rbEntersMoves);
    hboxEnemy->addWidget(this->rbPlaysComputer);
    hboxEnemy->setAlignment(Qt::AlignLeft);

    this->rbPlaysWhite = new QRadioButton(this->tr("White"));
    this->rbPlaysBlack = new QRadioButton(this->tr("Black"));

    this->rbPlaysWhite->setChecked(true);

    QLabel* lblChooseSide = new QLabel(this->tr("Choose your side:"));

    QButtonGroup *btnGroupSide = new QButtonGroup(this);
    btnGroupSide->addButton(this->rbPlaysWhite);
    btnGroupSide->addButton(this->rbPlaysBlack);

    lblChooseSide->setAlignment(Qt::AlignBottom);

    QHBoxLayout *hboxSide = new QHBoxLayout();
    hboxSide->addWidget(this->rbPlaysWhite);
    hboxSide->addWidget(this->rbPlaysBlack);
    hboxSide->setAlignment(Qt::AlignLeft);

    QLabel* lblStrength = new QLabel(this->tr("Computer Strength (internal engine only)"));
    lblStrength->setAlignment(Qt::AlignBottom);
    this->lblStrengthValue = new QLabel(tr("Level ").append(QString::number(currentStrength)));

    QHBoxLayout *hboxSliderStrength = new QHBoxLayout();
    this->sliderStrength = new QSlider(Qt::Horizontal, this);
    this->sliderStrength->setRange(0,20);
    this->sliderStrength->setPageStep(1);
    this->sliderStrength->setValue(currentStrength);

    hboxSliderStrength->addWidget(this->sliderStrength);
    hboxSliderStrength->addWidget(this->lblStrengthValue);

    QLabel *lblThink = new QLabel(this->tr("Computer's Time per Move"));
    lblThink->setAlignment(Qt::AlignBottom);
    this->lblThinkTime = new QLabel(QString::number(currentThinkTime).append(" ").append(tr("sec(s)")));
    QFontMetrics f = this->fontMetrics();
    int l = f.width(this->tr("20 sec(s)"));
    this->lblThinkTime->setFixedWidth(l);

    QHBoxLayout* hboxSliderThink = new QHBoxLayout();
    this->sliderThinkTime = new QSlider(Qt::Horizontal, this);
    this->sliderThinkTime->setRange(1,7); // 1, 2, 3, 5, 10, 15, 30
    this->sliderThinkTime->setPageStep(1);
    this->sliderThinkTime->setValue(this->thinkTimeToIntervalVal(currentThinkTime));

    hboxSliderThink->addWidget(this->sliderThinkTime);
    hboxSliderThink->addWidget(this->lblThinkTime);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok | QDialogButtonBox::Cancel);

    QVBoxLayout *layout = new QVBoxLayout();
    layout->addWidget(lblChooseEnemy);
    layout->addLayout(hboxEnemy);
    layout->addSpacing(20);
    layout->addWidget(lblChooseSide);
    layout->addLayout(hboxSide);
    layout->addSpacing(20);
    layout->addWidget(lblStrength);
    layout->addLayout(hboxSliderStrength);
    layout->addSpacing(20);
    layout->addWidget(lblThink);
    layout->addLayout(hboxSliderThink);
    layout->addSpacing(20);

    layout->addWidget(buttonBox);
    this->setLayout(layout);

    this->computerStrength = currentStrength;
    this->computerThinkTime = currentThinkTime;

    int widgetWidthEstimate = f.width(this->tr("Computer's Time per Move")) * 2;
    this->resize(widgetWidthEstimate, this->height());

    if(customEngine) {
        this->sliderStrength->setValue(20);
        this->lblStrengthValue->setText(tr("Max"));
    }
    this->sliderStrength->setEnabled(false);
    this->sliderThinkTime->setEnabled(false);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogNewGame::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogNewGame::reject);

    connect(rbPlaysComputer, &QRadioButton::toggled, this, &DialogNewGame::rbPlaysComputerToggled);
    connect(rbEntersMoves, &QRadioButton::toggled, this, &DialogNewGame::rbEntersMovesToggled);
    connect(rbPlaysWhite, &QRadioButton::toggled, this, &DialogNewGame::rbPlaysWhiteToggled);
    connect(rbPlaysBlack, &QRadioButton::toggled, this, &DialogNewGame::rbPlaysBlackToggled);
    connect(sliderStrength, &QSlider::valueChanged, this, &DialogNewGame::sliderStrengthChanged);
    connect(sliderThinkTime, &QSlider::valueChanged, this, &DialogNewGame::sliderThinkTimeChanged);

    this->playsWhite = true;
    this->playsComputer = false;
/*
    if(gamestate):
        think_time = gamestate.computer_think_time // 1000
        if(think_time == 30):
            think_time = 7
        elif(think_time == 15):
            think_time = 6
        elif(think_time == 10):
            think_time = 5
        elif(think_time == 5):
            think_time = 4
        strength = gamestate.strength_level
        self.slider_think.setValue(think_time)
        self.think_time = think_time
        self.slider_elo.setValue(strength)
        self.strength = strength
        self.set_lbl_elo_value(strength)
        self.set_lbl_think_value(think_time)

    # if a custom engine is used, we can't set the
    # strength level. set the slider to max an deactivate
    # slider
    if(user_settings != None and (not user_settings.active_engine == user_settings.engines[0])):
        self.slider_elo.setValue(20)
        self.set_lbl_elo_value(20)
        self.slider_elo.setEnabled(False)
*/

    /*
    self.slider_elo.valueChanged.connect(self.set_lbl_elo_value)
    self.slider_think.valueChanged.connect(self.set_lbl_think_value)
    self.connect(buttonBox, SIGNAL("accepted()"),self, SLOT("accept()"))
    self.connect(buttonBox, SIGNAL("rejected()"),self, SLOT("reject()"))
    self.resize(370, 150)
*/

}

int DialogNewGame::intervalValueToThinkTime(int intervalVal) {
    assert(intervalVal >= 1 && intervalVal <= 7);
    if(intervalVal <= 3) {
        return intervalVal;
    } else {
        if(intervalVal == 4) {
            return 5;
        } else if(intervalVal == 5) {
            return 10;
        } else if(intervalVal == 6) {
            return 15;
        } else {
            return 30;
        }
    }
}

int DialogNewGame::thinkTimeToIntervalVal(int thinkTime) {
    assert(thinkTime == 1 || thinkTime == 2 || thinkTime == 3 || thinkTime == 5
           || thinkTime == 10 || thinkTime == 15 || thinkTime == 30);
    if(thinkTime <= 3) {
        return thinkTime;
    } else {
        if(thinkTime == 5) {
            return 4;
        } else if(thinkTime == 10) {
            return 5;
        } else if(thinkTime == 15) {
            return 6;
        } else {
            return 7;
        }
    }
}

void DialogNewGame::rbPlaysComputerToggled() {
    if(!this->customEngine) {
        this->sliderStrength->setEnabled(true);
    }
    this->sliderThinkTime->setEnabled(true);
    this->playsComputer = true;
}

void DialogNewGame::rbEntersMovesToggled() {
    this->sliderStrength->setEnabled(false);
    this->sliderThinkTime->setEnabled(false);
    this->playsComputer = false;
}

void DialogNewGame::rbPlaysWhiteToggled() {
    this->playsWhite = true;
}

void DialogNewGame::rbPlaysBlackToggled() {
    this->playsWhite = false;
}

void DialogNewGame::sliderStrengthChanged() {
    this->computerStrength = this->sliderStrength->value();
    this->lblStrengthValue->setText(tr("Level ").append(QString::number(this->computerStrength)));
}

void DialogNewGame::sliderThinkTimeChanged() {
    this->computerThinkTime = this->intervalValueToThinkTime(this->sliderThinkTime->value());
    this->lblThinkTime->setText(QString::number(this->computerThinkTime).append(" ").append(tr("sec(s)")));
}
