package net.crewco.mythos.creation

import net.crewco.mythos.api.era.EraDefinition
import net.crewco.mythos.api.era.Objective
import net.crewco.mythos.api.role.ClaimResult
import net.crewco.mythos.api.role.ClaimRule
import net.crewco.mythos.api.role.ClaimRules
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.api.role.Succession

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
        // The only gate on the first age is being *there* — the world is empty and
        // someone has to be first. Turn `claiming.require-permission` on in
        // MythosCore's config.yml if you want a hand-picked pantheon instead.
        claimRules = listOf(ClaimRules.duringEra(ERA)),
        succession = succession,
    )

    val PRIMORDIALS = listOf(
        primordial(
            "chaos", "Chaos", "<dark_purple>",
            listOf("the void", "the gap"),
            listOf("You are the space where things are not yet.", "Everything that follows is a wound in you."),
            powers = listOf("unmake"),
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
        ),
        primordial("erebus", "Erebus", "<black>", listOf("darkness", "shadow"), listOf("You are the dark that is not merely an absence of light.")),
        primordial("tartarus", "Tartarus", "<dark_red>", listOf("the abyss", "the prison"), listOf("You are as far beneath Hades as the earth is beneath the sky.", "Things are put into you. They do not come out.")),
        primordial("pontus", "Pontus", "<blue>", listOf("sea", "depth"), listOf("The sea, before anyone thought to rule it.")),
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
