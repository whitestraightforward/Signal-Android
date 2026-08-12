[33mcommit 41208dcb9e929a135b0e2c3bf066b5623b7f5443[m[33m ([m[1;36mHEAD[m[33m -> [m[1;32mmain[m[33m)[m
Author: Your Name <you@example.com>
Date:   Thu Jul 16 10:50:50 2026 -0700

    update

[1mdiff --git a/gradlew.bat b/gradlew.bat[m
[1mindex ac1b06f938..e69de29bb2 100644[m
[1m--- a/gradlew.bat[m
[1m+++ b/gradlew.bat[m
[36m@@ -1,89 +0,0 @@[m
[31m-@rem[m
[31m-@rem Copyright 2015 the original author or authors.[m
[31m-@rem[m
[31m-@rem Licensed under the Apache License, Version 2.0 (the "License");[m
[31m-@rem you may not use this file except in compliance with the License.[m
[31m-@rem You may obtain a copy of the License at[m
[31m-@rem[m
[31m-@rem      https://www.apache.org/licenses/LICENSE-2.0[m
[31m-@rem[m
[31m-@rem Unless required by applicable law or agreed to in writing, software[m
[31m-@rem distributed under the License is distributed on an "AS IS" BASIS,[m
[31m-@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.[m
[31m-@rem See the License for the specific language governing permissions and[m
[31m-@rem limitations under the License.[m
[31m-@rem[m
[31m-[m
[31m-@if "%DEBUG%" == "" @echo off[m
[31m-@rem ##########################################################################[m
[31m-@rem[m
[31m-@rem  Gradle startup script for Windows[m
[31m-@rem[m
[31m-@rem ##########################################################################[m
[31m-[m
[31m-@rem Set local scope for the variables with windows NT shell[m
[31m-if "%OS%"=="Windows_NT" setlocal[m
[31m-[m
[31m-set DIRNAME=%~dp0[m
[31m-if "%DIRNAME%" == "" set DIRNAME=.[m
[31m-set APP_BASE_NAME=%~n0[m
[31m-set APP_HOME=%DIRNAME%[m
[31m-[m
[31m-@rem Resolve any "." and ".." in APP_HOME to make it shorter.[m
[31m-for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi[m
[31m-[m
[31m-@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.[m
[31m-set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"[m
[31m-[m
[31m-@rem Find java.exe[m
[31m-if defined JAVA_HOME goto findJavaFromJavaHome[m
[31m-[m
[31m-set JAVA_EXE=java.exe[m
[31m-%JAVA_EXE% -version >NUL 2>&1[m
[31m-if "%ERRORLEVEL%" == "0" goto execute[m
[31m-[m
[31m-echo.[m
[31m-echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.[m
[31m-echo.[m
[31m-echo Please set the JAVA_HOME variable in your environment to match the[m
[31m-echo location of your Java installation.[m
[31m-[m
[31m-goto fail[m
[31m-[m
[31m-:findJavaFromJavaHome[m
[31m-set JAVA_HOME=%JAVA_HOME:"=%[m
[31m-set JAVA_EXE=%JAVA_HOME%/bin/java.exe[m
[31m-[m
[31m-if exist "%JAVA_EXE%" goto execute[m
[31m-[m
[31m-echo.[m
[31m-echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%[m
[31m-echo.[m
[31m-echo Please set the JAVA_HOME variable in your environment to match the[m
[31m-echo location of your Java installation.[m
[31m-[m
[31m-goto fail[m
[31m-[m
[31m-:execute[m
[31m-@rem Setup the command line[m
[31m-[m
[31m-set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar[m
[31m-[m
[31m-[m
[31m-@rem Execute Gradle[m
[31m-"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*[m
[31m-[m
[31m-:end[m
[31m-@rem End local scope for the variables with windows NT shell[m
[31m-if "%ERRORLEVEL%"=="0" goto mainEnd[m
[31m-[m
[31m-:fail[m
[31m-rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of[m
[31m-rem the _cmd.exe /c_ return code![m
[31m-if  not "" == "%GRADLE_EXIT_CONSOLE%" exit 1[m
[31m-exit /b 1[m
[31m-[m
[31m-:mainEnd[m
[31m-if "%OS%"=="Windows_NT" endlocal[m
[31m-[m
[31m-:omega[m
