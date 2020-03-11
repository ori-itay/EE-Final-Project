#!/bin/bash

set -e
./gradlew build
mkdir -p ../mobile/VisualCrypto/app/libs
cp all-pc.jar ../mobile/VisualCrypto/app/libs

