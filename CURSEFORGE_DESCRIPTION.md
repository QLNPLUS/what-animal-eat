# What Animals Eat

## Short Description

Configure animal breeding foods and player-held attractants with JSON, live reload, KubeJS scripting, and persistent per-entity overrides.

## Full Description

What Animals Eat lets modpack authors customize animal breeding foods and player-held attractants through simple, readable JSON configuration files.

### Features

- Configure breeding foods with `what_animals_eat_food.json`.
- Configure player-held attractants with `what_animals_eat_attractant.json`.
- Support multiple items for one animal.
- Support item IDs, item tags, and KubeJS item stacks.
- Automatically generate default configurations on server startup.
- Automatically append newly registered animal entities to existing JSON files.
- Preserve existing entity entries and custom item lists.
- Apply configuration changes with `/reload`.
- Modify rules immediately through KubeJS.
- Store per-entity overrides in persistent entity NBT.
- Provide before-feeding and after-feeding KubeJS events.
- Available for Minecraft Forge 1.20.1 and NeoForge 1.21.1.

### Configuration

Food configuration:

`config/what_animals_eat_food.json`

Attractant configuration:

`config/what_animals_eat_attractant.json`

Example:

```json
{
  "minecraft:cow": [
    "minecraft:apple",
    "minecraft:golden_carrot"
  ],
  "minecraft:chicken": [
    "#minecraft:seeds"
  ],
  "*": [
    "minecraft:beetroot"
  ]
}
```

Each key is an entity ID and each value is an array of item IDs. Use `*` to define a rule for all supported animals. Use `#tag_id` to reference an item tag.

Food rules replace an animal's default breeding foods. Attractant rules define which items held by a player make the animal follow them.

Run `/reload` after editing either JSON file to apply the changes without restarting the server.

### Automatic Configuration Updates

When the server starts, the mod scans registered animal entities and detects their default breeding foods and attractants.

If a newly installed mod adds an animal with detectable default rules, its entity ID is automatically appended to the appropriate JSON file with the detected defaults. Existing entity entries and custom item lists are never overwritten.

The old `what_animals_eat.json` and `what_animals_eat-common.toml` files are migrated to `what_animals_eat_food.json` when possible.

### KubeJS Examples

Place scripts in the `server_scripts` folder.

#### Change Breeding Foods

```js
// Change the breeding foods for every cow.
WhatAnimalsEat.setBreedingFoods(
  'minecraft:cow',
  ['minecraft:apple', 'minecraft:golden_carrot']
)

// Add beetroot as an additional breeding food for every pig.
WhatAnimalsEat.addBreedingFood(
  'minecraft:pig',
  'minecraft:beetroot'
)
```

#### Change Attractants

```js
// Change the items that attract every cow.
WhatAnimalsEat.setAttractant(
  'minecraft:cow',
  ['minecraft:apple', 'minecraft:golden_carrot']
)

// Add beetroot as an attractant for every pig.
WhatAnimalsEat.addAttractant(
  'minecraft:pig',
  'minecraft:beetroot'
)
```

#### Modify One Individual Entity

Passing `event.animal` changes only the individual entity that was fed. The override is stored in the entity's persistent NBT and survives world saves and server restarts.

```js
WhatAnimalsEatEvents.afterAnimalFed(event => {
  console.log(
    event.player.name +
    ' fed ' +
    event.animal.id +
    ' with ' +
    event.item.id
  )

  // Only this individual animal is changed.
  WhatAnimalsEat.setBreedingFoods(
    event.animal,
    ['minecraft:apple', 'minecraft:golden_carrot']
  )

  // Only this individual animal is attracted by beetroot.
  WhatAnimalsEat.setAttractant(
    event.animal,
    ['minecraft:beetroot']
  )
})
```

Passing an entity ID string changes all entities of that type. Passing an animal entity object changes only that individual entity.

#### Before and After Feeding Events

```js
WhatAnimalsEatEvents.beforeAnimalFed(event => {
  console.log(
    'Player ' + event.player.name +
    ' is feeding ' + event.animal.id +
    ' with ' + event.item.id
  )

  // Cancel the feeding operation when necessary.
  // event.cancel()
})

WhatAnimalsEatEvents.afterAnimalFed(event => {
  console.log(
    'Player ' + event.player.name +
    ' fed ' + event.animal.id +
    ' with ' + event.item.id
  )
})
```

The feeding event provides:

- `event.player`: the player feeding the entity.
- `event.animal`: the animal being fed.
- `event.item`: the item being used.

#### Remove or Reset Rules

```js
// Remove one breeding food from every pig.
WhatAnimalsEat.removeBreedingFood(
  'minecraft:pig',
  'minecraft:carrot'
)

// Reset an individual breeding-food override.
WhatAnimalsEat.resetBreedingFoods(event.animal)

// Reset an individual attractant override.
WhatAnimalsEat.resetAttractant(event.animal)

// Read the currently effective rules.
console.log(WhatAnimalsEat.getBreedingFoods('minecraft:cow'))
console.log(WhatAnimalsEat.getAttractants('minecraft:cow'))
```

KubeJS changes are applied immediately on the current server. Individual entity overrides are stored in persistent entity NBT, while JSON files contain the default rules for entity types.
