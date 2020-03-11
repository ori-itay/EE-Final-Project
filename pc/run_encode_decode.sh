#!/bin/bash
set -e 

FILE=all-pc.jar
if [ ! -f "$FILE" ]; then
	./gradlew build -x test	
fi
java -jar all-pc.jar < encode_decode.txt
