#!/bin/bash
rm -r -f release
mkdir release

mkdir release/jerry_linux_x32
cp jerry.py ./release/jerry_linux_x32/
cp main_window.py ./release/jerry_linux_x32

mkdir release/jerry_linux_x32/books
cp books/*.* ./release/jerry_linux_x32/books

mkdir release/jerry_linux_x32/chess
cp chess/*.py ./release/jerry_linux_x32/chess/

mkdir release/jerry_linux_x32/controller
cp controller/*.py ./release/jerry_linux_x32/controller/

mkdir release/jerry_linux_x32/dialogs
cp dialogs/*.py ./release/jerry_linux_x32/dialogs/

mkdir release/jerry_linux_x32/engine
cp engine/* ./release/jerry_linux_x32/engine/

mkdir release/jerry_linux_x32/i18n
cp i18n/*.py ./release/jerry_linux_x32/i18n/

mkdir release/jerry_linux_x32/model
cp model/*.py ./release/jerry_linux_x32/model/

mkdir release/jerry_linux_x32/res
cp -r res/* ./release/jerry_linux_x32/res

mkdir release/jerry_linux_x32/uci
cp uci/*.py ./release/jerry_linux_x32/uci

mkdir release/jerry_linux_x32/util
cp util/*.py ./release/jerry_linux_x32/util

mkdir release/jerry_linux_x32/views
cp views/*.py ./release/jerry_linux_x32/views/

tar -zcvf ./release/jerry_linux_x86.tar.gz ./release/jerry_linux_x32

# now build the debian package
# edit the name of the control file to
# control the resulting architecture.
# we only build for i386, since 
# the gui is architecture-independent
# and engines are supplied for both
# architectures
mkdir release/jerry
mkdir release/jerry/DEBIAN
mkdir release/jerry/usr
mkdir release/jerry/usr/bin
mkdir release/jerry/usr/share
mkdir release/jerry/usr/share/applications
mkdir release/jerry/usr/share/jerry
cp -r release/jerry_linux_x32/* release/jerry/usr/share/jerry/
cp debian_package_files/jerry.desktop release/jerry/usr/share/applications/
cp debian_package_files/jerry release/jerry/usr/bin/
cp debian_package_files/control_i386 release/jerry/DEBIAN/control
cp debian_package_files/postinst release/jerry/DEBIAN/postinst
cp debian_package_files/postrm release/jerry/DEBIAN/postrm
chmod u+x release/jerry/usr/share/jerry/jerry.py
cd release/
fakeroot dpkg-deb -b jerry .
cd ..
cp debian_package_files/control_amd64 release/jerry/DEBIAN/control
cd release
fakeroot dpkg-deb -b jerry .
cd ..
