mkdir jerry.app/Contents/Resources
cp -R res jerry.app/Contents/Resources/
cp -R data jerry.app/Contents/Resources/
mkdir jerry.app/Contents/Resources/Books
mkdir jerry.app/Contents/Resources/Databases
mkdir jerry.app/Contents/Resources/Engines
macdeployqt Jerry.app -dmg


