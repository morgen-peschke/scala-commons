#!/bin/bash

cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && 
    git config core.hooksPath .githooks
