# use with https://github.com/casey/just
set shell := ['bash', '-c']

record-screenshots:
    ./gradlew recordPaparazzi

build:
    ./gradlew build bundle

release:
    bundle exe fastlane release
