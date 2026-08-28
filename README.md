# What Animals Eat

This branch targets Minecraft 1.21.1 with NeoForge 21.1.248.

The mod changes breeding foods through the JSON config file:

`config/what_animals_eat.json`

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

Each JSON key is an entity ID and its value is an array of item IDs. Use `#tag_id` for an item tag and `*` to apply a rule to every animal. Multiple foods are listed as separate array values. Once an animal has a rule, its vanilla breeding foods are replaced by the configured foods. Run `/reload` after changing the config to apply it without restarting the server.

When this file does not exist, the first server start scans all registered entities and items and writes the detected breeding rules with one JSON entry per line. If the old `what_animals_eat-common.toml` exists, it is read once and migrated to JSON. An existing JSON file is preserved and read as-is.

Only entities extending Minecraft's `Animal` class are handled. KubeJS is optional; this branch is compatible with KubeJS NeoForge 2101.7.2 or newer. With KubeJS installed, use `WhatAnimalsEatEvents.beforeAnimalFed` and `WhatAnimalsEatEvents.afterAnimalFed`.

```js
// server_scripts/animal_food.js
WhatAnimalsEat.setBreedingFoods('minecraft:cow', ['minecraft:apple', 'minecraft:golden_carrot'])
WhatAnimalsEat.addBreedingFood('minecraft:pig', 'minecraft:beetroot')

WhatAnimalsEatEvents.beforeAnimalFed(event => {
  if (event.animal.id == 'minecraft:cow') {
    // event.cancel() prevents the item from being consumed and the animal from entering love mode.
  }
})

WhatAnimalsEatEvents.afterAnimalFed(event => {
  console.log(event.player.name + ' fed ' + event.animal.id + ' with ' + event.item.id)
})
```

To change only the fed animal type after a successful feeding, pass the event entity directly. Other animal types are unchanged:

```js
WhatAnimalsEatEvents.afterAnimalFed(event => {
  Client.tell(event.player.username + ' fed ' + event.animal + ' with ' + event.item)
  WhatAnimalsEat.setBreedingFoods(event.animal, ['minecraft:apple', 'minecraft:golden_carrot'])
})
```

The API changes rules immediately for the current server. `setBreedingFoods`, `addBreedingFood`, `removeBreedingFood`, `resetBreedingFoods`, and `getBreedingFoods` accept either an entity ID string or an animal entity object. `setBreedingFoods` accepts one item ID, an array of item IDs, an item tag such as `#minecraft:flowers`, or KubeJS item stacks. `resetBreedingFoods` removes the runtime override and returns to the config rule.
