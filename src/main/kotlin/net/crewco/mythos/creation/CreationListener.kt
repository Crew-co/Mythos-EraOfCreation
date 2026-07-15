package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.event.DivineDeathEvent
import net.crewco.mythos.api.event.EraAdvancedEvent
import net.crewco.mythos.api.event.PlayerBecameSpiritEvent
import net.crewco.mythos.api.event.RoleClaimedEvent
import net.crewco.mythos.api.event.RoleReleasedEvent
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/**
 * The first age, watching itself happen — the parts that are *events*, not acts.
 *
 * The acted-out beats (birth, imprisonment, the forge, the strike) live in [CreationRites]. This
 * listener keeps the things that genuinely are reactions to the engine: a role being claimed, a
 * spirit being made, the Sky being cut down and the age turning to whatever jar wrote the next one.
 *
 * It still never reaches into another addon, which is why the Titanomachy can be written later, by
 * someone else, and pick up the moment this one puts down.
 */
class CreationListener(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Listener {

    private val era = CreationContent.ERA

    /** Set only when the sickle actually cuts the sky, so an abdication can't trigger the age's end. */
    @Volatile private var uranusCut = false

    // ---- the age assembling itself ------------------------------------------

    @EventHandler
    fun onClaimed(event: RoleClaimedEvent) {
        when (event.role.id) {
            "chaos" -> mythos.eras.complete(era, "chaos_stirs", "Chaos opened its eyes")
            "eros" -> mythos.eras.complete(era, "first_love", "desire entered the world")
            "gaia" -> context.schedulers.entity(event.player) {
                event.player.inventory.addItem(CreationItems.seed(context))
                event.player.sendMessage(mm("<green><i>You are the ground, and everything to come is folded up in you."))
                event.player.sendMessage(mm("<gray>Right-click the earth with the <white>Seed of Earth<gray> to bear a child."))
            }
        }

        // Earth and Sky, both worn: the story can actually start.
        if (mythos.roles.holders("gaia").isNotEmpty() && mythos.roles.holders("uranus").isNotEmpty()) {
            mythos.eras.complete(era, "earth_and_sky", "Sky lay over Earth")
            mythos.roles.holders("uranus").mapNotNull { Bukkit.getPlayer(it) }.forEach { uranus ->
                context.schedulers.entity(uranus) {
                    uranus.sendMessage(mm("<aqua><i>She will bear children. They will be stronger than you."))
                    uranus.sendMessage(mm("<gray>When you cannot bear the sight of one, <white>right-click them<gray>. The ground will take them."))
                }
            }
        }
    }

    /**
     * In the Age of Chaos, the dead have nowhere to stand — a spirit is put in the Void, a real,
     * empty world, because there is no world yet to haunt.
     */
    @EventHandler
    fun onSpirit(event: PlayerBecameSpiritEvent) {
        val now = mythos.eras.currentId()
        if (now != era && now.isNotEmpty()) return
        context.schedulers.globalDelayed(20) {
            mythos.realms.send(event.player, "void", "You drift out of the world, because there isn't one.")
        }
    }

    /** When the age actually begins, sweep anyone who slipped through into the Gap. */
    @EventHandler
    fun onChaosBegins(event: EraAdvancedEvent) {
        if (event.to.id != era) return
        context.schedulers.globalDelayed(60) {
            mythos.spirits.spirits().mapNotNull { Bukkit.getPlayer(it) }
                .filter { mythos.realms.realmOf(it)?.id != "void" }
                .forEach { mythos.realms.send(it, "void", "There is no world yet. You are in the space where things are not.") }
        }
    }

    /** When the age turns, everyone still in the Gap is brought out into the world there now is. */
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

    // ---- the unmaking of the sky --------------------------------------------

    /**
     * Core has decided this blow *could* kill a Primordial. We override that: nothing unmakes the
     * Sky except the thing the Earth made for it, swung by hand. (The blow itself is raised to lethal
     * by the engine's trigger service when the sickle lands — see [CreationRites].)
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onDivineDeath(event: DivineDeathEvent) {
        if (event.victimRole.id != "uranus") return
        val killer = event.killer
        val wieldsSickle = killer != null && CreationItems.has(killer.inventory.itemInMainHand, CreationItems.sickleKey(context))
        if (!wieldsSickle) {
            event.isCancelled = true
            killer?.sendMessage(mm("<gray>He does not even notice. <dark_gray><i>He is the sky."))
            return
        }
        event.isCancelled = false
        event.unmakes = true
        uranusCut = true // this release is the actual cut — abdication/stripping won't set this
    }

    /** Sky is cut from Earth. The age ends — and the next one was already written by another jar. */
    @EventHandler
    fun onReleased(event: RoleReleasedEvent) {
        if (event.role.id != "uranus") return

        // Only the sickle cuts the sky. If Uranus merely abdicated or was stripped, the age does NOT
        // end — the mantle is offered on, and someone else can wear the sky.
        if (!uranusCut) {
            mythos.chronicle.record("story", "<gray>The Sky was given up, not cut. Another may take it up.")
            return
        }
        uranusCut = false

        // Sundered now, and never worn again.
        mythos.roles.seal("uranus", "the sickle fell")
        mythos.chronicle.record(
            "story",
            "<gray>The sky was cut from the earth with a sickle of grey adamant. " +
                "<dark_gray><i>Gaia made it. One of her children was willing to use it.",
        )

        // The blood-scar: a momentary wound where the sky fell — the ground turns and cracks, then heals.
        event.player?.let { fallen ->
            context.schedulers.entity(fallen) { bloodScar(fallen.location.clone(), fallen.world) }
        }

        // Everything he buried climbs back out — into the world that now exists.
        context.schedulers.global {
            CreationContent.TITANS.forEach { titan ->
                mythos.roles.holders(titan.id).forEach { uuid ->
                    val profile = mythos.profiles.profile(uuid)
                    if (profile.hasFlag("creation.imprisoned")) {
                        profile.setFlag("creation.imprisoned", null)
                        Bukkit.getPlayer(uuid)?.let { freed ->
                            mythos.realms.send(freed, "gaia", "The weight lifts. You climb out of your mother, into the light.")
                        }
                    }
                }
            }
            mythos.eras.complete(era, "the_unmaking", "the sickle fell")
        }
    }

    /** Where the sky's blood fell the ground turns red and cracks — then, after a few seconds, it heals. */
    private fun bloodScar(at: org.bukkit.Location, world: org.bukkit.World) {
        val restore = ArrayList<Pair<org.bukkit.block.Block, org.bukkit.block.data.BlockData>>()
        for (x in -4..4) for (z in -4..4) {
            if (x * x + z * z > 16) continue
            val block = at.clone().add(x.toDouble(), -1.0, z.toDouble()).block
            if (block.type.isSolid) {
                restore += block to block.blockData.clone()
                block.type = if ((x + z) % 3 == 0) Material.CRIMSON_NYLIUM else Material.RED_SAND
            }
        }
        world.strikeLightningEffect(at)
        context.schedulers.regionDelayed(at, 300) {
            restore.forEach { (block, data) -> block.blockData = data }
        }
    }
}
