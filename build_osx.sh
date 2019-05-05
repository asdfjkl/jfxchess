mkdir Jerry.app/Contents/Resources
cp -R ../Resources/res Jerry.app/Contents/Resources/
cp -R ../Resources/books Jerry.app/Contents/Resources/
cp -R ../Resources/engine Jerry.app/Contents/Resources/
cp ../Resources/res/icons/osx_icon.icns Jerry.app/Contents/Resources/osx_icon.icns
cp Info.plist Jerry.app/Contents/Info.plist
/Users/user/Qt/5.12.3/clang_64/bin/macdeployqt Jerry.app -dmg


