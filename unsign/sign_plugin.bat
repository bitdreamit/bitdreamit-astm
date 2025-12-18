@echo off
setlocal enabledelayedexpansion

REM =====================================================
REM CONFIGURATION
REM =====================================================
set "UNSIGN_DIR=D:\Java\bitdreamit-astm\unsign\bitdreamit-astm"
set "SIGN_DIR=D:\Java\bitdreamit-astm\sign\bitdreamit-astm"
set "JKS_FILE=%SIGN_DIR%\mykeystore.jks"
set "P12_FILE=%SIGN_DIR%\mykeystore.p12"
set "STOREPASS=bitdreamit"
set "KEYPASS=bitdreamit"
set "ALIAS=mykey"
set "VALIDITY_DAYS=3650"

REM Check JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo ERROR: JAVA_HOME is not set
    exit /b 1
)

REM =====================================================
REM CLEAN AND CREATE SIGN DIRECTORY
REM =====================================================
if exist "%SIGN_DIR%" rmdir /s /q "%SIGN_DIR%"
mkdir "%SIGN_DIR%"

REM =====================================================
REM COPY UNSIGNED JARS
REM =====================================================
xcopy "%UNSIGN_DIR%\*" "%SIGN_DIR%\" /E /I /Y >nul

cd /d "%SIGN_DIR%"

REM =====================================================
REM CREATE KEYSTORE
REM =====================================================
if not exist "%JKS_FILE%" (
    "%JAVA_HOME%\bin\keytool" -genkeypair -alias %ALIAS% -keyalg RSA -keysize 2048 -validity %VALIDITY_DAYS% -keystore "%JKS_FILE%" -storepass %STOREPASS% -keypass %KEYPASS% -dname "CN=Bit Dream IT ASTM Plugin, OU=Development, O=Bit Dream IT, C=BD" -storetype JKS
    if errorlevel 1 exit /b 1
)

"%JAVA_HOME%\bin\keytool" -importkeystore -srckeystore "%JKS_FILE%" -destkeystore "%P12_FILE%" -deststoretype PKCS12 -srcstorepass %STOREPASS% -deststorepass %STOREPASS% -srcalias %ALIAS% -destalias %ALIAS% -srckeypass %KEYPASS% -destkeypass %KEYPASS% -noprompt
if errorlevel 1 exit /b 1

REM =====================================================
REM SIGN JARS
REM =====================================================
REM Handle lib folder separately
if exist "lib\AsyncAstm-3.2.jar" (
    cd lib
    "%JAVA_HOME%\bin\jarsigner" -keystore "%P12_FILE%" -storetype pkcs12 -storepass %STOREPASS% -keypass %KEYPASS% "AsyncAstm-3.2.jar" %ALIAS%
    if errorlevel 1 exit /b 1
    cd ..
)

for %%J in (astm-client.jar astm-server.jar astm-shared.jar) do (
    if exist "%%J" (
        "%JAVA_HOME%\bin\jarsigner" -keystore "%P12_FILE%" -storetype pkcs12 -storepass %STOREPASS% -keypass %KEYPASS% "%%J" %ALIAS%
        if errorlevel 1 exit /b 1
    )
)

REM =====================================================
REM VERIFY JARS
REM =====================================================
set "VERIFY_PASSED=1"

if exist "lib\AsyncAstm-3.2.jar" (
    cd lib
    "%JAVA_HOME%\bin\jarsigner" -verify "AsyncAstm-3.2.jar" || set "VERIFY_PASSED=0"
    cd ..
)

for %%J in (astm-client.jar astm-server.jar astm-shared.jar) do (
    if exist "%%J" (
        "%JAVA_HOME%\bin\jarsigner" -verify "%%J" || set "VERIFY_PASSED=0"
    )
)

if "%VERIFY_PASSED%"=="0" (
    echo ERROR: Some jars failed verification
    exit /b 1
)

REM =====================================================
REM CREATE FINAL ZIP
REM =====================================================
set "FINAL_ZIP=%SIGN_DIR%\bitdreamit-astm.zip"
if exist "%FINAL_ZIP%" del /q "%FINAL_ZIP%"
powershell -Command "Compress-Archive -Path '%SIGN_DIR%\*' -DestinationPath '%FINAL_ZIP%' -Force"

echo All jars signed and zipped successfully!
pause
