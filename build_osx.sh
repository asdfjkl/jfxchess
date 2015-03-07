python3 setup.py py2app --includes "sip"
cp -R res dist/Jerry.app/Contents/Resources/
cp -R books dist/Jerry.app/Contents/Resources/
cp -R engine dist/Jerry.app/Contents/Resources/
cp qt.conf dist/Jerry.app/Contents/Resources/qt.conf
