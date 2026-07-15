package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.ritual.Ritual
import net.crewco.mythos.api.ritual.Step
import net.crewco.mythos.api.role.RoleDefinition
import net.crewco.mythos.api.role.RoleTier
import net.crewco.mythos.api.story.beats
import net.crewco.mythos.api.story.line
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

/**
 * **The first age, as a set of things you do.**
 *
 * Not one `/power` in here. Gaia bears a Titan by *using the Seed on the ground*; Uranus buries a
 * child by *right-clicking them*; the sickle is *forged* in two acts and *swung* to end the Sky. The
 * engine's trigger, ritual and director services do the plumbing — this file only says what the acts
 * are and what they mean.
 *
 * The Director half is the other promise kept: every required beat has a fallback, so a server with
 * six players and no Uranus still watches the age turn, instead of hanging on a mantle nobody took.
 */
class CreationRites(private val mythos: Mythos, private val context: AddonContext) {

    private val era = CreationContent.ERA
    private val birthOrder = CreationContent.TITANS.map { it.id }

    private val lastVeil = HashMap<UUID, Long>()
    private val lastUnmake = HashMap<UUID, Long>()

    fun wire() {
        // ---- BIRTH: Gaia right-clicks the earth with the Seed -------------------------------
        mythos.triggers.onUse(CreationItems.seedKey(context)) { ctx ->
            val gaia = ctx.player
            if (mythos.roles.roleOf(gaia.uniqueId)?.id != "gaia") return@onUse false
            if (!mythos.eras.isComplete(era, "earth_and_sky")) {
                gaia.sendMessage(mm("<gray>There is no Sky yet to bear children to. <dark_gray><i>Wait for Uranus."))
                return@onUse true
            }
            val titanId = birthOrder.firstOrNull { mythos.roles.holders(it).isEmpty() } ?: run {
                gaia.sendMessage(mm("<gray>All your children are born.")); return@onUse true
            }
            val spirit = mythos.spirits.queue().firstOrNull { Bukkit.getPlayer(it) != null } ?: run {
                gaia.sendMessage(mm("<gray>No soul waits in the dark to be born. <dark_gray><i>Give it time.")); return@onUse true
            }
            val at = ctx.block?.location?.clone()?.add(0.5, 1.0, 0.5) ?: gaia.location
            bear(gaia, spirit, titanId, at)
            true
        }

        // ---- IMPRISON: Uranus lays hands on one of his children -----------------------------
        mythos.triggers.onInteractEntity { ctx ->
            if (mythos.roles.roleOf(ctx.player.uniqueId)?.id != "uranus") return@onInteractEntity false
            val child = ctx.target as? Player ?: return@onInteractEntity false
            val role = mythos.roles.roleOf(child.uniqueId) ?: return@onInteractEntity false
            if (role.tier != RoleTier.TITAN) return@onInteractEntity false
            if (!mythos.eras.isComplete(era, "children_born")) {
                ctx.player.sendMessage(mm("<gray>They are not all born yet. <dark_gray><i>Let her finish bearing them."))
                return@onInteractEntity true
            }
            imprison(ctx.player, child, role)
            true
        }

        // ---- UNMAKE: Chaos right-clicks a mortal thing out of the story ---------------------
        mythos.triggers.onInteractEntity { ctx ->
            if (mythos.roles.roleOf(ctx.player.uniqueId)?.id != "chaos") return@onInteractEntity false
            val target = ctx.target as? Player ?: return@onInteractEntity false
            unmake(ctx.player, target)
        }

        // ---- VEIL: Nyx draws the night over herself with a gesture --------------------------
        mythos.triggers.onGesture { ctx ->
            if (mythos.roles.roleOf(ctx.player.uniqueId)?.id != "nyx") return@onGesture false
            veil(ctx.player)
            false // don't swallow the sneak itself
        }

        // ---- THE STRIKE: the sickle, on the Sky, is decisive --------------------------------
        mythos.triggers.onStrike(CreationItems.sickleKey(context)) { ctx ->
            val victim = ctx.target as? Player ?: return@onStrike false
            if (mythos.roles.roleOf(victim.uniqueId)?.id != "uranus") return@onStrike false
            ctx.player.sendMessage(mm("<gray><i>The sickle knows what it is for."))
            true // the engine turns this into a killing blow; core's death check does the rest
        }

        // ---- THE DIRECTOR: declared ways to force a stuck beat, run by /mythos forward ------
        mythos.director.fallback(era, "children_born") {
            mythos.narrator.tell(beats { line("<gray>Earth bore them without ceremony, because there was no one to watch.", 30) })
            mythos.eras.complete(era, "children_born", "resolved: too few to be borne by hand")
        }
        mythos.director.fallback(era, "the_imprisonment") {
            mythos.narrator.tell(beats { line("<aqua>The Sky feared his children and buried them, unseen.", 30) })
            mythos.eras.complete(era, "the_imprisonment", "resolved")
        }
        mythos.director.fallback(era, "the_sickle") {
            mythos.rituals.handle("creation-forge")?.resolve()
                ?: mythos.eras.complete(era, "the_sickle", "resolved: the Earth made it alone")
        }
        mythos.director.fallback(era, "the_unmaking") {
            mythos.narrator.tell(beats { line("<dark_red>The Sky was never worn, and it fell of its own weight.", 30) })
            mythos.eras.complete(era, "the_unmaking", "resolved: the sky went uncut and uncrowned")
        }
    }

    // ---- birth ---------------------------------------------------------------

