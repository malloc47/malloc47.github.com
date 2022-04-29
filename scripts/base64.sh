#!/bin/bash
uuencode -m $1 dummy | grep -v ^begin | grep -v ==== | tr -d '\n'
