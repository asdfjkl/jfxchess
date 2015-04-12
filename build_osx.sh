/opt/local/bin/python3.4 setup.py py2app --includes "sip"
cp -R res dist/Jerry.app/Contents/Resources/
cp -R books dist/Jerry.app/Contents/Resources/
cp -R engine dist/Jerry.app/Contents/Resources/
cp qt.conf dist/Jerry.app/Contents/Resources/qt.conf
hdiutil create -volname Jerry -srcfolder ./dist -ov -format UDZO Jerry.dmg
