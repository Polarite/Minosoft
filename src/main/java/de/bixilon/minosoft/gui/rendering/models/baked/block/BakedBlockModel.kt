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

package de.bixilon.minosoft.gui.rendering.models.baked.block

import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.data.registries.blocks.BlockState
import de.bixilon.minosoft.gui.rendering.block.mesh.ChunkSectionMeshes
import de.bixilon.minosoft.gui.rendering.models.FaceProperties
import de.bixilon.minosoft.gui.rendering.models.baked.BakedModel
import glm_.vec3.Vec3i
import java.util.*

interface BakedBlockModel : BakedModel {

    fun getTouchingFaceProperties(random: Random, direction: Directions): Array<FaceProperties>

    fun singleRender(position: Vec3i, mesh: ChunkSectionMeshes, random: Random, blockState: BlockState, neighbours: Array<BlockState?>, light: Int, ambientLight: FloatArray, tints: IntArray?): Boolean
}
