#ifndef DIALOG_GAMEANALYSIS_H
#define DIALOG_GAMEANALYSIS_H

#include <QDialog>
#include <QRadioButton>

class DialogGameanalysis : public QDialog
{
    Q_OBJECT
public:
    explicit DialogGameanalysis(int secsPerMove, float threshold, QWidget *parent = 0);
    int secsPerMove;
    double threshold;
    QRadioButton* rbAnalyseBoth;
    QRadioButton* rbAnalyseWhite;
    QRadioButton* rbAnalyseBlack;

signals:

public slots:
    void onSecsChanged(int val);
    void onThresholdChanged(double val);

};

#endif // DIALOG_GAMEANALYSIS_H
