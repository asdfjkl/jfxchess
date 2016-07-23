#ifndef DIALOG_NEWGAME_H
#define DIALOG_NEWGAME_H

#include <QDialog>
#include <QRadioButton>
#include <QLabel>
#include <QSlider>

class DialogNewGame : public QDialog
{
    Q_OBJECT
public:
    explicit DialogNewGame(bool customEngine, int currentStrength, int currentThinkTime, QWidget *parent = 0);
    bool customEngine;
    bool playsWhite;
    int computerStrength;
    int computerThinkTime;
    bool playsComputer;

private:
    QRadioButton *rbPlaysComputer;
    QRadioButton *rbEntersMoves;
    QRadioButton *rbPlaysWhite;
    QRadioButton *rbPlaysBlack;
    QLabel *lblStrengthValue;
    QSlider *sliderStrength;
    QLabel *lblThinkTime;
    QSlider *sliderThinkTime;

    int intervalValueToThinkTime(int intervalVal);
    int thinkTimeToIntervalVal(int thinkTime);

signals:

public slots:
    void rbPlaysComputerToggled();
    void rbEntersMovesToggled();
    void rbPlaysWhiteToggled();
    void rbPlaysBlackToggled();
    void sliderStrengthChanged();
    void sliderThinkTimeChanged();

};

#endif // DIALOG_NEWGAME_H
