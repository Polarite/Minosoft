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

package de.bixilon.minosoft.gui.rendering.system.base

import de.bixilon.minosoft.data.registries.ResourceLocation
import de.bixilon.minosoft.gui.rendering.system.base.buffer.uniform.FloatUniformBuffer
import de.bixilon.minosoft.gui.rendering.system.base.buffer.uniform.IntUniformBuffer
import de.bixilon.minosoft.gui.rendering.system.base.buffer.vertex.FloatVertexBuffer
import de.bixilon.minosoft.gui.rendering.system.base.buffer.vertex.PrimitiveTypes
import de.bixilon.minosoft.gui.rendering.system.base.shader.Shader
import de.bixilon.minosoft.gui.rendering.util.mesh.MeshStruct
import glm_.vec2.Vec2i
import java.nio.ByteBuffer

interface RenderSystem {
    val shaders: MutableSet<Shader>
    val vendor: GPUVendor
    var shader: Shader?

    fun init()

    fun reset(
        depthTest: Boolean = true,
        blending: Boolean = true,
        faceCulling: Boolean = true,
        depthMask: Boolean = true,
        sourceAlpha: BlendingFunctions = BlendingFunctions.SOURCE_ALPHA,
        destinationAlpha: BlendingFunctions = BlendingFunctions.ONE_MINUS_SOURCE_ALPHA,
        depth: DepthFunctions = DepthFunctions.LESS,
    ) {
        setBlendFunc(sourceAlpha, destinationAlpha, BlendingFunctions.ONE, BlendingFunctions.ZERO)
        this[RenderingCapabilities.DEPTH_TEST] = depthTest
        this[RenderingCapabilities.BLENDING] = blending
        this[RenderingCapabilities.FACE_CULLING] = faceCulling
        this.depth = depth
        this.depthMask = depthMask
    }

    fun enable(capability: RenderingCapabilities)
    fun disable(capability: RenderingCapabilities)
    operator fun set(capability: RenderingCapabilities, status: Boolean)
    operator fun get(capability: RenderingCapabilities): Boolean

    operator fun set(source: BlendingFunctions, destination: BlendingFunctions)

    fun setBlendFunc(sourceRGB: BlendingFunctions, destinationRGB: BlendingFunctions, sourceAlphaFactor: BlendingFunctions, destinationAlphaFactor: BlendingFunctions)

    var depth: DepthFunctions
    var depthMask: Boolean

    var polygonMode: PolygonModes


    val usedVRAM: Long
    val availableVRAM: Long
    val maximumVRAM: Long

    fun readPixels(start: Vec2i, end: Vec2i, type: PixelTypes): ByteBuffer


    fun createShader(resourceLocation: ResourceLocation): Shader

    fun createVertexBuffer(structure: MeshStruct, data: FloatArray, primitiveType: PrimitiveTypes = PrimitiveTypes.TRIANGLE): FloatVertexBuffer
    fun createIntUniformBuffer(bindingIndex: Int = 0, data: IntArray = IntArray(0)): IntUniformBuffer
    fun createFloatUniformBuffer(bindingIndex: Int = 0, data: FloatArray = FloatArray(0)): FloatUniformBuffer
}