    private fun bear(gaia: Player, spirit: UUID, titanId: String, at: Location) {
        val titan = mythos.roles.definition(titanId) ?: return
        context.schedulers.global {
            if (mythos.roles.holders(titanId).isNotEmpty() || !mythos.spirits.isSpirit(spirit)) return@global
            mythos.roles.assign(spirit, titanId, "borne by Gaia")
            Bukkit.getServer().sendMessage(
                mm("<dark_gray>» <green>Gaia <gray>bears <gold>${titan.displayName}<gray>, and the world is heavier for it."),
            )
            Bukkit.getPlayer(spirit)?.let { child -> context.schedulers.entity(child) { child.teleportAsync(at) } }
            context.schedulers.region(at) { ring(at) }

            val born = CreationContent.TITANS.count { mythos.roles.holders(it.id).isNotEmpty() }
            if (born >= mythos.director.scale(6)) mythos.eras.complete(era, "children_born", "the children of Earth and Sky")
        }
    }

    /** A ring of moss blooms where a child rose out of the ground, then fades and the ground returns. */
    private fun ring(at: Location) {
        val restore = ArrayList<Pair<org.bukkit.block.Block, org.bukkit.block.data.BlockData>>()
        var a = 0
        while (a < 360) {
            val x = at.x + 2.0 * cos(Math.toRadians(a.toDouble()))
            val z = at.z + 2.0 * sin(Math.toRadians(a.toDouble()))
            val block = Location(at.world, x, at.y - 1, z).block
            if (block.type.isSolid) {
                restore += block to block.blockData.clone()
                block.type = Material.MOSS_BLOCK
            }
            a += 30
        }
        at.world?.spawnParticle(Particle.HEART, at, 24, 1.2, 1.2, 1.2)

        // The bloom is momentary. Put the ground back exactly as it was after a few seconds.
        context.schedulers.regionDelayed(at, 120) {
            restore.forEach { (block, data) -> block.blockData = data }
        }
    }

    // ---- imprison ------------------------------------------------------------

    private fun imprison(uranus: Player, child: Player, role: RoleDefinition) {
        val first = !mythos.eras.isComplete(era, "the_imprisonment")

        mythos.profiles.profile(child.uniqueId).setFlag("creation.imprisoned", true)
        mythos.realms.send(child, "tartarus", "<dark_red>The sky closes over you, and keeps closing, and does not stop.")
        Bukkit.getServer().sendMessage(
            mm("<dark_gray>» <aqua>Uranus <gray>casts <gold>${role.displayName} <gray>into the dark beneath the world."),
        )

        if (first) {
            mythos.roles.holders("gaia").mapNotNull { Bukkit.getPlayer(it) }.forEach { g ->
                context.schedulers.entity(g) {
                    g.inventory.addItem(CreationItems.adamant(context))
                    g.sendMessage(mm("<green><i>You feel them, packed into you like stones. It is unbearable. Do something."))
                    g.sendMessage(mm("<gray>  Raise the <white>grey adamant<gray>, and bend over it, and work it."))
                }
            }
            beginForge()
            mythos.eras.complete(era, "the_imprisonment", "Uranus feared his children")
        }
    }

    // ---- the forge (a rite, not a command) -----------------------------------

    private fun beginForge() {
        mythos.rituals.begin(
            Ritual(
                id = "creation-forge",
                displayName = "The Forging of the Sickle",
                ordered = true,
                minPlayers = 1,
                lore = listOf("<dark_gray><i>Made underground. Nobody sees it but the Earth."),
                steps = listOf(
                    Step.UseItem("raise", "Raise the grey adamant", CreationItems.adamantKey(context)),
                    Step.Kneel("work", "Bend over it and work it"),
                ),
                onComplete = { by -> forged(by) },
                onResolve = { mythos.eras.complete(era, "the_sickle", "the Earth made it alone") },
            ),
        )
    }

    private fun forged(by: Player?) {
        val gaia = by ?: mythos.roles.holders("gaia").firstNotNullOfOrNull { Bukkit.getPlayer(it) }
        gaia?.let { g ->
            context.schedulers.entity(g) {
                g.inventory.contents.forEachIndexed { i, item -> if (CreationItems.has(item, CreationItems.adamantKey(context))) g.inventory.setItem(i, null) }
                g.inventory.addItem(CreationItems.sickle(context))
                g.sendMessage(mm("<green>Grey adamant, jagged-toothed. <gray>Now: which of your children hates him enough?"))
            }
        }
        Bukkit.getServer().sendMessage(
            mm("<dark_gray>» <gray>Something is made underground. <dark_gray><i>Nobody sees it but the Earth."),
        )
        mythos.eras.complete(era, "the_sickle", "Gaia forged it in secret")
    }

    // ---- flavour, as gestures ------------------------------------------------

    private fun veil(nyx: Player) {
        val now = System.currentTimeMillis()
        if (now - (lastVeil[nyx.uniqueId] ?: 0L) < 180_000L) return
        lastVeil[nyx.uniqueId] = now
        nyx.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, false, false))
        nyx.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 600, 1, false, false))
        nyx.sendMessage(mm("<dark_gray><i>The night is yours, and it closes."))
    }

    private fun unmake(chaos: Player, target: Player): Boolean {
        val role = mythos.roles.roleOf(target.uniqueId)
        if (role != null && role.tier.heartsBonus > 0) {
            chaos.sendMessage(mm("<red>${role.displayName} is too solid to simply stop existing. You are old, not omnipotent."))
            return false
        }
        val now = System.currentTimeMillis()
        if (now - (lastUnmake[chaos.uniqueId] ?: 0L) < 600_000L) {
            chaos.sendMessage(mm("<dark_gray><i>The gap has closed again. Not yet.")); return false
        }
        lastUnmake[chaos.uniqueId] = now
        context.schedulers.entity(target) {
            target.sendMessage(mm("<dark_purple><i>For a moment, there is nothing where you are."))
            target.damage(1000.0, chaos)
        }
        return true
    }
}
