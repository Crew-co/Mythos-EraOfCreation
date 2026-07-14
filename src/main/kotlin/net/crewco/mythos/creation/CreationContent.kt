package net.crewco.mythos.creation

import net.crewco.mythos.api.era.EraDefinition
import net.crewco.mythos.api.era.Objective
import net.crewco.mythos.api.realm.RealmDefinition
import net.crewco.mythos.api.realm.RealmKind
import net.crewco.mythos.api.realm.RealmRules
import org.bukkit.potion.PotionEffectType
import net.crewco.mythos.api.role.ClaimResult
import net.crewco.mythos.api.role.ClaimRule
import net.crewco.mythos.api.role.ClaimRules
import net.crewco.mythos.api.role.Endurance
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.api.role.Succession
import net.crewco.mythos.api.story.beats
import net.crewco.mythos.api.story.line
import net.crewco.mythos.api.story.pause
import net.crewco.mythos.api.story.title

/**
 * The cast and the score of the first age.
 *
 * Two different gates are on show here, and they're the whole point of the design:
 *
 *  - **The Primordials are CLAIMED.** Eight seats, and once they're gone they're
 *    gone — everyone else is a spirit, in the queue, waiting.
 *  - **The Titans are NOT claimed. They are BORN.** No player can `/claim kronos`;
 *    the only way into a Titan is for whoever holds Gaia to reach into the spirit
 *    world and pull someone out of it (`/power birth <spirit> <titan>`).
 *
 * That second one is what makes the server a story instead of a lobby: one player's
 * power over another player's *existence*, which is exactly the deal Greek myth
 * is actually about.
 */
object CreationContent {

    const val ERA = "chaos"

    // ---- the cosmos ---------------------------------------------------------

    /**
     * **The Void is a world.** Not a region with a fancy name — an empty world, generated
     * with nothing in it, because nothing has been made yet.
     *
     * Every spirit in the Age of Chaos stands here. There is no ground, no time, no weather,
     * no Tuesday. When the age turns and there is finally a world to haunt, they leave.
     */
    val VOID = RealmDefinition(
        id = "void",
        displayName = "The Gap",
        kind = RealmKind.VOID,
        access = RealmRules.any(RealmRules.SPIRITS, RealmRules.roles("chaos", "nyx", "erebus")),
        refusal = "<dark_gray><i>There is nothing here for you. There is nothing here at all.",
        entryLore = listOf(
            "<dark_gray><i>Not darkness — darkness is a thing.",
            "<dark_gray><i>You are in the space where things are not yet.",
        ),
        flight = true,
        still = true, // no time, no weather, no mobs. The Void does not have a Tuesday.
        platformRadius = 8,
        platformMaterial = "BLACK_CONCRETE",
    )

    /** Gaia is the world the server already has. Nobody generates the ground; it was here first. */
    val GAIA = RealmDefinition(
        id = "gaia",
        displayName = "Gaia",
        kind = RealmKind.PRIMARY,
        entryLore = listOf("<green><i>Ground. Under everything, and under everyone."),
    )

    /**
     * **Tartarus.** As far below Hades as the earth is below the sky, and it is a separate
     * world because that is what it *is*.
     *
     * `SENT_ONLY`: you cannot walk in. You are *put* here — by Uranus, by Zeus, by the engine.
     * Things go into Tartarus. They do not come out on their own.
     */
    val TARTARUS = RealmDefinition(
        id = "tartarus",
        displayName = "Tartarus",
        kind = RealmKind.NETHER,
        // The gods may go down — somebody has to be the jailer, and later somebody has to go
        // and let the Cyclopes out. The IMPRISONED may be here, but the leash is what stops
        // them leaving: being allowed somewhere and being able to get out are different.
        access = RealmRules.any(
            RealmRules.DIVINE,
            RealmRules.flagged("creation.imprisoned"),
            RealmRules.flagged("titanomachy.imprisoned"),
            RealmRules.roles("tartarus"),
        ),
        refusal = "<dark_red>You cannot get in. <dark_gray><i>That is not the problem with Tartarus.",
        entryLore = listOf(
            "<dark_red><i>An anvil would fall for nine days to get here.",
            "<dark_gray><i>Things are put into you. They do not come out.",
        ),
        ambient = listOf(PotionEffectType.BLINDNESS, PotionEffectType.MINING_FATIGUE),
    )

