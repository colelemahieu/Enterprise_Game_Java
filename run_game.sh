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

# Run the game with performance optimizations
echo "Starting game..."
java -Djava.awt.headless=false -Dsun.java2d.opengl=true -Dsun.java2d.accthresh=0 enterpriseGame
