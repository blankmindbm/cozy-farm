# Our Cozy Farm

A lil Stardew-style farming sim for Android, made for Jeremy.

No internet needed, no account, your farm saves on your phone.

## How to play (v4: isometric 2.5D map, house, and animals)

The map is now drawn in isometric 2.5D: tiles are diamonds, with the top of the map at the top center. All v3 gameplay is unchanged, only the look changed.

Walk a 10 by 8 map. House is top left, pond top right, mine middle right,
animal pen bottom right, and the farm field is the middle area, cols 2 to 7,
rows 2 to 5.

1. Hold an arrow button to walk your farmer around the map. House, pond,
   and mine tiles block walking. Your position saves.
2. Stand on a field tile and tap Use Tool. With Till, that tills the soil
   under you. Plant, water, and harvest there as before.
3. Buy a chicken (50c) or a cow (150c). It lives in the pen, up to 6 animals.
4. Each Next Day, animals make produce and wander to a new pen tile.
   Stand on an animal and use Collect: egg 15c, milk 35c.
5. Walk next to your house and tap Home to save with a welcome home message.
   Pond and mine are on the map now, fishing and mining come in a later stage.

Crops:
- Wheat: 10 coins to plant, sells for 25, takes 2 watered days
- Carrot: 15 coins to plant, sells for 40, takes 3 watered days
- Pumpkin: 25 coins to plant, sells for 80, takes 4 watered days

You start with 50 coins on Day 1. Note: v3 changed the map from 6 by 5 to
10 by 8, so an old v2 field layout does not carry over, but coins, day,
and animals (if any) logic starts clean on the new map.

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
