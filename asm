#!/bin/bash

fasm $1 _temp
chmod +x _temp

./_temp
echo Exited with: $?

rm _temp