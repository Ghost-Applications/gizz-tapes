# use with https://github.com/casey/just
set shell := ['bash', '-c']

test:
    ./gradlew test allTests runIntegrationTests detekt

detekt:
    ./gradlew detekt

build:
    ./gradlew build bundle

build-debug:
    ./gradlew assembleFullDebug

build-ios:
    xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS' -configuration Release archive -archivePath build/iosApp.xcarchive

release:
    bundle exec fastlane android release
    bundle exec fastlane ios release
