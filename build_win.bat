python setup.py py2exe --includes sip
mkdir dist\res
xcopy res dist\res /e
mkdir dist\engine
xcopy engine dist\engine /e

