set VERSION=1.2.6-SNAPSHOT
call ..\set-play-home-%VERSION%.bat

set MODULE_NAME=testrunner

call install-play-module-with-jar.bat
