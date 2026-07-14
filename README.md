# EraOfCreation

Chaos, the Primordials, and the sickle that ended the sky. **Story #1 — the era `chaos`.**

A story addon for the [Mythos](https://github.com/Crew-co/Mythos) engine, built on
[FoliaAddonTemplate](https://github.com/Crew-co/FoliaAddonTemplate). Its own repo, its
own jar, its own release cycle — and exactly one dependency:

```kotlin
compileOnly("net.crewco:mythos-addon-api:0.1.2")
```

No `depends:` in addon.yml. The engine is the plugin, not an addon: it's already running
before this jar is loaded.

## Build

```bash
# once, in the Mythos repo:  ./gradlew publishApiLocally
./gradlew build          # → build/libs/EraOfCreation-0.1.0.jar
./gradlew deployAddon    # set testServerPath in ~/.gradle/gradle.properties first
```

Drop the jar in `plugins/Mythos/addons/`. `/addons` should list it.

## What it registers

- **Era** `chaos` → declares `next = "titanomachy"`. It does not know or care what
  registers that.
- **8 Primordials, claimed** — Chaos, Gaia, Uranus, Nyx, Erebus, Tartarus, Pontus,
  Eros. Eight seats. Everyone else is a spirit, in the queue, waiting for one to fall
  vacant.
- **12 Titans, *not* claimed** — their `ClaimRule` denies everyone, always. The only
  way to become Kronos is for whoever holds **Gaia** to reach into the spirit world and
  pull you out of it: `/power birth <spirit> <titan>`.
- **Powers** — `birth` (Gaia makes a spirit real) · `imprison` (Uranus buries a child at
  bedrock) · `sickle` (Gaia can only forge it *after* he does) · `veil` (Nyx) ·
  `unmake` (Chaos).

## The shape

Uranus is unkillable — by anything except the sickle. That isn't hard-coded in core:
core fires `DivineDeathEvent` with the blow pre-cancelled, and this addon un-cancels it
when the killer is holding a specific PDC-tagged hoe. The same hook is Achilles' heel,
ten addons later.

When the sickle falls, `the_unmaking` — the era's last required objective — completes,
and core advances the world to `titanomachy`. This addon then goes quiet forever,
without ever learning what came next.

`compileOnly`, never `implementation` — a shaded copy of the API is a different class
with the same name, and the addon silently refuses to load.
