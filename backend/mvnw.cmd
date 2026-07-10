@echo off
setlocal
set BASE_DIR=%~dp0
set MAVEN_VERSION=3.9.9
set MAVEN_HOME=%BASE_DIR%.mvn\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\bin\mvn.cmd

for /f "tokens=2 delims= " %%v in ('javac -version 2^>^&1') do set PATH_JAVAC_VERSION=%%v
echo %PATH_JAVAC_VERSION% | findstr /r "^17\." >nul
if %ERRORLEVEL%==0 set JAVA_HOME=

if not exist "%MAVEN_BIN%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop';" ^
    "$base='%BASE_DIR%';" ^
    "$version='%MAVEN_VERSION%';" ^
    "$dir=Join-Path $base '.mvn';" ^
    "New-Item -ItemType Directory -Force -Path $dir | Out-Null;" ^
    "$zip=Join-Path $dir ('apache-maven-' + $version + '-bin.zip');" ^
    "$url='https://archive.apache.org/dist/maven/maven-3/' + $version + '/binaries/apache-maven-' + $version + '-bin.zip';" ^
    "Invoke-WebRequest -Uri $url -OutFile $zip;" ^
    "Expand-Archive -Path $zip -DestinationPath $dir -Force;"
)

call "%MAVEN_BIN%" %*
set EXIT_CODE=%ERRORLEVEL%
endlocal & exit /b %EXIT_CODE%
