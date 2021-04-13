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

package de.bixilon.minosoft.protocol.packets.serverbound.login;

import de.bixilon.minosoft.protocol.packets.serverbound.PlayServerboundPacket;
import de.bixilon.minosoft.protocol.protocol.OutPlayByteBuffer;
import de.bixilon.minosoft.util.logging.Log;

public class LoginPluginResponseServerboundPacket implements PlayServerboundPacket {
    private final int messageId;
    private final boolean successful;
    byte[] data;

    public LoginPluginResponseServerboundPacket(int messageId, boolean successful) {
        this.messageId = messageId;
        this.successful = successful;
    }

    public LoginPluginResponseServerboundPacket(int messageId, byte[] data) {
        this.messageId = messageId;
        this.successful = true;
        this.data = data;
    }

    @Override
    public void write(OutPlayByteBuffer buffer) {
        buffer.writeVarInt(this.messageId);
        buffer.writeBoolean(this.successful);
        if (this.successful) {
            buffer.writeBytes(this.data);
        }
    }

    @Override
    public void log() {
        Log.protocol(String.format("[OUT] Sending login plugin response (messageId=%d, successful=%s)", this.messageId, this.successful));
    }
}
