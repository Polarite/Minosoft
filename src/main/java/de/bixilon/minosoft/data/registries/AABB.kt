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

package de.bixilon.minosoft.data.registries

import com.google.gson.JsonObject
import de.bixilon.minosoft.data.Axes
import de.bixilon.minosoft.data.direction.Directions
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.EMPTY
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.ONE
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.get
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3Util.toVec3
import de.bixilon.minosoft.gui.rendering.util.vec.vec3.Vec3dUtil.ONE
import de.bixilon.minosoft.util.MMath.ceil
import de.bixilon.minosoft.util.MMath.floor
import glm_.Java.Companion.glm
import glm_.vec3.Vec3
import glm_.vec3.Vec3d
import glm_.vec3.Vec3i
import glm_.vec3.Vec3t


class AABB {
    val min: Vec3d
    val max: Vec3d

    constructor(jsonData: JsonObject) : this(jsonData["from"].toVec3(Vec3.EMPTY), jsonData["to"].toVec3(Vec3.ONE))

    constructor(jsonData: Map<String, Any>) : this(jsonData["from"]!!.toVec3(Vec3.EMPTY), jsonData["to"]!!.toVec3(Vec3.ONE))

    constructor(aabb: AABB) : this(aabb.min, aabb.max)

    constructor(min: Vec3, max: Vec3) : this(Vec3d(min), Vec3d(max))

    constructor(min: Vec3d, max: Vec3d) {
        this.min = Vec3d(glm.min(min.x, max.x), glm.min(min.y, max.y), glm.min(min.z, max.z))
        this.max = Vec3d(glm.max(min.x, max.x), glm.max(min.y, max.y), glm.max(min.z, max.z))
    }

    private constructor(unsafe: Boolean, min: Vec3d, max: Vec3d) {
        this.min = min
        this.max = max
    }

    fun intersect(other: AABB): Boolean {
        return (min.x < other.max.x && max.x > other.min.x) && (min.y < other.max.y && max.y > other.min.y) && (min.z < other.max.z && max.z > other.min.z)
    }

    operator fun plus(other: Vec3t<out Number>): AABB = offset(other)

    fun offset(other: Vec3t<out Number>): AABB {
        return AABB(true, min + other, max + other)
    }

    operator fun plus(other: AABB): AABB {
        val newMin = Vec3d(glm.min(min.x, other.min.x), glm.min(min.y, other.min.y), glm.min(min.z, other.min.z))
        val newMax = Vec3d(glm.max(max.x, other.max.x), glm.max(max.y, other.max.y), glm.max(max.z, other.max.z))
        return AABB(true, newMin, newMax)
    }

    val blockPositions: List<Vec3i>
        get() {
            val xRange = getRange(min.x, max.x)
            val yRange = getRange(min.y, max.y)
            val zRange = getRange(min.z, max.z)

            val result: MutableList<Vec3i> = mutableListOf()
            for (x in xRange) {
                for (y in yRange) {
                    for (z in zRange) {
                        result += Vec3i(x, y, z)
                    }
                }
            }
            return result.toList()
        }

    private fun min(axis: Axes): Double {
        return min[axis]
    }

    private fun max(axis: Axes): Double {
        return max[axis]
    }


    fun extend(vec3: Vec3d): AABB {
        val newMin = Vec3d(min)
        val newMax = Vec3d(max)

        if (vec3.x < 0) {
            newMin.x += vec3.x
        } else {
            newMax.x += vec3.x
        }

        if (vec3.y < 0) {
            newMin.y += vec3.y
        } else {
            newMax.y += vec3.y
        }

        if (vec3.z < 0) {
            newMin.z += vec3.z
        } else {
            newMax.z += vec3.z
        }

        return AABB(true, newMin, newMax)
    }

    fun extend(vec3i: Vec3i): AABB {
        return this.extend(Vec3d(vec3i))
    }

    fun extend(direction: Directions): AABB {
        return this.extend(direction.vectord)
    }

    fun grow(size: Double = 1.0E-7): AABB {
        return AABB(min - size, max + size)
    }

    fun grow(size: Float): AABB {
        return AABB(min - size, max + size)
    }

    fun computeOffset(other: AABB, offset: Double, axis: Axes): Double {
        if (!offset(axis, offset).intersect(other)) {
            return offset
        }
        val thisMin = min(axis)
        val thisMax = max(axis)
        val otherMin = other.min(axis)
        val otherMax = other.max(axis)
        if (offset > 0 && thisMin <= otherMax + offset) {
            return glm.min(thisMin - otherMax, offset)
        }
        if (offset < 0 && thisMax >= otherMin + offset) {
            return glm.max(thisMax - otherMin, offset)
        }
        return offset
    }

    fun plusAssign(x: Float, y: Float, z: Float) {
        this += Vec3(x, y, z)
    }

    operator fun plusAssign(vec3: Vec3) {
        min += vec3
        max += vec3
    }

    operator fun plusAssign(vec3d: Vec3d) {
        min += vec3d
        max += vec3d
    }

    fun offset(axis: Axes, offset: Double): AABB {
        return when (axis) {
            Axes.X -> this + Vec3d(-offset, 0, 0)
            Axes.Y -> this + Vec3d(0, -offset, 0)
            Axes.Z -> this + Vec3d(0, 0, -offset)
        }
    }

    fun raycast(position: Vec3d, direction: Vec3d): Double {
        if (max - min == Vec3d.ONE || position in this) {
            return 0.0
        }
        var tMin = 0.0
        var tMax = +100.0
        for (axis in Axes.VALUES) {
            val t1 = getLengthMultiplier(position, direction, min, axis)
            val t2 = getLengthMultiplier(position, direction, max, axis)
            tMin = glm.max(tMin, glm.min(t1, t2))
            tMax = glm.min(tMax, glm.max(t1, t2))
        }
        return if (tMax > tMin) {
            tMin
        } else {
            Double.MAX_VALUE
        }
    }

    private fun getLengthMultiplier(position: Vec3d, direction: Vec3d, target: Vec3d, axis: Axes): Double {
        return (target[axis] - position[axis]) / direction[axis]
    }

    operator fun contains(position: Vec3d): Boolean {
        return (position.x in min.x..max.x && position.y in min.y..max.y && position.z in min.z..max.z)
    }

    fun shrink(size: Float): AABB {
        return grow(-size)
    }

    fun hShrink(size: Float): AABB {
        val vec = Vec3d(size, 0.0, size)
        return AABB(min + vec, max - vec)
    }

    fun shrink(size: Double = 1.0E-7): AABB {
        return grow(-size)
    }

    val center: Vec3d
        get() = Vec3d((min.x + max.x) / 2.0, (min.y + max.y) / 2.0, (min.z + max.z) / 2.0)


    override fun hashCode(): Int {
        return min.hashCode() + max.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (this.hashCode() != other?.hashCode()) {
            return false
        }
        if (other !is AABB) {
            return false
        }
        return min == other.min && max == other.max
    }

    override fun toString(): String {
        return "AABB[$min -> $max]"
    }

    companion object {
        val BLOCK: AABB = AABB(Vec3.EMPTY, Vec3.ONE)
        val EMPTY: AABB = AABB(Vec3.EMPTY, Vec3.EMPTY)

        private fun getRange(min: Double, max: Double): IntRange {
            return IntRange(min.floor, max.ceil - 1)
        }
    }
}
