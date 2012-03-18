#!/bin/bash
#convert $1 -thumbnail '300x300>' -background transparent -gravity center -extent 300x300 $(dirname $1)/thumb/$(basename $1)
convert $1 -thumbnail '300' -quality 60 $(dirname $1)/thumb/$(basename $1 .png).jpg
