#!/bin/sh
base=$1
shift

for file in $@; do
  cat $base > build/$file.js
  echo "main = module.exports.$file;" >> build/$file.js
done