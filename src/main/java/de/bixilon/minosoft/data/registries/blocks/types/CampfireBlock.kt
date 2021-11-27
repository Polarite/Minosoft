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

package de.bixilon.minosoft.data.registries.blocks.types

import de.bixilon.minosoft.data.inventory.ItemStack
import de.bixilon.minosoft.data.player.Hands
import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.data.registries.blocks.BlockFactory
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.data.registries.blocks.properties.BlockProperties
import de.bixilon.minosoft.data.registries.items.tools.ShovelItem
import de.bixilon.minosoft.data.registries.registries.Registries
import de.bixilon.minosoft.gui.rendering.input.camera.hit.RaycastHit
import de.bixilon.minosoft.gui.rendering.input.interaction.InteractionResults
import de.bixilon.minosoft.gui.rendering.particle.types.render.texture.simple.campfire.CampfireSmokeParticle
import de.bixilon.minosoft.gui.rendering.particle.types.render.texture.simple.fire.SmokeParticle
import de.bixilon.minosoft.gui.rendering.particle.types.render.texture.simple.lava.LavaParticle
import de.bixilon.minosoft.gui.rendering.util.VecUtil.horizontalPlus
import de.bixilon.minosoft.gui.rendering.util.VecUtil.noise
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection
import de.bixilon.minosoft.util.KUtil.chance
import de.bixilon.minosoft.util.KUtil.toBoolean
import de.bixilon.minosoft.util.KUtil.toResourceLocation
import glm_.vec3.Vec3
import glm_.vec3.Vec3d
import glm_.vec3.Vec3i
import kotlin.random.Random

open class CampfireBlock(resourceLocation: ResourceLocation, registries: Registries, data: Map<String, Any>) : Block(resourceLocation, registries, data) {
    val lavaParticles = data["lava_particles"]?.toBoolean() ?: true

    private val cosySmokeParticle = registries.particleTypeRegistry[CampfireSmokeParticle.CosyFactory]!!
    private val signalSmokeParticle = registries.particleTypeRegistry[CampfireSmokeParticle.SignalFactory]!!
    private val lavaParticle = registries.particleTypeRegistry[LavaParticle]!!
    private val smokeParticle = registries.particleTypeRegistry[SmokeParticle]!!

    private fun extinguish(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i) {
        for (i in 0 until 20) {
            spawnSmokeParticles(connection, blockState, blockPosition, true)
        }
    }

    fun spawnSmokeParticles(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i, extinguished: Boolean) {
        val position = Vec3d(blockPosition).horizontalPlus(
            { 0.5 + 3.0.noise },
            Random.nextDouble() + Random.nextDouble() + 0.5 // ToDo: This +0.5f is a temporary fix for not making the particle stuck in ourself
        )

        val isSignal = blockState.properties[BlockProperties.CAMPFIRE_SIGNAL_FIRE] == true

        val particleType = if (isSignal) {
            signalSmokeParticle
        } else {
            cosySmokeParticle
        }

        connection.world += CampfireSmokeParticle(connection, position, Vec3d(0.0f, 0.07f, 0.0f), particleType.default(), isSignal)

        if (extinguished) {
            val position = Vec3d(blockPosition).horizontalPlus(
                { 0.5 + 4.0.noise },
                0.5
            )
            connection.world += SmokeParticle(connection, position, Vec3d(0.0f, 0.005f, 0.0f), smokeParticle.default())
        }
    }

    override fun randomTick(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i, random: Random) {
        if (blockState.properties[BlockProperties.LIT] != true) {
            return
        }
        if (random.chance(10)) {
            connection.world.playSoundEvent(CAMPFIRE_CRACKLE_SOUND, blockPosition + Vec3(0.5f), 0.5f + random.nextFloat(), 0.6f + random.nextFloat() * 0.7f)
        }

        if (lavaParticles && random.chance(20)) {
            val position = Vec3d(blockPosition) + 0.5
            for (i in 0 until random.nextInt(1) + 1) {
                connection.world += LavaParticle(connection, position, lavaParticle.default())
            }
        }
    }

    override fun onUse(connection: PlayConnection, blockState: BlockState, blockPosition: Vec3i, raycastHit: RaycastHit, hand: Hands, itemStack: ItemStack?): InteractionResults {
        // ToDo: Ignite (flint and steel, etc)
        if (itemStack?.item !is ShovelItem || blockState.properties[BlockProperties.LIT] != true) {
            return super.onUse(connection, blockState, blockPosition, raycastHit, hand, itemStack)
        }
        connection.world.setBlockState(blockPosition, blockState.withProperties(BlockProperties.LIT to false))
        extinguish(connection, blockState, blockPosition)
        return InteractionResults.SUCCESS
    }

    companion object : BlockFactory<CampfireBlock> {
        private val CAMPFIRE_CRACKLE_SOUND = "minecraft:block.campfire.crackle".toResourceLocation()

        override fun build(resourceLocation: ResourceLocation, registries: Registries, data: Map<String, Any>): CampfireBlock {
            return CampfireBlock(resourceLocation, registries, data)
        }
    }
}
