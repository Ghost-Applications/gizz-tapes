# use with https://github.com/casey/just
set shell := ['bash', '-c']

test:
    ./gradlew test allTests runIntegrationTests detekt

build:
    ./gradlew build bundle

release:
    bundle exe fastlane release
