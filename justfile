# use with https://github.com/casey/just
set shell := ['bash', '-c']

test:
    ./gradlew test allTests runIntegrationTests detekt

detekt:
    ./gradlew detekt

build:
    ./gradlew build bundle

build-ios:
    xcodebuild -workspace iosApp/iosApp.xcworkspace -scheme iosApp -configuration Release archive -archivePath build/iosApp.xcarchive

build-debug:
    ./gradlew assembleFullDebug

release:
    bundle exe fastlane release
