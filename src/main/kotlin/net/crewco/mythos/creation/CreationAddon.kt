package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonBase
import net.crewco.mythos.api.Mythos

/**
 * Addon #1: the beginning.
 *
 * The shape every later myth copies:
 *   1. grab the core services
 *   2. register the realms, the era (naming the era that FOLLOWS it), and the roles it introduces
 *   3. wire the ACTS — triggers, rites and director fallbacks — that carry the story forward
 *   4. register one listener for the things that are genuinely engine events
 *
 * Note what is *not* here any more: not one power, not one command. The age is claimed, borne,
 * buried, forged and cut down entirely through the world. When its last objective is struck, core
 * advances to `next` — and this addon goes quiet forever, never knowing what came after it.
 */
class CreationAddon : AddonBase() {

    override fun onEnable() {
        val mythos = Mythos.from(context)

        // The cosmos. The engine builds these worlds once every addon has spoken, at the tail of
        // startup — worlds cannot be created while a server is running.
        mythos.realms.register(CreationContent.VOID)
        mythos.realms.register(CreationContent.GAIA)
        mythos.realms.register(CreationContent.TARTARUS)

        mythos.eras.register(CreationContent.ERA_OF_CHAOS)

        CreationContent.PRIMORDIALS.forEach(mythos.roles::register)
        CreationContent.TITANS.forEach(mythos.roles::register)

        // The acts: Gaia's Seed, Uranus's hands, the forge rite, the sickle — and the Director that
        // makes sure the age turns even when the house is too small to perform all of it.
        CreationRites(mythos, context).wire()

        context.registerListener(CreationListener(mythos, context))

        context.logger.info("The gap yawns. ${CreationContent.PRIMORDIALS.size} seats, ${CreationContent.TITANS.size} unborn.")
    }
}
