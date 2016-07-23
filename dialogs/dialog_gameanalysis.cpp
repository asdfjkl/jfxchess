#include "dialog_gameanalysis.h"
#include <QSpinBox>
#include <QDoubleSpinBox>
#include <QFormLayout>
#include <QVBoxLayout>
#include <QDialogButtonBox>
#include <QLabel>
#include <QButtonGroup>

DialogGameanalysis::DialogGameanalysis(int secsPerMove, float threshold, QWidget *parent) :
    QDialog(parent)
{
    this->secsPerMove = secsPerMove;
    this->threshold = threshold;

    /*
    self.sb_secs = QSpinBox()
    self.sb_secs.setRange(1,30)
    self.sb_secs.setValue(3)
    self.sb_secs.setFixedWidth(l)

    self.sb_threshold = QDoubleSpinBox()
    self.sb_threshold.setRange(0.1,1.0)
    self.sb_threshold.setSingleStep(0.1)
    self.sb_threshold.setValue(0.5)
    self.sb_threshold.setFixedWidth(l)
    */

    QSpinBox *spinSecs = new QSpinBox(this);
    int initval = this->secsPerMove / 1000;
    if(initval < 1) {
        initval = 1;
    }
    spinSecs->setValue(initval);
    spinSecs->setRange(1,30);

    QDoubleSpinBox *spinThreshold = new QDoubleSpinBox(this);
    spinThreshold->setValue(this->threshold);
    spinThreshold->setRange(0.1,1.0);

    this->rbAnalyseBoth = new QRadioButton(this->tr("Both"));
    this->rbAnalyseWhite = new QRadioButton(this->tr("White"));
    this->rbAnalyseBlack = new QRadioButton(this->tr("Black"));

    this->rbAnalyseBoth->setChecked(true);

    QLabel* lblChoosePlayers = new QLabel(this->tr("Analyse Players:"));

    QButtonGroup *btnGroupPlayers = new QButtonGroup(this);
    btnGroupPlayers->addButton(this->rbAnalyseBoth);
    btnGroupPlayers->addButton(this->rbAnalyseWhite);
    btnGroupPlayers->addButton(this->rbAnalyseBlack);

    lblChoosePlayers->setAlignment(Qt::AlignBottom);

    QHBoxLayout *hboxPlayers = new QHBoxLayout();
    hboxPlayers->addWidget(this->rbAnalyseBoth);
    hboxPlayers->addWidget(this->rbAnalyseWhite);
    hboxPlayers->addWidget(this->rbAnalyseBlack);
    hboxPlayers->setAlignment(Qt::AlignLeft);

    QDialogButtonBox *buttonBox = new QDialogButtonBox(QDialogButtonBox::Ok| QDialogButtonBox::Cancel);

    connect(buttonBox, &QDialogButtonBox::accepted, this, &DialogGameanalysis::accept);
    connect(buttonBox, &QDialogButtonBox::rejected, this, &DialogGameanalysis::reject);

    QFormLayout *formLayout = new QFormLayout();
    formLayout->addRow(tr("Sec(s) per Move"), spinSecs);
    formLayout->addRow(tr("Threshold (in pawns)"), spinThreshold);

    QVBoxLayout *layout = new QVBoxLayout();
    layout->addLayout(formLayout);
    layout->addWidget(lblChoosePlayers);
    layout->addLayout(hboxPlayers);
    layout->addWidget(buttonBox);

    this->setWindowTitle(tr("Game Analysis"));

    connect(spinSecs, static_cast<void (QSpinBox::*)(int)>(&QSpinBox::valueChanged),
            this, &DialogGameanalysis::onSecsChanged);
    connect(spinThreshold, static_cast<void (QDoubleSpinBox::*)(double)>(&QDoubleSpinBox::valueChanged),
            this, &DialogGameanalysis::onThresholdChanged);

    this->setLayout(layout);
}

void DialogGameanalysis::onSecsChanged(int val) {
    this->secsPerMove = val * 1000;
}

void DialogGameanalysis::onThresholdChanged(double val) {
    this->threshold = val;
}
