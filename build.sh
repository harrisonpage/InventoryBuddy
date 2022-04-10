#!/usr/bin/bash
mvn package && \
    cp target/InventoryBuddy-1.0.0.jar /minecraft/kitty.lawyer/plugins/ &&
    cp target/InventoryBuddy-1.0.0.jar /minecraft/toilet.quest/plugins/
