@ECHO OFF
SETLOCAL

SET "BASE_DIR=%~dp0"
SET "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
SET "PROPERTIES_FILE=%WRAPPER_DIR%\maven-wrapper.properties"
SET "DISTRIBUTION_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
SET "DISTRIBUTION_DIR=%WRAPPER_DIR%\dists\apache-maven-3.9.9"
SET "MAVEN_CMD=%DISTRIBUTION_DIR%\bin\mvn.cmd"
SET "ZIP_FILE=%WRAPPER_DIR%\apache-maven-3.9.9-bin.zip"

IF EXIST "%PROPERTIES_FILE%" (
  FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPERTIES_FILE%") DO (
    IF "%%A"=="distributionUrl" SET "DISTRIBUTION_URL=%%B"
  )
)

FOR %%I IN ("%DISTRIBUTION_URL%") DO SET "ZIP_FILE=%WRAPPER_DIR%\%%~nxI"

IF NOT EXIST "%MAVEN_CMD%" (
  IF NOT EXIST "%WRAPPER_DIR%" MKDIR "%WRAPPER_DIR%"
  IF NOT EXIST "%WRAPPER_DIR%\dists" MKDIR "%WRAPPER_DIR%\dists"
  ECHO Downloading Maven distribution...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%ZIP_FILE%'"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%WRAPPER_DIR%\\dists' -Force"
)

IF NOT EXIST "%MAVEN_CMD%" (
  ECHO Failed to prepare the Maven distribution at %MAVEN_CMD%
  EXIT /B 1
)

cmd /d /s /c ""%MAVEN_CMD%" %*"

ENDLOCAL