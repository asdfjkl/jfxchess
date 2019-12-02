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


#include <QApplication>
#include <QObject>
#include <QList>
#include <iostream>
#include "chess/board.h"
#include "chess/move.h"
#include <bitset>
#include "funct.h"
#include "main_window.h"
#include <ctime>
#include <QDebug>
#include <QStyleFactory>
#include "various/resource_finder.h"
#include "profile/profile.h"

#include <chrono>

using namespace std;

int main(int argc, char *argv[]) {

    Profile::durationRunAll = chrono::nanoseconds::zero();
    Profile::first_part = chrono::nanoseconds::zero();
    Profile::pseudo_generation = chrono::nanoseconds::zero();
    Profile::filter_pseudos = chrono::nanoseconds::zero();
    Profile::filter_legal_check = chrono::nanoseconds::zero();
    Profile::parse_san_fast = chrono::nanoseconds::zero();

    srand(time(NULL));
    chess::FuncT *p = new chess::FuncT();
    auto start = std::chrono::steady_clock::now();

    p->run_pgn_parse_speedtest();

    auto stop = std::chrono::steady_clock::now();
    std::chrono::duration<double> diff = (stop - start);
    auto i_millis = std::chrono::duration_cast<std::chrono::nanoseconds>(diff);
    Profile::durationRunAll += i_millis;

    std::cout << "Duration All       :" << (Profile::durationRunAll.count() ) << '\n';
    std::cout << "Duration First Part:" << (Profile::first_part.count() ) << '\n';
    std::cout << "Duration Pseudo Gen:" << (Profile::pseudo_generation.count() ) << '\n';
    std::cout << "Duration Filter Pse:" << (Profile::filter_pseudos.count() ) << '\n';
    std::cout << "Duration Filter Leg:" << (Profile::filter_legal_check.count() ) << '\n';
    std::cout <<std::fixed<< "Duration Parse San :" << (Profile::parse_san_fast.count() ) << '\n';

    //p->run_pgn_speedtest();
    //p->run_zobrist_test();
    //p->run_pgnt();
    //p.run_pgn_scant();
    //p.run_sant();
    // p->run_ucit();
    p->run_pertf();

    //QApplication::setAttribute(Qt::AA_EnableHighDpiScaling); // DPI support
    QApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
    // from Qt 5.6 onwards

    qDebug() << "foo";

    QApplication app(argc, argv);
    app.setOrganizationName("dkl");
    app.setApplicationName("Jerry");

    // setting dark style
    // todo: fix bold colors in textedit
    // currently not activated
    //qApp->setStyle(QStyleFactory::create("Fusion"));
    /*
        QPalette darkPalette;
        darkPalette.setColor(QPalette::Window, QColor(53,53,53));
        darkPalette.setColor(QPalette::WindowText, Qt::white);
        darkPalette.setColor(QPalette::Base, QColor(25,25,25));
        darkPalette.setColor(QPalette::AlternateBase, QColor(53,53,53));
        darkPalette.setColor(QPalette::ToolTipBase, Qt::white);
        darkPalette.setColor(QPalette::ToolTipText, Qt::white);
        darkPalette.setColor(QPalette::Text, Qt::white);
        darkPalette.setColor(QPalette::Button, QColor(53,53,53));
        darkPalette.setColor(QPalette::ButtonText, Qt::white);
        darkPalette.setColor(QPalette::BrightText, Qt::red);
        darkPalette.setColor(QPalette::Link, QColor(42, 130, 218));

        darkPalette.setColor(QPalette::Highlight, QColor(42, 130, 218));
        darkPalette.setColor(QPalette::HighlightedText, Qt::black);

        qApp->setPalette(darkPalette);

        qApp->setStyleSheet("QToolTip { color: #ffffff; background-color: #2a82da; border: 1px solid white; }");
    */
    // dark style end


    // set application icon
    QIcon *app_icon = new QIcon();
    QString path = ResourceFinder::getPath();
#ifdef __APPLE__
    path = path.append("/../Resources/");
#endif
    app_icon->addFile(":/res/icons/jerry_icon16.png",   QSize(16,16));
    app_icon->addFile(":/res/icons/jerry_icon32.png",   QSize(32,32));
    app_icon->addFile(":/res/icons/jerry_icon48.png",   QSize(48,48));
    app_icon->addFile(":/res/icons/jerry_icon256.png",  QSize(256,256));
    //app_icon->addFile(":/res/icons/icon1024.png", QSize(1024,1024));
    app.setWindowIcon(*app_icon);

    //app.setStyle(QStyleFactory::create("Fusion"));
    //app.setStyle(QStyleFactory::create("Windows"));

    MainWindow mainWin;

    QObject::connect(&app, &QApplication::aboutToQuit, &mainWin, &MainWindow::aboutToQuit);

    //mainWin.centerAndResize();
    mainWin.show();
    mainWin.resetLayout();

    return app.exec();

}
