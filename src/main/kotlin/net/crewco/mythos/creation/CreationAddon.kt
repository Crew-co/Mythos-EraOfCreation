package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonBase
import net.crewco.mythos.api.Mythos

/**
 * Addon #1: the beginning.
 *
 * The shape every later myth copies:
 *   1. grab the core services
 *   2. register an era (with the id of the era that FOLLOWS it)
 *   3. register the roles that era introduces
 *   4. register the powers those roles grant
 *   5. register one listener that turns player actions into completed objectives
 *
 * When its last objective is struck, core advances the world to `next` — and this
 * addon goes quiet forever, without ever knowing what came after it.
 */
class CreationAddon : AddonBase() {

    override fun onEnable() {
        val mythos = Mythos.from(context)

        // The cosmos. The engine builds these worlds once every addon has spoken, at the
        // tail of startup — worlds cannot be created while a server is running.
        mythos.realms.register(CreationContent.VOID)
        mythos.realms.register(CreationContent.GAIA)
        mythos.realms.register(CreationContent.TARTARUS)

        mythos.eras.register(CreationContent.ERA_OF_CHAOS)

        CreationContent.PRIMORDIALS.forEach(mythos.roles::register)
        CreationContent.TITANS.forEach(mythos.roles::register)

        mythos.powers.register(BirthPower(mythos, context))
        mythos.powers.register(ImprisonPower(mythos, context))
        mythos.powers.register(SicklePower(mythos, context))
        mythos.powers.register(VeilPower(context))
        mythos.powers.register(UnmakePower(mythos, context))

        context.registerListener(CreationListener(mythos, context))

        context.logger.info("The gap yawns. ${CreationContent.PRIMORDIALS.size} seats, ${CreationContent.TITANS.size} unborn.")
    }
}
