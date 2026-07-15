package net.crewco.mythos.creation

import net.crewco.mythos.addon.AddonContext
import net.crewco.mythos.api.role.RoleItems
import net.crewco.mythos.api.trigger.TriggerService
import net.crewco.mythos.command.CommandContext.Companion.mm
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * The three objects the first age turns on. None of them is typed into a chat box: each is a thing
 * you hold and use. The engine's [TriggerService] recognises them by the byte stamped under their
 * key — the material is only what it looks like.
 *
 * Two of them are **bound to Gaia** with [RoleItems]: the Seed and the adamant belong to the mantle,
 * not the person, so an abdicated Gaia can't keep bearing Titans. The **sickle is deliberately not**
 * — it is forged to be handed to a child and swung, and taken off a corpse if need be.
 */
object CreationItems {

    fun seedKey(ctx: AddonContext) = NamespacedKey(ctx.plugin, "seed_of_earth")
    fun adamantKey(ctx: AddonContext) = NamespacedKey(ctx.plugin, "grey_adamant")
    fun sickleKey(ctx: AddonContext) = NamespacedKey(ctx.plugin, "adamantine_sickle")

    private fun mark(item: ItemStack, key: NamespacedKey) = item.apply {
        editMeta { it.persistentDataContainer.set(key, PersistentDataType.BYTE, 1) }
    }

    fun has(item: ItemStack?, key: NamespacedKey): Boolean =
        item?.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.BYTE) == true

    /** Gaia's — and bound to her mantle. Right-click the ground with it and she bears a Titan. */
    fun seed(ctx: AddonContext): ItemStack = RoleItems.bind(ctx.plugin, mark(ItemStack(Material.WHEAT_SEEDS), seedKey(ctx)).apply {
        editMeta { meta ->
            meta.displayName(mm("<green><b>The Seed of Earth"))
            meta.lore(listOf(
                mm("<dark_gray><i>Everything that will ever live is folded up in here."),
                mm("<gray>Right-click the ground. Bear a child.</gray>"),
            ))
            meta.isUnbreakable = true
        }
    }, "gaia")

    /** Given to Gaia when her children are buried — and bound to her. The raw stuff of the thing that ends the Sky. */
    fun adamant(ctx: AddonContext): ItemStack = RoleItems.bind(ctx.plugin, mark(ItemStack(Material.NETHERITE_SCRAP), adamantKey(ctx)).apply {
        editMeta { meta ->
            meta.displayName(mm("<gray><b>Grey Adamant"))
            meta.lore(listOf(
                mm("<dark_gray><i>Older than iron, and it remembers being struck."),
                mm("<gray>Raise it, and bend over it, and work it.</gray>"),
            ))
        }
    }, "gaia")

    /**
     * The forged thing. It can unmake the Sky, and nothing else can. Swung, never typed — and NOT
     * role-bound, because it is made to change hands: Gaia forges it, a child swings it.
     */
    fun sickle(ctx: AddonContext): ItemStack = mark(ItemStack(Material.NETHERITE_HOE), sickleKey(ctx)).apply {
        editMeta { meta ->
            meta.displayName(mm("<gray><b>The Adamantine Sickle"))
            meta.lore(listOf(
                mm("<dark_gray><i>Grey flint, jagged-toothed."),
                mm("<dark_gray><i>Made by the Earth, for a child who will use it."),
                mm(""),
                mm("<red>It can unmake the Sky. Nothing else can."),
            ))
            meta.isUnbreakable = true
        }
    }
}
