# Our Cozy Farm

A lil Stardew-style farming sim for Android, made for Jeremy.

No internet needed, no account, your farm saves on your phone.

## How to play

1. Till the grass.
2. Pick a seed and Plant it on tilled soil.
3. Water it.
4. Tap Next Day to grow it. Water again each day or it will not grow.
5. Harvest when it is ready, then plant again on that same soil.

Crops:
- Wheat: 10 coins to plant, sells for 25, takes 2 watered days
- Carrot: 15 coins to plant, sells for 40, takes 3 watered days
- Pumpkin: 25 coins to plant, sells for 80, takes 4 watered days

You start with 50 coins on Day 1, on a 6 by 5 field (30 tiles).

## Build the APK with GitHub, no Android Studio

See GITHUB_BUILD.md. In short: create an empty repo called cozy-farm
on github.com, upload this project (including .github/workflows/build-apk.yml),
wait for the Build APK action, download the cozy-farm-debug-apk artifact,
unzip it, and install app-debug.apk on your phone.

This workflow uses Gradle 8.7 and JDK 17, and installs platform 34 and
build-tools 34.0.0 from the runner's preinstalled Android SDK. It does
not use android-actions/setup-android, which fails on current runners.

## Files

- app/src/main/java/com/jeremy/cozyfarm/MainActivity.kt
  All of the game logic: tiles, tools, seeds, days, coins, and saving.
- app/src/main/AndroidManifest.xml
  App name and launcher activity.
- app/build.gradle.kts, build.gradle.kts, settings.gradle.kts
  Standard Android build setup: Kotlin, AppCompat, Material.

## Change it later

To add a crop, add one line to the crops map in MainActivity.kt with
its seed cost, sell price, growth days, and ready emoji. To change the
starting coins or field size, change coins, cols, and rows at the top
of MainActivity.kt.
