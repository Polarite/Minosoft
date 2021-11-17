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
package de.bixilon.minosoft.data.registries.biomes

import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.data.registries.registries.Registries
import de.bixilon.minosoft.data.registries.registries.registry.RegistryItem
import de.bixilon.minosoft.data.registries.registries.registry.ResourceLocationDeserializer
import de.bixilon.minosoft.data.text.RGBColor
import de.bixilon.minosoft.data.text.RGBColor.Companion.asRGBColor
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.gui.rendering.tint.TintManager
import de.bixilon.minosoft.protocol.protocol.ProtocolDefinition
import de.bixilon.minosoft.util.KUtil.nullCast
import de.bixilon.minosoft.util.KUtil.toInt
import de.bixilon.minosoft.util.KUtil.unsafeCast
import de.bixilon.minosoft.util.MMath
import java.util.*

data class Biome(
    override val resourceLocation: ResourceLocation,
    val depth: Float,
    val scale: Float,
    val temperature: Float,
    val downfall: Float,
    val waterFogColor: RGBColor?,
    val category: BiomeCategory,
    val precipitation: BiomePrecipitation,
    val skyColor: RGBColor,
    val descriptionId: String?,
    val grassColorModifier: GrassColorModifiers = GrassColorModifiers.NONE,
) : RegistryItem() {
    val temperatureColorMapCoordinate = getColorMapCoordinate(temperature)
    val downfallColorMapCoordinate = getColorMapCoordinate(downfall * temperature)


    fun getClampedTemperature(height: Int): Int {
        return getColorMapCoordinate(MMath.clamp(temperature + (MMath.clamp(height - ProtocolDefinition.SEA_LEVEL_HEIGHT, 1, Int.MAX_VALUE) * ProtocolDefinition.HEIGHT_SEA_LEVEL_MODIFIER), 0.0f, 1.0f))
    }

    override fun toString(): String {
        return resourceLocation.full
    }

    companion object : ResourceLocationDeserializer<Biome> {

        private fun getColorMapCoordinate(value: Float): Int {
            return ((1.0 - MMath.clamp(value, 0.0f, 1.0f)) * RenderConstants.COLORMAP_SIZE).toInt()
        }

        override fun deserialize(registries: Registries?, resourceLocation: ResourceLocation, data: Map<String, Any>): Biome {
            check(registries != null) { "Registries is null!" }
            return Biome(
                resourceLocation = resourceLocation,
                depth = data["depth"]?.unsafeCast<Float>() ?: 0.0f,
                scale = data["scale"]?.unsafeCast<Float>() ?: 0.0f,
                temperature = data["temperature"]?.unsafeCast<Float>() ?: 0.0f,
                downfall = data["downfall"]?.unsafeCast<Float>() ?: 0.0f,
                waterFogColor = TintManager.getJsonColor(data["water_fog_color"]?.toInt() ?: 0),
                category = registries.biomeCategoryRegistry[data["category"]?.toInt() ?: -1] ?: DEFAULT_CATEGORY,
                precipitation = registries.biomePrecipitationRegistry[data["precipitation"]?.toInt() ?: -1] ?: DEFAULT_PRECIPITATION,
                skyColor = data["sky_color"]?.toInt()?.asRGBColor() ?: RenderConstants.GRASS_FAILOVER_COLOR,
                descriptionId = data["water_fog_color"].nullCast(),
                grassColorModifier = data["grass_color_modifier"].nullCast<String>()?.uppercase(Locale.getDefault())?.let { GrassColorModifiers.valueOf(it) } ?: when (resourceLocation) {
                    ResourceLocation("minecraft:swamp"), ResourceLocation("minecraft:swamp_hills") -> GrassColorModifiers.SWAMP
                    ResourceLocation("minecraft:dark_forest"), ResourceLocation("minecraft:dark_forest_hills") -> GrassColorModifiers.DARK_FOREST
                    else -> GrassColorModifiers.NONE
                }
            )
        }

        private val DEFAULT_PRECIPITATION = BiomePrecipitation("NONE")
        private val DEFAULT_CATEGORY = BiomeCategory("NONE")

    }

    enum class GrassColorModifiers {
        NONE,
        DARK_FOREST,
        SWAMP,
        ;
    }
}