    /** Titans cannot be claimed. Gaia bears them, or they don't exist. */
    private val BORN_NOT_CLAIMED = ClaimRule { _, _ ->
        ClaimResult.Deny("Titans are not claimed. They are born — and Gaia has not borne you.")
    }

    val ERA_OF_CHAOS = EraDefinition(
        id = ERA,
        displayName = "The Age of Chaos",
        order = 0,
        next = "titanomachy",
        subtitle = "before anything, the gap",
        lore = listOf(
            "First there was Chaos — not disorder, but a yawning emptiness.",
            "Then Earth, broad-breasted, the ever-sure foundation.",
            "And Earth bore Sky to cover her, equal to herself.",
        ),
        // The curtain going up. The engine plays these beat by beat, and holds the world
        // still while they land — nobody can claim anything mid-scene.
        prologue = beats {
            pause(20)
            line("<dark_gray><i>Before anything, there was the gap.", delayTicks = 40)
            line("<dark_gray><i>Not darkness — darkness is a thing. This was the absence of things.", delayTicks = 60)
            pause(30)
            line("<gray>And then, for no reason anyone will ever record: <white>something.", delayTicks = 50)
            pause(40)
            line("<white>Eight names exist. Nobody is wearing them. <gold>/claim", delayTicks = 20)
            line("<dark_gray><i>Everyone else waits in the dark. That is not a punishment. It is a queue.", delayTicks = 30)
        },

        // The curtain coming down. This is the last thing the Age of Chaos ever says,
        // and it plays *before* the Titanomachy's prologue — so the story finishes
        // instead of being trampled by the next one starting.
        epilogue = beats {
            pause(30)
            title(
                "<dark_red>The Sky Is Cut From The Earth",
                "<gray>and staggers back, and does not come down again",
                delayTicks = 20,
                sound = "minecraft:entity.wither.death",
            )
            pause(60)
            line("<gray>Where the blood fell on the earth, things grew that nobody wanted:", delayTicks = 50)
            line("<dark_gray><i>the Furies, who remember every oath.", delayTicks = 45)
            line("<dark_gray><i>the Giants, who will try this again.", delayTicks = 45)
            line("<dark_gray><i>and, out of the foam where it fell on the sea — something beautiful, and much worse.", delayTicks = 55)
            pause(50)
            line("<gray>The son who did it is holding the sickle.", delayTicks = 50)
            line("<gray>He has learned exactly one lesson from all this, and it is the wrong one.", delayTicks = 60)
            pause(60)
        },

        objectives = listOf(
            Objective("chaos_stirs", "Something stirs in the gap"),
            Objective("earth_and_sky", "Earth is covered by Sky"),
            Objective("children_born", "Gaia bears six children to Uranus"),
            Objective("the_imprisonment", "Uranus casts his children into Tartarus"),
            Objective("the_sickle", "Gaia forges a sickle of grey adamant"),
            Objective("the_unmaking", "Sky is cut from Earth", hidden = true),
            Objective("first_love", "Eros walks among them", optional = true),
        ),
    )

    // ---- the eight seats of the first age -----------------------------------

    private fun primordial(
        id: String,
        name: String,
        color: String,
        domains: List<String>,
        lore: List<String>,
        powers: List<String> = emptyList(),
        succession: Succession = Succession.QUEUE,
        /**
         * ETERNAL: still on stage when the story moves on (Gaia is the ground under
         * Troy; Tartarus is where they'll put Kronos).
         * ERA: their part is played. When the age ends they go back to the spirit
         * world with an epithet and a pocketful of essence, and the name is sealed
         * until some later myth reopens it.
         */
        endurance: Endurance = Endurance.ETERNAL,
    ) = RoleDefinition(
        id = id,
        displayName = name,
        tier = RoleTier.PRIMORDIAL,
        era = ERA,
        domains = domains,
        maxHolders = 1,
        color = color,
        lore = lore,
        powers = powers,
        endurance = endurance,
        // sinceEra, NOT duringEra: the Primordials persist. Gaia is still the ground
        // under the Trojan War — if her seat falls vacant in a later age, someone must
        // still be able to take it. `duringEra` would seal her out of her own world the
        // moment the story moved on.
        //
        // The only gate on the first age is being *there*: the world is empty and
        // someone has to be first. Turn `claiming.require-permission` on in MythosCore's
        // config.yml if you want a hand-picked pantheon instead.
        claimRules = listOf(ClaimRules.sinceEra(ERA)),
        succession = succession,
    )

