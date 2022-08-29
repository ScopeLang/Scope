#!/bin/bash

fasm $1 _temp.out
chmod +x _temp.out

./_temp.out
echo Exited with: $?