# Frequency Works — Minecraft 1.21.1 NeoForge mod

**Current development build: Alpha A5D (0.3.0-alpha5d).** Target: NeoForge 21.1.265 / ATM10.

Frequency Works experiments with persistent shared item storage across frequency-bound Transfer Buffers and item-transfer Ports. It is **alpha test software**, not safe for valuable survival worlds yet.

**A5D focuses on restoring the standard Minecraft chest-style player inventory** in the Transfer Buffer screen: normal three-row player inventory and hotbar, standard slot controls, while custom frequency storage continues to support long item quantities, sorting and row-by-row scrolling. [Details](docs/ALPHA_A5D_VANILLA_INVENTORY.md).

Supported in current alpha: Buffer/Port frequency behavior, configurable Tag Cards, and Creative + OP Frequency Debugger. **Not yet functional:** Item Sequencer, Sequence Tablets, Interaction and Redstone Cards, Frequency Monitor. See [implementation status](docs/IMPLEMENTATION_STATUS.md).

## Compile

GitHub Actions (`.github/workflows/build.yml`) runs standalone tests and `gradle build` with Java 21 and Gradle 8.10.2. A successful run uploads the compiled mod as `frequencyworks-neoforge-jar`.

Standalone checks:

```sh
bash scripts/test-core.sh
bash scripts/test-bridge.sh
bash scripts/test-buffer-gui.sh
bash scripts/test-security.sh
python3 scripts/check-resources.py
```

Never run new alpha versions on an important Minecraft world without a backup.
