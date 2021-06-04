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
package de.bixilon.minosoft.data.entities.entities.animal

import de.bixilon.minosoft.data.entities.EntityMetaDataFields
import de.bixilon.minosoft.data.entities.EntityRotation
import de.bixilon.minosoft.data.entities.entities.EntityMetaDataFunction
import de.bixilon.minosoft.data.entities.entities.TamableAnimal
import de.bixilon.minosoft.data.mappings.ResourceLocation
import de.bixilon.minosoft.data.mappings.entities.EntityFactory
import de.bixilon.minosoft.data.mappings.entities.EntityType
import de.bixilon.minosoft.data.text.ChatColors
import de.bixilon.minosoft.data.text.RGBColor
import de.bixilon.minosoft.protocol.network.connection.PlayConnection
import glm_.vec3.Vec3

class Cat(connection: PlayConnection, entityType: EntityType, position: Vec3, rotation: EntityRotation) : TamableAnimal(connection, entityType, position, rotation) {

    @get:EntityMetaDataFunction(name = "Variant")
    val variant: CatVariants
        get() = CatVariants.byId(entityMetaData.sets.getInt(EntityMetaDataFields.CAT_VARIANT))

    @get:EntityMetaDataFunction(name = "Lying")
    val isLying: Boolean
        get() = entityMetaData.sets.getBoolean(EntityMetaDataFields.CAT_IS_LYING)

    @get:EntityMetaDataFunction(name = "Relaxed")
    val isRelaxed: Boolean
        get() = entityMetaData.sets.getBoolean(EntityMetaDataFields.CAT_IS_RELAXED)

    @get:EntityMetaDataFunction(name = "Collar color")
    val collarColor: RGBColor
        get() = ChatColors[entityMetaData.sets.getInt(EntityMetaDataFields.CAT_GET_COLLAR_COLOR)]

    enum class CatVariants {
        TABBY,
        BLACK,
        RED,
        SIAMESE,
        BRITISH_SHORT_HAIR,
        CALICO,
        PERSIAN,
        RAGDOLL,
        ALL_BLACK,
        ;

        companion object {
            private val CAT_VARIANTS = values()

            fun byId(id: Int): CatVariants {
                return CAT_VARIANTS[id]
            }
        }
    }

    companion object : EntityFactory<Cat> {
        override val RESOURCE_LOCATION: ResourceLocation = ResourceLocation("cat")

        override fun build(connection: PlayConnection, entityType: EntityType, position: Vec3, rotation: EntityRotation): Cat {
            return Cat(connection, entityType, position, rotation)
        }
    }
}
