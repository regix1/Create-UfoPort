# Create-UfoPort

An unofficial port of [Create Fabric](https://modrinth.com/mod/create-fabric) for Minecraft 1.21 / 1.21.1.

## Installation

Download the mod from [Releases](https://github.com/vlad250906/Create-UfoPort/releases/) and install the required dependencies:

- [Fabric Loader](https://fabricmc.net/use/installer/) (>= 0.16.0)
- [Fabric API](https://modrinth.com/mod/fabric-api) (>= 0.100.7)
- [Forge Config API Port](https://modrinth.com/mod/forge-config-api-port) (>= 21.0.5)

## Cobblemon Integration

When [Cobblemon](https://modrinth.com/mod/cobblemon) (>= 1.7.3) is installed alongside Create-UfoPort, a full compat layer activates automatically. All integration is optional and has zero impact when Cobblemon is absent.

### Contraption Fixes

- **Harvester** recognizes apricorn trees as crops. Mature apricorns are harvested and reset to age 0 instead of being destroyed, enabling fully automated apricorn farms.
- **Deployer** with an empty hand can interact with apricorn blocks using right-click, allowing stationary apricorn-picking setups.
- **Apricorn blocks** are registered as tree attachments so tree-felling contraptions handle them correctly.

### Mechanical Arm Support

Create's Mechanical Arm can target several Cobblemon blocks:

| Block | Arm Behavior |
|---|---|
| Fossil Analyzer | Deposit only (insert fossils) |
| Restoration Tank | Deposit only (insert materials) |
| Pasture Block | Deposit only |
| Display Case | Full insert / extract |
| Gilded Chest (all 8 variants) | Full insert / extract |
| Gimmighoul Chest | Full insert / extract |

### Recipes (45 total)

All recipes use `fabric:load_conditions` and only appear when Cobblemon is loaded.

**Crushing (23 recipes)**
- 10 evolution stone ores + 10 deepslate variants: 1x guaranteed stone, 75% bonus stone, 75% experience nugget.
- 3 tumblestone clusters: 4x tumblestone + 25% bonus.

**Milling (7 recipes)**
- Each apricorn color mills into 2x matching dye + 10% seed chance.

**Mixing (5 recipes)**
- Heated mixing of berries + water produces Cobblemon medicines (Potion, Super Potion, Antidote, Burn Heal, Paralyze Heal).

**Sequenced Assembly (7 recipes)**
- Tier 1 Pokeball production line: press copper into sheet, deploy two apricorns, press to compact. Yields 4x Pokeballs per run.
- Colors: Poke Ball, Citrine Ball, Verdant Ball, Azure Ball, Roseate Ball, Slate Ball, Premier Ball.

**Splashing (3 recipes)**
- Fan-washing raw tumblestone blocks into polished variants.

### Breeding Farm Support

Cobblemon eggs that spawn near Pasture Blocks are standard item entities. Create's belts, funnels, depots, and chutes all pick them up automatically with no extra configuration needed. Place a belt loop or funnel ring around your Pasture Block for hands-free egg collection.

Pasture Blocks, Fossil Analyzers, Restoration Tanks, and PCs are marked non-movable to prevent contraptions from breaking their active state.

## Known Incompatibilities

- [Farmer's Delight](https://modrinth.com/mod/farmers-delight-refabricated)

If you find any incompatibility with a mod, feel free to open an issue or send a pull request.

## Building from Source

```
git clone https://github.com/vlad250906/Create-UfoPort.git
cd Create-UfoPort
./gradlew build
```

The output jar is in `build/libs/`.

## Credits

- [Create Fabric](https://github.com/Fabricators-of-Create/Create)
- [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib)
- [Flywheel](https://github.com/Engine-Room/Flywheel)
- [Milk Lib](https://github.com/TropheusJ/milk-lib)
- [Registrate Refabricated](https://github.com/Fabricators-of-Create/Registrate-Refabricated)
