@echo off
REM Terrier - Terabyte Retriever
REM Webpage: http://ir.dcs.gla.ac.uk/terrier
REM Contact: terrier@dcs.gla.ac.uk
REM
REM The contents of this file are subject to the Mozilla Public
REM License Version 1.1 (the "License"); you may not use this file
REM except in compliance with the License. You may obtain a copy of
REM the License at http://www.mozilla.org/MPL/
REM
REM Software distributed under the License is distributed on an "AS
REM IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
REM implied. See the License for the specific language governing
REM rights and limitations under the License.
REM
REM The Original Code is compile.bat
REM
REM The Initial Developer of the Original Code is the Jurrie Overgoor
REM Portions created by The Initial Developer are Copyright (C) 2006
REM the initial Developer. All Rights Reserved.
REM
REM Contributor(s):
REM   Jurrie Overgoor (j.m.overgoor@alumnus.utwente.nl)
REM   
REM ----------------------------------------------------------------------------
REM COMPILE.BAT - By Jurrie Overgoor (j.m.overgoor@alumnus.utwente.nl)
REM
REM It's basically a simple port of compile.sh to a Windows environment.
REM I hope it works... Let me know.
REM ----------------------------------------------------------------------------

if "Windows_NT"=="%OS%" setlocal

rem keep %0 in case we overwrite
SET PROGRAM=%0
rem SCRIPT contains the full path filename of this script
SET SCRIPT=%~f0
rem BIN contains the path of the BIN folder
SET BIN=%~dp0

set COLLECTIONPATH=%~f1

REM --------------------------
REM Load a settings batch file if it exists
REM --------------------------
if NOT EXIST "%BIN%\terrier-env.bat" GOTO defaultvars
CALL "%BIN%\terrier-env.bat" "%BIN%\.."

:defaultvars

if defined VERSION goto terrier_bin
SET VERSION=2.2.1


REM -----------------------------------------------------
REM Derive TERRIER_HOME, TERRIER_ETC, TERRIER_LIB, etc.
REM -----------------------------------------------------
:terrier_bin
if defined TERRIER_HOME goto terrier_etc
CALL "%BIN%\fq.bat" "%BIN%\.."
SET TERRIER_HOME=%FQ%
echo Set TERRIER_HOME to be %TERRIER_HOME%

:terrier_etc
if defined TERRIER_ETC goto terrier_lib
SET TERRIER_ETC=%TERRIER_HOME%\etc

:terrier_lib
if defined TERRIER_LIB goto terrier_jarname
SET TERRIER_LIB=%TERRIER_HOME%\lib

:terrier_jarname
if defined JARNAME goto terrier_tmpdir
SET JARNAME=terrier-%VERSION%.jar

:terrier_tmpdir
if defined TMPDIR goto classpath
SET TMPDIR=%TERRIER_HOME%\tmp_classes

:classpath
REM ------------------------
REM -- Build up class path
REM ------------------------
SET LOCALCLASSPATH=%TERRIER_HOME%\src
call "%BIN%\lcp.bat" %CLASSPATH%
FOR %%i IN ("%TERRIER_LIB%\*.jar") DO call "%BIN%\lcp.bat" "%%i"

REM ---------------------------
REM -- Compile ANTLR .g files
REM ---------------------------
cd %TERRIER_HOME%\src\uk\ac\gla\terrier\querying\parser
"%JAVA_HOME%\bin\java" -cp "%LOCALCLASSPATH%" antlr.Tool terrier_floatlex.g
"%JAVA_HOME%\bin\java" -cp "%LOCALCLASSPATH%" antlr.Tool terrier_normallex.g
"%JAVA_HOME%\bin\java" -cp "%LOCALCLASSPATH%" antlr.Tool terrier.g
cd %TERRIER_HOME%

REM --------------------------------------------------------
REM -- Compile .java files, and jar .class files
REM --------------------------------------------------------
xcopy %TERRIER_HOME%\src\*.java %TMPDIR% /E /Y /I /H /Q > nul
dir %TMPDIR%\*.java /B /S > %TMPDIR%\_files.src
"%JAVA_HOME%\bin\javac" -classpath %LOCALCLASSPATH% @%TMPDIR%\_files.src
del /Q %TMPDIR%\_files.src
del /F /S /Q %TMPDIR%\*.java > nul
"%JAVA_HOME%\bin\jar" cfM %TERRIER_HOME%\lib\%JARNAME% -C %TMPDIR% .
cd %TERRIER_HOME%
rmdir %TMPDIR% /S /Q

if "Windows_NT"=="%OS%" endlocal
