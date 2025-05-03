@echo off
echo === Загрузка проекта Zrtp на GitHub ===
echo.

echo 1. Установка конфигурации Git...
git config --global credential.helper store
git config --global user.name "Z-oobastik-s"
git config --global user.email "ваш-email@example.com"

echo 2. Убедимся, что файлы добавлены в индекс...
git add .

echo 3. Коммит изменений...
git commit -m "Initial commit - Random Teleport plugin with multi-language support"

echo 4. Настройка удаленного репозитория...
git remote set-url origin https://github.com/Z-oobastik-s/Zrtp.git

echo 5. Отправка на GitHub (введите ваш токен вместо пароля)...
echo    Имя пользователя: Z-oobastik-s
git push -u origin master

echo.
echo === Готово! Проверьте репозиторий на GitHub ===
echo https://github.com/Z-oobastik-s/Zrtp

pause 