/*
 * Minosoft
 * Copyright (C) 2021 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.entities.block

import de.bixilon.minosoft.data.inventory.ItemStack
import de.bixilon.minosoft.data.mappings.ResourceLocation
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.protocol.network.connection.PlayConnection
import de.bixilon.minosoft.util.KUtil.nullCast
import de.bixilon.minosoft.util.nbt.tag.NBTUtil.listCast

class CampfireBlockEntity(connection: PlayConnection) : BlockEntity(connection) {
    val items: Array<ItemStack?> = arrayOfNulls(RenderConstants.CAMPFIRE_ITEMS)


    override fun updateNBT(nbt: Map<String, Any>) {
        val itemArray = nbt["Items"]?.listCast<Map<String, Any>>() ?: return
        for (slot in itemArray) {

            val itemStack = ItemStack(
                item = connection.registries.itemRegistry[slot["id"]?.nullCast<String>()!!]!!,
                version = connection.version,
                count = slot["Count"]?.nullCast<Number>()?.toInt() ?: 1,
            )

            items[slot["Slot"]?.nullCast<Number>()?.toInt()!!] = itemStack
        }
    }

    companion object : BlockEntityFactory<CampfireBlockEntity> {
        override val RESOURCE_LOCATION: ResourceLocation = ResourceLocation("minecraft:campfire")

        override fun build(connection: PlayConnection): CampfireBlockEntity {
            return CampfireBlockEntity(connection)
        }
    }
}
