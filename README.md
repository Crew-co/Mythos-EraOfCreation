# EraOfCreation

**Chaos, the Primordials, and the sickle that ended the sky.**

A story addon for the [Mythos](https://github.com/Crew-co/Mythos) engine — the era **`chaos`**.

📖 **[Read the wiki →](https://github.com/Crew-co/EraOfCreation/wiki)** · roles, powers, extension points

## How it's played

Not one `/power`, not one command. Gaia bears a Titan by **using the Seed of Earth on the ground**; Uranus buries a child by **right-clicking them**; the sickle is **forged** in two acts and **swung** to end the Sky; Nyx vanishes on a **sneak**. Every required beat also has a Director fallback, so the age turns even with a handful of players and no Uranus. Full table: **[How the age is played](wiki/Powers.md)**.

## Install

Drop the jar in `plugins/Mythos/addons/`. That's it — no `depends:`, no configuration, no load order
to worry about. The engine wires the era chain at bootstrap.

## Build

```bash
# once, in the Mythos repo:  ./gradlew publishApiLocally
./gradlew build          # → build/libs/EraOfCreation-0.1.0.jar
./gradlew deployAddon    # set testServerPath in ~/.gradle/gradle.properties
```

```kotlin
compileOnly("net.crewco:mythos-addon-api:0.1.4")   // the only dependency
```

`compileOnly`, never `implementation` — a shaded copy of the API is a different class with the same
name, and the addon will silently refuse to load.

## Testing it alone

`/mythos dev` — every crowd-sized number in this chapter becomes 1. You are one person; the story was
written for a hundred.

---

*Part of [Mythos](https://github.com/Crew-co/Mythos): a Greek mythology engine for Folia, where every
story is a separate jar.*
