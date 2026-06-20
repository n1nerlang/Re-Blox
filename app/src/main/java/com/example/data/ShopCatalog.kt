package com.example.data

sealed class ShopItem(
    val id: String,
    val name: String,
    val price: Int,
    val description: String,
    val type: ShopItemType
) {
    enum class ShopItemType {
        COSMETIC, // Hat or Gear
        GAMEPASS  // Ability or badge
    }

    object DefaultHat : ShopItem("default_hat", "Classic Visor", 0, "Nostalgic blue visor for early builders.", ShopItemType.COSMETIC)
    object ClassicFedora : ShopItem("classic_fedora", "Classic Fedora", 250, "A super sharp black fedora. FREE by registering your email!", ShopItemType.COSMETIC)
    object ValkyrieHelm : ShopItem("valkyrie_helm", "Valkyrie Helm", 1000, "Highly respected ancient warrior helmet.", ShopItemType.COSMETIC)
    object Dominus : ShopItem("dominus", "Dominus Empyreus", 5000, "Vastly legendary hood. Pure status symbol.", ShopItemType.COSMETIC)
    object GoldCrown : ShopItem("gold_crown", "Golden Crown", 150, "A shiny royal headpiece to look like classic kings.", ShopItemType.COSMETIC)
    object ClassicSword : ShopItem("classic_sword", "Classic Sword", 500, "Nostalgic pixelated linked sword.", ShopItemType.COSMETIC)

    object SpeedCoil : ShopItem("speed_coil", "Speed Coil", 350, "Unlocks rapid movement speed. Trail particles included!", ShopItemType.GAMEPASS)
    object GravityCoil : ShopItem("gravity_coil", "Gravity Coil", 350, "Defy gravity. Jump 1.5x higher in the obby!", ShopItemType.GAMEPASS)
    object VipPass : ShopItem("vip_pass", "VIP Gamepass", 400, "Grants [VIP] chat tag & 2x Robux rewards during obby play!", ShopItemType.GAMEPASS)

    companion object {
        val allItems = listOf(
            DefaultHat, ClassicFedora, ValkyrieHelm, Dominus, GoldCrown, ClassicSword,
            SpeedCoil, GravityCoil, VipPass
        )

        fun getItemById(id: String): ShopItem? {
            return allItems.find { it.id == id }
        }
    }
}
