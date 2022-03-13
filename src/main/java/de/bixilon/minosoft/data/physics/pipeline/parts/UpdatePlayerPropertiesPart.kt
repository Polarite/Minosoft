/*
 * Minosoft
 * Copyright (C) 2020-2022 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.data.physics.pipeline.parts

import de.bixilon.minosoft.data.entities.entities.player.PlayerEntity
import de.bixilon.minosoft.data.physics.pipeline.PipelineBuilder
import de.bixilon.minosoft.data.physics.pipeline.PipelineContext
import de.bixilon.minosoft.data.physics.pipeline.PipelinePart
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import kotlin.reflect.KClass

object UpdatePlayerPropertiesPart : PipelinePart<PlayerEntity>, PipelineBuilder<PlayerEntity, UpdatePlayerPropertiesPart> {
    override val name: String = "update_player_properties"
    override val entity: KClass<PlayerEntity> = PlayerEntity::class


    override fun handle(context: PipelineContext, entity: PlayerEntity) {
        entity.currentBiome = entity.connection.world.getBiome(entity.physics.positioning.blockPosition)
    }

    override fun build(connection: PlayConnection): UpdatePlayerPropertiesPart {
        return this
    }
}
