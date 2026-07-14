package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.event.DivineDeathEvent
import net.crewco.mythos.api.event.RoleClaimedEvent
import net.crewco.mythos.api.event.EraAdvancedEvent
import net.crewco.mythos.api.event.PlayerBecameSpiritEvent
import net.crewco.mythos.api.event.RoleReleasedEvent
import org.bukkit.Material
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

    /**
     * **In the Age of Chaos, the dead have nowhere to stand.**
     *
     * A spirit made during this era is put in the Void — a real, empty world — because there
     * is no world yet to haunt. Once the age turns, they stay wherever they are: the audience
     * of the Titanomachy needs to be able to *watch* it.
     */
    @EventHandler
    fun onSpirit(event: PlayerBecameSpiritEvent) {
        if (mythos.eras.currentId() != era) return
        context.schedulers.globalDelayed(20) {
            mythos.realms.send(event.player, "void", "You drift out of the world, because there isn't one.")
        }
    }

    /**
     * BUG, fixed: spirits made during the Age of Chaos were left in the Void forever.
     *
     * The Void's access rule lets spirits stay, so nothing evicted them — and the audience for
     * the Titanomachy was standing in an empty world watching nothing at all. When the age
     * turns, everyone still in the Gap is brought out into the world there now is.
     */
    @EventHandler
    fun onAgeTurns(event: EraAdvancedEvent) {
        if (event.from?.id != era) return
        context.schedulers.globalDelayed(100) {
            val void = mythos.realms.world("void") ?: return@globalDelayed
            void.players.toList().forEach { stranded ->
                mythos.realms.send(stranded, "gaia", "There is a world now. Go and watch it happen.")
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

        // The Chronicle outlives this addon. Three ages from now, a player who wasn't
        // born when this happened can type /chronicle era chaos and read who did it.
        mythos.chronicle.record(
            "story",
            "<gray>The sky was cut from the earth with a sickle of grey adamant. " +
                "<dark_gray><i>Gaia made it. One of her children was willing to use it.",
        )

        // THE SCAR. Where the sky's blood fell, the ground is changed — permanently, visibly,
        // and four addons from now Aphrodite will be claimable because of exactly this.
        event.player?.let { fallen ->
            context.schedulers.entity(fallen) {
                val ground = fallen.location.clone()
                for (x in -4..4) for (z in -4..4) {
                    if (x * x + z * z > 16) continue
                    val block = ground.clone().add(x.toDouble(), -1.0, z.toDouble()).block
                    if (block.type.isSolid) {
                        block.type = if ((x + z) % 3 == 0) Material.CRIMSON_NYLIUM else Material.RED_SAND
                    }
                }
                fallen.world.strikeLightningEffect(ground)
            }
        }

        // Everything he buried comes back up.
        context.schedulers.global {
            CreationContent.TITANS.forEach { titan ->
                mythos.roles.holders(titan.id).forEach { uuid ->
                    val profile = mythos.profiles.profile(uuid)
                    if (profile.hasFlag("creation.imprisoned")) {
                        profile.setFlag("creation.imprisoned", null)
                        Bukkit.getPlayer(uuid)?.let { freed ->
                            // Out of Tartarus, and back into the world. An actual world change.
                            mythos.realms.send(freed, "gaia", "The weight lifts. You climb out of your mother, into the light.")
                        }
                    }
                }
            }
            // This is the last required beat of the age: core will now advance the
            // world to whatever era declared itself next — the Titanomachy.
            mythos.eras.complete(era, "the_unmaking", "the sickle fell")
        }
    }

    // The opening used to be broadcast from here, which now double-speaks over the
    // era's own prologue. Anything the whole server should hear as the age begins
    // belongs in `EraDefinition.prologue` — the narrator paces it, and the world holds
    // still while it lands. A listener is for things only SOME players should hear.
}
