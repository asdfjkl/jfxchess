mkdir Jerry.app/Contents/Resources
cp -R res Jerry.app/Contents/Resources/
cp -R data Jerry.app/Contents/Resources/
mkdir Jerry.app/Contents/Resources/Books
mkdir Jerry.app/Contents/Resources/Databases
mkdir Jerry.app/Contents/Resources/Engines
macdeployqt Jerry.app -dmg


