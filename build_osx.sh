rm -r -f dist
mkdir dist
cp -R ../build-jerry3-Desktop_Qt_5_6_0_clang_64bit-Release/Jerry.app ./dist
cp -R res ./dist/Jerry.app/Contents/Resources/
cp -R engine ./dist/Jerry.app/Contents/Resources/
cp -R books ./dist/Jerry.app/Contents/Resources/
cp res/icons/osx_icon.icns ./dist/Jerry.app/Contents/Resources/
cp Info.plist ./dist/Jerry.app/Contents/Info.plist
../Qt/5.6/clang_64/bin/macdeployqt /Users/user/jerry/dist/Jerry.app -dmg
