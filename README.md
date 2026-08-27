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

The default list is empty, so the mod does nothing until a rule is added. Only entities extending Minecraft's `Animal` class are handled.
