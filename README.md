# Treebound (Fabric, Minecraft 1.21.1)
By gefstudiosyt. A tree-race mod: be reincarnated as a tree and grow. Original mod, not tied to any other game or book.

## Get the mod file (no coding)
1. Make a new empty GitHub repository.
2. Upload the `src` folder (drag the folder itself), `build.gradle`, `settings.gradle`, `gradle.properties`, `README.md`.
3. Add file > Create new file, name `.github/workflows/build.yml`, paste in the workflow text, commit.
4. Actions > wait for the green check > download Artifacts > treebound-mod, unzip, put the `.jar` in `mods` (needs Fabric API).

## Play
Craft a **Seed of Rebirth** (shapeless): oak sapling + diamond + 2 bone meal. Right-click it to become a tree.
- You start as a **Sapling**, then grow into a **Young Tree**, **Great Tree** and **World Tree**.
- **Grow** by standing in sunlight (or rain), and faster by being **rooted**: sneak without moving on dirt or grass.
- Sunlight feeds you; Young Tree and up regenerate in the sun. Each stage: +8 max health and +3 armor (bark). You move 10% slower.
- Rooted = Resistance II + Regeneration II. Fire hurts you extra.
- **Great Tree and up:** nearby crops and plants grow on their own.
- **Root Snare** (Young Tree+): right-click the seed to tangle and slow nearby monsters. Sneak + right-click shows your growth.
- Your race is kept after death.
- Operators: `/treerace xp <amount>` and `/treerace reset` to test.

Settings: `config/treebound.json`.
