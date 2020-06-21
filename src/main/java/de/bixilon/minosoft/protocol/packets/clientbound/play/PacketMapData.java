/*
 * Codename Minosoft
 * Copyright (C) 2020 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.protocol.packets.clientbound.play;

import de.bixilon.minosoft.logging.Log;
import de.bixilon.minosoft.protocol.packets.ClientboundPacket;
import de.bixilon.minosoft.protocol.protocol.InPacketBuffer;
import de.bixilon.minosoft.protocol.protocol.PacketHandler;
import de.bixilon.minosoft.protocol.protocol.ProtocolVersion;
import de.bixilon.minosoft.util.BitByte;

import java.util.ArrayList;
import java.util.List;

public class PacketMapData implements ClientboundPacket {
    int mapId;
    PacketMapDataData dataData;

    // depends on data
    // start
    byte xStart;
    byte yStart;
    byte[] colors;

    // players
    List<MapPlayerSet> players;

    //scale
    byte scale;


    @Override
    public void read(InPacketBuffer buffer, ProtocolVersion v) {
        switch (v) {
            case VERSION_1_7_10:
                mapId = buffer.readVarInt(); // mapId
                short length = buffer.readShort();
                // read action
                dataData = PacketMapDataData.byId(buffer.readByte());
                switch (dataData) {
                    case START:
                        xStart = buffer.readByte();
                        yStart = buffer.readByte();
                        colors = buffer.readBytes(length - 3); // 3: dataData(1) + xStart (1) + yStart (1)
                        break;
                    case PLAYERS:
                        players = new ArrayList<>();
                        length--; // minus the dataData
                        for (int i = 0; i < length / 3; i++) { // loop over all sets ( 1 set: 3 bytes)
                            byte data = buffer.readByte();
                            byte type = BitByte.getLow4Bits(data);
                            MapPlayerDirection direction = MapPlayerDirection.byId(BitByte.getHigh4Bits(data));
                            byte x = buffer.readByte();
                            byte y = buffer.readByte();
                            players.add(new MapPlayerSet(type, direction, x, y));
                        }
                        break;
                    case SCALE:
                        scale = buffer.readByte();
                        return;
                }
                break;
            case VERSION_1_8:
                //ToDo
                break;
        }
    }

    @Override
    public void log() {
        Log.protocol(String.format("Received map meta data (mapId=%d)", mapId));
    }

    @Override
    public void handle(PacketHandler h) {
        h.handle(this);
    }

    public PacketMapDataData getDataData() {
        return dataData;
    }

    public byte getXStart() {
        return xStart;
    }

    public byte getYStart() {
        return yStart;
    }

    public byte[] getColors() {
        return colors;
    }


    public List<MapPlayerSet> getPlayers() {
        return players;
    }
    

    public byte getScale() {
        return scale;
    }

    public enum PacketMapDataData {
        START(0),
        PLAYERS(1),
        SCALE(2);

        final int id;

        PacketMapDataData(int id) {
            this.id = id;
        }

        public static PacketMapDataData byId(int id) {
            for (PacketMapDataData d : values()) {
                if (d.getId() == id) {
                    return d;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }


    public enum MapPlayerDirection {
        //ToDo
        TO_DO(0);

        final int id;

        MapPlayerDirection(int id) {
            this.id = id;
        }

        public static MapPlayerDirection byId(int id) {
            for (MapPlayerDirection d : values()) {
                if (d.getId() == id) {
                    return d;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }

    public class MapPlayerSet {
        final int type;
        final MapPlayerDirection direction;
        byte x;
        byte z;

        public MapPlayerSet(int type, MapPlayerDirection direction, byte x, byte z) {
            this.type = type;
            this.direction = direction;
            this.x = x;
            this.z = z;
        }

        public int getType() {
            return type;
        }

        public MapPlayerDirection getDirection() {
            return direction;
        }

        public byte getX() {
            return x;
        }

        public byte getZ() {
            return z;
        }
    }
}
