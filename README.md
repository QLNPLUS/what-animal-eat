# What Animals Eat

This branch targets Minecraft 1.21.1 with NeoForge 21.1.248.

The mod changes breeding foods and player-held attractants through separate JSON config files:

`config/what_animals_eat_food.json`

`config/what_animals_eat_attractant.json`

Example:

```json
{
  "minecraft:chicken": [
    "#minecraft:flowers"
  ],
  "minecraft:cow": [
    "minecraft:apple"
  ],
  "minecraft:pig": [
    "minecraft:carrot",
    "minecraft:potato"
  ],
  "*": [
    "minecraft:golden_carrot"
  ]
}
```

Each JSON key is an entity ID and its value is an array of item IDs. Use `#tag_id` for an item tag and `*` to apply a rule to every animal. Multiple items are listed as separate array values. A food rule replaces the animal's vanilla breeding foods. An attractant rule controls which items in a player's hand make the animal follow the player. Run `/reload` after changing either config to apply it without restarting the server.

When either file does not exist, the first server start scans registered entities and their vanilla rules and writes the detected defaults. The old `what_animals_eat.json` and `what_animals_eat-common.toml` files are migrated to `what_animals_eat_food.json` when present. Existing JSON files are preserved and read as-is.

On every server start, registered animals are scanned again. If a newly installed mod adds an animal with detected breeding or attractant items, its missing entity ID is appended to the corresponding JSON file with the detected defaults. Existing entity entries and their custom item lists are never overwritten.

Only entities extending Minecraft's `Animal` class are handled. KubeJS is optional; this branch is compatible with KubeJS NeoForge 2101.7.2 or newer. With KubeJS installed, use `WhatAnimalsEatEvents.beforeAnimalFed` and `WhatAnimalsEatEvents.afterAnimalFed`.

```js
// server_scripts/animal_food.js
WhatAnimalsEat.setBreedingFoods('minecraft:cow', ['minecraft:apple', 'minecraft:golden_carrot'])
WhatAnimalsEat.addBreedingFood('minecraft:pig', 'minecraft:beetroot')
WhatAnimalsEat.setAttractant('minecraft:cow', ['minecraft:apple', 'minecraft:golden_carrot'])
WhatAnimalsEat.addAttractant('minecraft:pig', 'minecraft:beetroot')

WhatAnimalsEatEvents.beforeAnimalFed(event => {
  if (event.animal.id == 'minecraft:cow') {
    // event.cancel() prevents the item from being consumed and the animal from entering love mode.
  }
})

WhatAnimalsEatEvents.afterAnimalFed(event => {
  console.log(event.player.name + ' fed ' + event.animal.id + ' with ' + event.item.id)
})
```

To change only the individual entity that was fed after a successful feeding, pass the event entity directly. Other entities, including animals of the same type, are unchanged:

```js
WhatAnimalsEatEvents.afterAnimalFed(event => {
  Client.tell(event.player.username + ' fed ' + event.animal + ' with ' + event.item)
  WhatAnimalsEat.setBreedingFoods(event.animal, ['minecraft:apple', 'minecraft:golden_carrot'])
})
```

The API changes rules immediately for the current server. Passing an entity ID string changes all entities of that type; passing an animal entity object changes only that entity and saves the override in its persistent entity NBT. The same applies to attractants. `setBreedingFoods` and `setAttractant` accept one item ID, an array of item IDs, an item tag such as `#minecraft:flowers`, or KubeJS item stacks. The `add`, `remove`, `reset`, and `get` variants are available for both rule types.
