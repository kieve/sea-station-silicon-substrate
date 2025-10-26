@echo off
setlocal

set "REPORT_HTML=%~dp0core\build\reports\jacoco\test\html\index.html"

if exist "%REPORT_HTML%" goto openReport

echo JaCoCo report not found. Generating via Gradle...
call "%~dp0gradlew.bat" core:jacocoTestReport
if errorlevel 1 (
  echo Gradle execution failed. Cannot open report.
  exit /b 1
)

if not exist "%REPORT_HTML%" (
  echo JaCoCo HTML report was not created at "%REPORT_HTML%".
  exit /b 1
)

:openReport
echo Opening JaCoCo report in Firefox...
set "REPORT_URI=file:///%REPORT_HTML:\=/%"
start "" "firefox" "%REPORT_URI%"

endlocal
