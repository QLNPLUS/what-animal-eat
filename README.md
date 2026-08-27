# What Animals Eat

This branch targets Minecraft 1.20.1 with Forge 47.x.

The mod changes breeding foods through the common config file:

`config/what_animals_eat-common.toml`

Example:

```toml
breedingRules = [
  "minecraft:cow=minecraft:apple",
  "minecraft:pig=minecraft:carrot|minecraft:potato",
  "minecraft:chicken=#minecraft:flowers",
  "*=minecraft:golden_carrot"
]
```

Each rule uses `entity_id=food_id`. Use `#tag_id` for an item tag and `*` to apply a rule to every animal. Multiple rules for the same animal are merged. Once an animal has a rule, its vanilla breeding foods are replaced by the configured foods. Restart the server after changing the config.

When this file does not exist, the first server start scans all registered entities and items and writes the detected breeding rules. An existing file is always preserved and read as-is.

Only entities extending Minecraft's `Animal` class are handled. KubeJS is optional. With KubeJS installed, use `WhatAnimalsEatEvents.beforeAnimalFed` and `WhatAnimalsEatEvents.afterAnimalFed`.

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

The API changes rules immediately for the current server. `setBreedingFoods` accepts one item ID, an array of item IDs, an item tag such as `#minecraft:flowers`, or KubeJS item stacks. `resetBreedingFoods` removes the runtime override and returns to the config rule.
