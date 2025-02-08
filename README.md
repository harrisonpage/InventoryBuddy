# InventoryBuddy

Minecraft plugin to automatically arrange inventory by loading/saving an inventory layout.

* `/inventory save` will store an inventory layout: a list of items and slot numbers
* `/inventory load` will apply the saved inventory layout
* `/inventory list` will show an existing layout

# Edge Cases

This is a little boring and pedantic but whatever, here we go:

## Player is missing expected items

* Player saves an inventory layout
* Player drops item(s)
* Player loads inventory layout

Player is shown a message:

```
InventoryBuddy: [WARNING] Items missing from inventory: GOLDEN_APPLE, TNT
```

## Player has unexpected items

* Player saves an inventory layout
* Player gathers more items
* Player loads inventory layout

Player is shown a message:

```
InventoryBuddy: [WARNING] Unexpected items: COBBLESTONE
```

The unexpected items will be placed in empty slots.

## Player has duplicate items

This is fine but if you are carrying two axes and you are wielding a favorite axe, it may not necessarily appear where you expect.

# Building

```
mvn clean ; mvn package
```

# Install

```
cp target/InventoryBuddy-1.0.0.jar ~/minecraft/plugins/
```

# Etc

Created by [harrison.page](https://harrison.page) on 9-Apr-2022
