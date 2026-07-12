package net.crewco.mythos.creation

import net.crewco.mythos.api.Mythos
import net.crewco.mythos.api.power.Power
import net.crewco.mythos.api.power.PowerContext
import net.crewco.mythos.command.CommandContext.Companion.mm
import net.crewco.mythos.addon.AddonContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * The three powers that drive the first age forward. Note what they *are*: none of
 * them is a fireball. Each one is a lever one player pulls on another player's story.
 */

/** Gaia reaches into the spirit world and makes someone real. */
class BirthPower(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Power {
    override val id = "birth"
    override val displayName = "Bear a Child"
    override val description = "Pull a spirit out of the dark and make them a Titan. /power birth <spirit> <titan>"
    override val cooldownSeconds = 120

    override fun use(ctx: PowerContext): Boolean {
        val gaia = ctx.player
        if (ctx.args.size < 2) {
            gaia.sendMessage(mm("<red>/power birth <spirit> <titan>"))
            val free = CreationContent.TITANS.filter { mythos.roles.holders(it.id).isEmpty() }
            gaia.sendMessage(mm("<gray>Unborn: <white>${free.joinToString { it.id }}"))
            gaia.sendMessage(mm("<gray>Waiting: <white>${mythos.spirits.queue().mapNotNull { Bukkit.getPlayer(it)?.name }.joinToString()}"))
            return false
        }

        val target = Bukkit.getPlayerExact(ctx.args[0]) ?: run {
            gaia.sendMessage(mm("<red>No such spirit is present.")); return false
        }
        val titanId = ctx.args[1].lowercase()
        val titan = mythos.roles.definition(titanId) ?: run {
            gaia.sendMessage(mm("<red>No such Titan.")); return false
        }
        if (CreationContent.TITANS.none { it.id == titanId }) {
            gaia.sendMessage(mm("<red>That is not yours to bear.")); return false
        }
        if (!mythos.spirits.isSpirit(target.uniqueId)) {
            gaia.sendMessage(mm("<red>${target.name} already exists. You cannot bear them twice.")); return false
        }
        if (mythos.roles.holders(titanId).isNotEmpty()) {
            gaia.sendMessage(mm("<red>${titan.displayName} already walks the world.")); return false
        }

        // Cross-region: the target may be ticking on another thread entirely.
        // assign() hops onto their region itself, so we only need to be on global.
        context.schedulers.global {
            mythos.roles.assign(target.uniqueId, titanId, "borne by Gaia")
            Bukkit.getServer().sendMessage(
                mm("<dark_gray>» <green>Gaia <gray>bears <gold>${titan.displayName}<gray>, and the world is heavier for it."),
            )
            // Six children, and the sky begins to be afraid.
            val born = CreationContent.TITANS.count { mythos.roles.holders(it.id).isNotEmpty() }
            if (born >= 6) mythos.eras.complete(CreationContent.ERA, "children_born", "six children of Earth and Sky")
        }
        return true
    }
}

/** Uranus buries a child in Tartarus, because he cannot bear to look at them. */
class ImprisonPower(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Power {
    override val id = "imprison"
    override val displayName = "Cast Down"
    override val description = "Bury one of your children in Tartarus. /power imprison <titan>"
    override val cooldownSeconds = 300

    override fun use(ctx: PowerContext): Boolean {
        val uranus = ctx.player
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) } ?: run {
            uranus.sendMessage(mm("<red>/power imprison <player>")); return false
        }
        val role = mythos.roles.roleOf(target.uniqueId) ?: run {
            uranus.sendMessage(mm("<red>Only your children can be cast down.")); return false
        }
        if (CreationContent.TITANS.none { it.id == role.id }) {
            uranus.sendMessage(mm("<red>${target.name} is not yours to bury.")); return false
        }

        val profile = mythos.profiles.profile(target.uniqueId)
        profile.setFlag("creation.imprisoned", true)

        val pit: Location = target.world.spawnLocation.clone().apply { y = target.world.minHeight + 5.0 }

        // Folia: teleportAsync, always. And the effects must be applied on the
        // region that owns the TARGET, not the one that owns Uranus.
        target.teleportAsync(pit).thenRun {
            context.schedulers.entity(target) {
                target.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 200, 0))
                target.addPotionEffect(PotionEffect(PotionEffectType.SLOWNESS, 400, 1))
                target.sendMessage(mm("<dark_red>The sky closes over you. You are in Tartarus, and it is very far down."))
            }
        }

        Bukkit.getServer().sendMessage(
            mm("<dark_gray>» <aqua>Uranus <gray>casts <gold>${role.displayName} <gray>into the dark beneath the world."),
        )
        // Gaia feels every one of them, buried in her.
        mythos.roles.holders("gaia").mapNotNull { Bukkit.getPlayer(it) }.forEach { gaia ->
            context.schedulers.entity(gaia) {
                gaia.sendMessage(mm("<green><i>You feel them, packed into you like stones. It is unbearable. Do something."))
                gaia.sendMessage(mm("<gray>  <white>/power sickle"))
            }
        }
        mythos.eras.complete(CreationContent.ERA, "the_imprisonment", "Uranus feared his children")
        return true
    }
}

