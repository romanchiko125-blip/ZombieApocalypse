# Zombie Apocalypse Mod (Forge 1.12.2)

Открывай проект в **IntelliJ IDEA** как Gradle-проект (через `build.gradle`).

## Что уже есть
- Gradle-проект + ForgeGradle
- Gradle-конфигурация для Forge 1.12.2
- Исходники мода в `src/main/java`
- Ресурсы мода в `src/main/resources`

## Сборка JAR
В терминале из корня проекта:

```bash
gradle build
```

Готовый файл мода будет в:

`build/libs/`

## Если IDE пишет `Resolve conflicts` / `Could not resolve`
- В IntelliJ нажми `Reload All Gradle Projects`.
- Проверь, что в Project SDK выбран **Java 8**.
- В `Settings -> Build Tools -> Gradle` выбери **Use Gradle from: Gradle wrapper** или локальный Gradle.
- Если wrapper не используется в твоём окружении, просто собирай из терминала командой `gradle build`.
