#! /bin/sh

HOST=localhost
DB=db_icc
USERNAME=root
PASSWORD=sa

ANDROID_JARS=../../android-platforms
APP=$1

rm -rf testspace/*

java -Xmx8192m -jar Epicc.jar $ANDROID_JARS $APP $HOST $DB $USERNAME $PASSWORD

rm -rf testspace/*
