#!/bin/bash

# Compile the game if not already compiled or if source is newer
if [ ! -f enterpriseGame.class ] || [ enterpriseGame.java -nt enterpriseGame.class ]; then
    echo "Compiling game..."
    javac enterpriseGame.java
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
    echo "Compilation successful!"
fi

# Run the game
echo "Starting game..."
java -Djava.awt.headless=false enterpriseGame
