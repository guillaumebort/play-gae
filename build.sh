#!/bin/sh

###
# This script pulls in necessary dependencies and builds the module.
###

# Update dependencies
printf "\033[036m1/2 - Updating dependencies...\e[0m\n"
play deps --sync


# Build the module
if [ "$?" = 0 ]; then
    printf "\033[036m2/2 - Building the module...\e[0m\n"
    play build-module

    if [ "$?" = 0 ]; then
        printf "\n\e[1;32m Step 2/2 - Build succesfull\e[0m\n"
    else
        printf "\n\e[1;31mStep 2/2 - Build failed\e[0m\n"
    fi
else
    printf "\n\e[1;31m Step 1/2 - Updating dependencies failed.\e[0m\n"
fi