    val PRIMORDIALS = listOf(
        primordial(
            "chaos", "Chaos", "<dark_purple>",
            listOf("the void", "the gap"),
            listOf("You are the space where things are not yet.", "Everything that follows is a wound in you."),
            powers = listOf("unmake"),
            endurance = Endurance.ERA, // nothing in the Titanomachy is about the void
        ),
        primordial(
            "gaia", "Gaia", "<green>",
            listOf("earth", "birth"),
            listOf("You are the ground under all of it.", "Everything that lives, lives on you. Everything that suffers, suffers on you."),
            powers = listOf("birth", "sickle"),
        ),
        primordial(
            "uranus", "Uranus", "<aqua>",
            listOf("sky", "dominion"),
            listOf("You cover the Earth entirely. Nothing escapes you.", "You will be a tyrant. It is written. Try anyway."),
            powers = listOf("imprison"),
            // Once the sickle falls, the sky is sundered and never worn again.
            succession = Succession.CLOSED,
        ),
        primordial(
            "nyx", "Nyx", "<dark_gray>",
            listOf("night", "fear"),
            listOf("Even Zeus will fear you, one day.", "You are older than his fear."),
            powers = listOf("veil"),
            endurance = Endurance.ERA, // she matters again much later — a myth can reopen her
        ),
        primordial(
            "erebus", "Erebus", "<black>", listOf("darkness", "shadow"),
            listOf("You are the dark that is not merely an absence of light."),
            endurance = Endurance.ERA,
        ),
        primordial("tartarus", "Tartarus", "<dark_red>", listOf("the abyss", "the prison"), listOf("You are as far beneath Hades as the earth is beneath the sky.", "Things are put into you. They do not come out.")),
        primordial(
            "pontus", "Pontus", "<blue>", listOf("sea", "depth"),
            listOf("The sea, before anyone thought to rule it."),
            endurance = Endurance.ERA, // Poseidon is coming, and he doesn't share
        ),
        // Eros stays. Desire does not go out of fashion, and the Iliad is his fault.
        primordial("eros", "Eros", "<light_purple>", listOf("desire", "generation"), listOf("Nothing would ever have made anything else, without you.")),
    )

    // ---- the twelve who must be born ----------------------------------------

    private fun titan(id: String, name: String, domains: List<String>) = RoleDefinition(
        id = id,
        displayName = name,
        tier = RoleTier.TITAN,
        era = ERA,
        domains = domains,
        maxHolders = 1,
        color = "<gold>",
        lore = listOf("A child of Earth and Sky. Your father is afraid of you, and he is right to be."),
        claimRules = listOf(BORN_NOT_CLAIMED),
        succession = Succession.QUEUE,
        // ETERNAL, emphatically: the Titans don't retire when the Age of Chaos ends.
        // They walk straight into the next addon's story as its entire antagonist cast.
        endurance = Endurance.ETERNAL,
    )

    val TITANS = listOf(
        titan("kronos", "Kronos", listOf("time", "harvest")),
        titan("rhea", "Rhea", listOf("flow", "motherhood")),
        titan("oceanus", "Oceanus", listOf("the world-river")),
        titan("tethys", "Tethys", listOf("fresh water")),
        titan("hyperion", "Hyperion", listOf("light")),
        titan("theia", "Theia", listOf("sight", "gold")),
        titan("coeus", "Coeus", listOf("intellect", "the axis")),
        titan("phoebe", "Phoebe", listOf("prophecy")),
        titan("crius", "Crius", listOf("constellations")),
        titan("mnemosyne", "Mnemosyne", listOf("memory")),
        titan("iapetus", "Iapetus", listOf("mortality", "craft")),
        titan("themis", "Themis", listOf("law", "custom")),
    )
}
