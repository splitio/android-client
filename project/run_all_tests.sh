!#/bin/zsh

./gradlew test
./gradlew connectedAndroidTest
open ./build/reports/androidTests/connected/index.html