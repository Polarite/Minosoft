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

import de.bixilon.minosoft.data.Tag;
import de.bixilon.minosoft.data.mappings.ResourceLocation;
import de.bixilon.minosoft.protocol.packets.clientbound.PlayClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.PlayInByteBuffer;
import de.bixilon.minosoft.util.logging.Log;

import static de.bixilon.minosoft.protocol.protocol.ProtocolVersions.*;

public class PacketTags extends PlayClientboundPacket {
    private Tag[] blockTags = {};
    private Tag[] itemTags = {};
    private Tag[] fluidTags = {};
    private Tag[] entityTags = {};
    private Tag[] gameEventTags = {};

    public PacketTags(PlayInByteBuffer buffer) {
        if (buffer.getVersionId() < V_20W51A) {
            this.blockTags = readTags(buffer);
            this.itemTags = readTags(buffer);
            this.fluidTags = readTags(buffer); // ToDo: when was this added? Was not available in 18w01
            if (buffer.getVersionId() >= V_18W43A) {
                this.entityTags = readTags(buffer);
            }
            if (buffer.getVersionId() >= V_20W49A) {
                this.gameEventTags = readTags(buffer);
            }
            return;
        }
        int length = buffer.readVarInt();
        for (int i = 0; i < length; i++) {
            ResourceLocation resourceLocation = buffer.readResourceLocation();
            switch (resourceLocation.getFull()) {
                case "minecraft:block" -> this.blockTags = readTags(buffer);
                case "minecraft:item" -> this.itemTags = readTags(buffer);
                case "minecraft:fluid" -> this.fluidTags = readTags(buffer);
                case "minecraft:entity_type" -> this.entityTags = readTags(buffer);
                case "minecraft:game_event" -> this.gameEventTags = readTags(buffer);
            }
        }
    }

    private Tag[] readTags(PlayInByteBuffer buffer) {
        Tag[] ret = new Tag[buffer.readVarInt()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = new Tag(buffer.readResourceLocation(), buffer.readVarIntArray());
        }
        return ret;
    }

    @Override
    public void log() {
        Log.protocol(String.format("[IN] Received tags (blockLength=%d, itemLength=%d, fluidLength=%d, entityLength=%d, gameEventLength=%d)", this.blockTags.length, this.itemTags.length, this.fluidTags.length, this.entityTags.length, this.gameEventTags.length));
    }
}