/** Gaia makes the thing that ends her husband. */
class SicklePower(
    private val mythos: Mythos,
    private val context: AddonContext,
) : Power {
    override val id = "sickle"
    override val displayName = "Forge the Adamantine Sickle"
    override val description = "Make a grey sickle of jagged teeth — and give it to a child brave enough. /power sickle"
    override val cooldownSeconds = 0

    override fun use(ctx: PowerContext): Boolean {
        val gaia = ctx.player
        if (!mythos.eras.isComplete(CreationContent.ERA, "the_imprisonment")) {
            gaia.sendMessage(mm("<red>You have no reason yet. Wait — he will give you one."))
            return false
        }
        if (mythos.eras.isComplete(CreationContent.ERA, "the_sickle")) {
            gaia.sendMessage(mm("<red>It is already made. Find someone willing to hold it."))
            return false
        }

        gaia.inventory.addItem(sickle(context))
        gaia.sendMessage(mm("<green>Grey adamant, jagged-toothed. <gray>Now: which of your children hates him enough?"))
        Bukkit.getServer().sendMessage(
            mm("<dark_gray>» <gray>Something is being made underground. <dark_gray><i>Nobody sees it but the Earth."),
        )
        mythos.eras.complete(CreationContent.ERA, "the_sickle", "Gaia forged it in secret")
        return true
    }

    companion object {
        fun sickleKey(context: AddonContext) = NamespacedKey(context.plugin, "adamantine_sickle")

        fun sickle(context: AddonContext): ItemStack = ItemStack(Material.NETHERITE_HOE).apply {
            editMeta { meta ->
                meta.displayName(mm("<gray><b>The Adamantine Sickle"))
                meta.lore(
                    listOf(
                        mm("<dark_gray><i>Grey flint, jagged-toothed."),
                        mm("<dark_gray><i>Made by the Earth, for a child who will use it."),
                        mm(""),
                        mm("<red>It can unmake the Sky. Nothing else can."),
                    ),
                )
                meta.isUnbreakable = true
                meta.persistentDataContainer.set(sickleKey(context), PersistentDataType.BYTE, 1)
            }
        }

        fun isSickle(item: ItemStack?, context: AddonContext): Boolean {
            val meta = item?.itemMeta ?: return false
            return meta.persistentDataContainer.has(sickleKey(context), PersistentDataType.BYTE)
        }
    }
}

/** Nyx draws the night over herself, and even the sky loses her. */
class VeilPower(private val context: AddonContext) : Power {
    override val id = "veil"
    override val displayName = "Veil of Night"
    override val description = "Vanish into your own darkness for 30 seconds. /power veil"
    override val cooldownSeconds = 180

    override fun use(ctx: PowerContext): Boolean {
        val player: Player = ctx.player
        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 600, 0, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 600, 1, false, false))
        player.sendMessage(mm("<dark_gray><i>The night is yours, and it closes."))
        return true
    }
}

/** Chaos remembers that it can simply... stop. */
class UnmakePower(private val mythos: Mythos, private val context: AddonContext) : Power {
    override val id = "unmake"
    override val displayName = "The Gap"
    override val description = "Open the void beneath a mortal thing, and let it fall out of the story. /power unmake <player>"
    override val cooldownSeconds = 600

    override fun use(ctx: PowerContext): Boolean {
        val chaos = ctx.player
        val target = ctx.args.firstOrNull()?.let { Bukkit.getPlayerExact(it) } ?: run {
            chaos.sendMessage(mm("<red>/power unmake <player>")); return false
        }
        val role = mythos.roles.roleOf(target.uniqueId)
        if (role != null && role.tier.heartsBonus > 0) {
            chaos.sendMessage(mm("<red>${role.displayName} is too solid to simply stop existing. You are old, not omnipotent."))
            return false
        }
        context.schedulers.entity(target) {
            target.sendMessage(mm("<dark_purple><i>For a moment, there is nothing where you are."))
            target.damage(1000.0, chaos)
        }
        return true
    }
}
