package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.event.DivineDeathEvent
import net.crewco.mythos.api.event.EraAdvancedEvent
import net.crewco.mythos.api.event.RoleClaimedEvent
import net.crewco.mythos.api.event.RoleReleasedEvent
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.entity.Player

/**
 * The first age, watching itself happen.
 *
 * Everything here hangs off events that MythosCore fires. This addon never once
 * reaches into another addon — which is exactly why the Titanomachy can be written
 * later, by someone else, and still pick up the moment this one puts down.
 */
class CreationListener(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Listener {

    private val era = CreationContent.ERA

    // ---- the age assembling itself ------------------------------------------

    @EventHandler
    fun onClaimed(event: RoleClaimedEvent) {
        when (event.role.id) {
            "chaos" -> mythos.eras.complete(era, "chaos_stirs", "Chaos opened its eyes")
            "eros" -> mythos.eras.complete(era, "first_love", "desire entered the world")
        }
        // Earth and Sky, both worn: the story can actually start.
        if (mythos.roles.holders("gaia").isNotEmpty() && mythos.roles.holders("uranus").isNotEmpty()) {
            mythos.eras.complete(era, "earth_and_sky", "Sky lay over Earth")
            mythos.roles.holders("uranus").mapNotNull { Bukkit.getPlayer(it) }.forEach { uranus ->
                context.schedulers.entity(uranus) {
                    uranus.sendMessage(mm("<aqua><i>She will bear children. They will be stronger than you."))
                    uranus.sendMessage(mm("<gray>You have <white>/power imprison<gray>. Everyone knows how this goes."))
                }
            }
        }
    }

    // ---- the sickle ---------------------------------------------------------

    /**
     * Runs at NORMAL, *before* core's lethal-damage check at HIGH — so by the time
     * core asks "was that fatal?", the answer is yes.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onSickleStrike(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return
        if (mythos.roles.roleOf(victim.uniqueId)?.id != "uranus") return
        if (!SicklePower.isSickle(attacker.inventory.itemInMainHand, context)) return

        event.damage = 10_000.0
        attacker.sendMessage(mm("<gray><i>The sickle knows what it is for."))
    }

    /**
     * Core has already decided this blow *could* kill a Primordial (Titan-tier can,
     * by the default rules). We override that: nothing unmakes the Sky except the
     * thing the Earth made for it.
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onDivineDeath(event: DivineDeathEvent) {
        if (event.victimRole.id != "uranus") return
        val killer = event.killer

        val wieldsSickle = killer != null && SicklePower.isSickle(killer.inventory.itemInMainHand, context)
        if (!wieldsSickle) {
            event.isCancelled = true
            killer?.sendMessage(mm("<gray>He does not even notice. <dark_gray><i>He is the sky."))
            return
        }

        event.isCancelled = false
        event.unmakes = true
    }

    /** Sky is cut from Earth. The age ends — and the next one has already been written by another jar. */
    @EventHandler
    fun onReleased(event: RoleReleasedEvent) {
        if (event.role.id != "uranus") return

        Bukkit.getServer().sendMessage(mm(""))
        Bukkit.getServer().sendMessage(mm("<gray>The sky is cut from the earth, and staggers back, and does not come down again."))
        Bukkit.getServer().sendMessage(mm("<dark_gray><i>Where the blood fell, things grew that nobody wanted."))
        Bukkit.getServer().sendMessage(mm(""))

        // Everything he buried comes back up.
        context.schedulers.global {
            CreationContent.TITANS.forEach { titan ->
                mythos.roles.holders(titan.id).forEach { uuid ->
                    val profile = mythos.profiles.profile(uuid)
                    if (profile.hasFlag("creation.imprisoned")) {
                        profile.setFlag("creation.imprisoned", null)
                        Bukkit.getPlayer(uuid)?.let { freed ->
                            val surface = freed.world.spawnLocation
                            freed.teleportAsync(surface).thenRun {
                                context.schedulers.entity(freed) {
                                    freed.sendMessage(mm("<gold>The weight lifts. You climb out of your mother, into the light."))
                                }
                            }
                        }
                    }
                }
            }
            // This is the last required beat of the age: core will now advance the
            // world to whatever era declared itself next — the Titanomachy.
            mythos.eras.complete(era, "the_unmaking", "the sickle fell")
        }
    }

    // ---- opening titles ------------------------------------------------------

    @EventHandler
    fun onEraAdvanced(event: EraAdvancedEvent) {
        if (event.to.id != era) return
        context.schedulers.globalDelayed(60) {
            Bukkit.getOnlinePlayers().forEach { player ->
                context.schedulers.entity(player) {
                    player.sendMessage(mm("<gray>Eight names exist, and no one is wearing them. <white>/claim"))
                    player.sendMessage(mm("<dark_gray><i>Everyone else waits in the dark. That is not a punishment; it is a queue."))
                }
            }
        }
    }
}
