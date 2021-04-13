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

package de.bixilon.minosoft.protocol.packets.clientbound.play;

import de.bixilon.minosoft.protocol.packets.clientbound.PlayClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer;
import de.bixilon.minosoft.util.logging.Log;

public class PacketConfirmTransactionReceiving extends PlayClientboundPacket {
    private final byte windowId;
    private final short actionNumber;
    private final boolean accepted;

    public PacketConfirmTransactionReceiving(PlayInByteBuffer buffer) {
        this.windowId = buffer.readByte();
        this.actionNumber = buffer.readShort();
        this.accepted = buffer.readBoolean();
    }

    @Override
    public void log() {
        Log.protocol(String.format("[IN] Window transaction receiving (windowId=%d, actionNumber=%s, accepted=%s)", this.windowId, this.actionNumber, this.accepted));
    }

    public byte getWindowId() {
        return this.windowId;
    }

    public boolean wasAccepted() {
        return this.accepted;
    }

    public short getActionNumber() {
        return this.actionNumber;
    }
}
