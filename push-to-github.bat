@echo off
chcp 65001 >nul
color 0A
title Обновление репозитория GitHub

echo =========================================================
echo       СБРОС И ОБНОВЛЕНИЕ РЕПОЗИТОРИЯ GITHUB
echo =========================================================
echo.
echo  Внимание! Этот скрипт обновит все файлы в репозитории
echo  GitHub, включая ваш локальный README.md.
echo.
echo  Убедитесь, что ваш токен GitHub готов к использованию!
echo =========================================================
echo.

set /p confirm=Вы уверены, что хотите продолжить? (y/n): 
if /i not "%confirm%"=="y" goto :cancel

echo.
set token=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
echo.

echo Очистка временных данных...
if exist temp_readme rmdir /S /Q temp_readme
echo.

REM Проверяем, существует ли локальный README.md
set has_readme=false
if exist README.md (
  set has_readme=true
  echo Обнаружен локальный README.md, он будет загружен в репозиторий.
  echo.
)

echo Удаление .git папки...
rmdir /S /Q .git 2>nul
echo.

echo Очистка настроек Git...
if exist .gitignore del /F .gitignore
echo.

echo Инициализация нового репозитория...
git init
echo.

echo Настройка пользователя Git...
git config user.email "zoobastiks.developer@gmail.com"
git config user.name "Zoobastiks"
echo.

echo Создание .gitignore...
echo reset_and_push.bat > .gitignore
echo.

echo Добавление удаленного репозитория...
git remote add origin https://Z-oobastik-s:%token%@github.com/Z-oobastik-s/Zrtp.git
echo.

echo Создание ветки master...
git checkout -b master
echo.

echo Добавление всех файлов...
git add --all
echo.

echo Создание начального коммита...
git commit -m "Полное обновление проекта"
if %errorlevel% neq 0 (
  echo Ошибка при создании коммита!
  goto :cleanup_error
)
echo.

echo Принудительное обновление удаленного репозитория...
git push -f origin master
if %errorlevel% neq 0 (
  echo Ошибка при отправке изменений!
  goto :cleanup_error
)
echo.

echo =========================================================
echo Репозиторий успешно сброшен и обновлен!
if "%has_readme%"=="true" (
  echo Ваш локальный README.md был загружен в репозиторий.
)
echo BAT-файлы не загружены в репозиторий (.gitignore).
echo =========================================================
goto :end

:cleanup_error
echo.
echo =========================================================
echo Произошла ошибка. Очистка...
echo =========================================================
goto :end

:cancel
echo.
echo Операция отменена пользователем.

:end
echo.
echo Нажмите любую клавишу для завершения...
pause > nul 