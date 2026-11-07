# Minecraft Mod Core

Заготовка проекта для **Minecraft Java 1.21.1**, **NeoForge** и **Fabric**.
Требуется **JDK 21**. Открывать в IDE нужно эту папку, где расположен `settings.gradle`.

## Структура

- `common/` — общая игровая логика, интерфейсы платформы и ресурсы.
- `neoforge/` — точка входа, адаптер платформы и сборка NeoForge.
- `fabric/` — точка входа, адаптер платформы и сборка Fabric.
- `buildSrc/` — общие Gradle-конвенции.
- `gradle/`, `gradlew`, `gradlew.bat` — Gradle Wrapper; отдельная установка Gradle не нужна.
- `work_notes/` — локальные заметки и справочник ресурсов, исключены из Git.

## Команды PowerShell

```powershell
.\gradlew.bat build
.\gradlew.bat :neoforge:runClient
.\gradlew.bat :fabric:runClient
.\gradlew.bat :neoforge:runServer
.\gradlew.bat :fabric:runServer
```

Готовые моды находятся в `neoforge/build/libs/` и `fabric/build/libs/`.
Для пользователей предназначены обычные JAR, без суффиксов `sources`, `javadoc` и `dev`.
Первый запуск сервера требует принятия EULA владельцем сервера.

## Перед разработкой конкретного мода

Это пока шаблон: `examplemod`, `ExampleMod`, автор и пакет `com.example.examplemod`
сохранены как заполнители. Имя Gradle-проекта — `core`.
После выбора имени согласованно обновить `gradle.properties`, пакеты Java,
точки входа, имена Mixin-конфигураций и файлы `META-INF/services`.
Проверить выбранную лицензию перед публикацией.

Версии зависимостей перенесены из шаблона без массового обновления.
Fabric Loom закреплён на версии `1.8.13` вместо изменяемого `1.8-SNAPSHOT`.
Оба загрузчика используют Mojang mappings с Parchment.
NeoForge ограничен точной версией Minecraft 1.21.1, как и Fabric.
Дополнительные runtime-библиотеки, включая GeckoLib, пока не подключены.

Общий код не должен обращаться напрямую к API загрузчика. Клиентский рендеринг
должен быть отделён от кода, загружаемого dedicated server.
Перед выпуском проверять клиент и dedicated server обоих загрузчиков.

## Происхождение

Основа: [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1),
ветка `1.21.1`, коммит `7bd74e027936c3ecefb38a518c5d7ac71476d543`.
Исходная лицензия сохранена в `LICENSE`.
