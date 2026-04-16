# mageswand-spell-pack

This is the recovered external spell-jar project for MagesWand.
It is not a Paper plugin and does not need a `plugin.yml`.
Everything in `src/main/java` is compiled into one jar.

Build it from the repository root with:

```bash
./gradlew :mageswand-spell-pack:build
```

Drop the resulting jar into:

```text
plugins/mageswand/spells/
```

Then configure `plugins/mageswand/wand-fuels.json` with executor class names.
A ready-made example mapping lives in `../examples/wand-fuels.with-spell-pack.json`.
