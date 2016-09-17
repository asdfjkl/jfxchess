#!/bin/bash
echo 'cleaning up'
rm -r -f release
rm -r -f ~/workspace/build-jerry3-Desktop-Release/jerry_ubuntu16_04_1lts

echo 'collecting shared libs'
cp find_deps_linux ../build-jerry3-Desktop-Release/
cd ~/workspace/build-jerry3-Desktop-Release/
./find_deps_linux jerry

cd ~/workspace/jerry
mkdir release

echo 'setting up release directory...'
mkdir ./release/ubuntu64
mkdir ./release/ubuntu64/engine
mkdir ./release/ubuntu64/res
mkdir ./release/ubuntu64/books
cp ./engine/* ./release/ubuntu64/engine
cp ./books/* ./release/ubuntu64/books
cp -r ./res/* ./release/ubuntu64/res
cp ~/workspace/build-jerry3-Desktop-Release/Jerry ./release/ubuntu64/jerry
cp ~/workspace/jerry/jerry.sh ./release/ubuntu64/jerry.sh
cp ~/workspace/build-jerry3-Desktop-Release/jerry_ubuntu16_04_1lts/libs/* ./release/ubuntu64

mkdir release/jerry
mkdir release/jerry/DEBIAN
mkdir release/jerry/usr
mkdir release/jerry/usr/bin
mkdir release/jerry/usr/share
mkdir release/jerry/usr/share/applications
mkdir release/jerry/usr/share/jerry
echo 'copying binaries...'
cp -r release/ubuntu64/* release/jerry/usr/share/jerry/
cp debian_package_files/jerry.desktop release/jerry/usr/share/applications/
cp debian_package_files/jerry release/jerry/usr/bin/
cp debian_package_files/control_amd64 release/jerry/DEBIAN/control
cp debian_package_files/postinst release/jerry/DEBIAN/postinst
cp debian_package_files/postrm release/jerry/DEBIAN/postrm
chmod u+x release/jerry/usr/bin/jerry
echo 'building package'
cp debian_package_files/control_amd64 release/jerry/DEBIAN/control
cd release
fakeroot dpkg-deb -b jerry .
cd ..


