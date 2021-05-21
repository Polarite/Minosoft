/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */
package de.bixilon.minosoft.protocol.packets.s2c.play

import de.bixilon.minosoft.data.mappings.particle.ParticleType
import de.bixilon.minosoft.data.mappings.particle.data.ParticleData
import de.bixilon.minosoft.modding.event.events.ParticleSpawnEvent
import de.bixilon.minosoft.protocol.network.connection.PlayConnection
import de.bixilon.minosoft.protocol.packets.s2c.PlayS2CPacket
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer
import de.bixilon.minosoft.protocol.protocol.ProtocolVersions
import de.bixilon.minosoft.util.logging.Log
import de.bixilon.minosoft.util.logging.LogLevels
import de.bixilon.minosoft.util.logging.LogMessageType
import glm_.vec3.Vec3

class ParticleS2CP(buffer: PlayInByteBuffer) : PlayS2CPacket() {
    val type: ParticleType = if (buffer.versionId < ProtocolVersions.V_14W19A) {
        buffer.connection.registries.particleTypeRegistry[buffer.readResourceLocation()]!!
    } else {
        buffer.connection.registries.particleTypeRegistry[buffer.readInt()]
    }
    val longDistance = if (buffer.versionId >= ProtocolVersions.V_14W29A) {
        buffer.readBoolean()
    } else {
        false
    }
    val position: Vec3 = if (buffer.versionId < ProtocolVersions.V_1_15_PRE4) {
        buffer.readFloatPosition()
    } else {
        buffer.readPosition()
    }
    val offset: Vec3 = buffer.readFloatPosition()
    val data: Float = buffer.readFloat()
    val count: Int = buffer.readInt()
    val particleData: ParticleData = buffer.readParticleData(type)


    override fun handle(connection: PlayConnection) {
        if (connection.fireEvent(ParticleSpawnEvent(connection, this))) {
            return
        }
    }

    override fun log() {
        Log.log(LogMessageType.NETWORK_PACKETS_IN, level = LogLevels.VERBOSE) { "ParticleType (type=$type, longDistance=$longDistance, position=$position, offset=$offset, data=$data, count=$count, particleData=$particleData)" }
    }
}
