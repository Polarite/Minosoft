/*
 * Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.protocol.packets.clientbound.play;

import de.bixilon.minosoft.protocol.network.Connection;
import de.bixilon.minosoft.protocol.packets.ClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.InByteBuffer;
import de.bixilon.minosoft.util.logging.Log;
import glm_.vec2.Vec2i;

public class PacketUnloadChunk extends ClientboundPacket {
    private Vec2i chunkPosition;

    @Override
    public boolean read(InByteBuffer buffer) {
        this.chunkPosition = new Vec2i(buffer.readInt(), buffer.readInt());
        return true;
    }

    @Override
    public void handle(Connection connection) {
        connection.getPlayer().getWorld().unloadChunk(getChunkPosition());
        connection.getRenderer().getRenderWindow().getWorldRenderer().unloadChunk(this.chunkPosition);
    }

    @Override
    public void log() {
        Log.protocol(String.format("[IN] Received unload chunk packet (chunkPosition=%s)", this.chunkPosition));
    }

    public Vec2i getChunkPosition() {
        return this.chunkPosition;
    }
}
