@echo off
setlocal
set "MAVEN_VERSION=3.9.11"
if defined MAVEN_USER_HOME (
  set "MAVEN_BASE=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
) else (
  set "MAVEN_BASE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
)
set "MAVEN_HOME=%MAVEN_BASE%\apache-maven-%MAVEN_VERSION%"
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%MAVEN_BASE%" mkdir "%MAVEN_BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $url='https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip'; $zip='%MAVEN_BASE%\apache-maven-%MAVEN_VERSION%-bin.zip'; if (-not (Test-Path -LiteralPath $zip)) { Invoke-WebRequest -Uri $url -OutFile $zip }; Expand-Archive -LiteralPath $zip -DestinationPath '%MAVEN_BASE%' -Force"
  if errorlevel 1 exit /b 1
)
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%

