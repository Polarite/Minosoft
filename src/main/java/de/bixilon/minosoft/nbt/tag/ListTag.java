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

package de.bixilon.minosoft.nbt.tag;

import de.bixilon.minosoft.protocol.protocol.InByteBuffer;
import de.bixilon.minosoft.protocol.protocol.OutByteBuffer;

import java.util.ArrayList;
import java.util.List;

public class ListTag implements Tag {
    final TagTypes type;
    final List<Tag> list;

    public ListTag(TagTypes type, List<Tag> list) {
        this.type = type;
        this.list = list;
    }

    public ListTag(InByteBuffer buffer) {
        this.type = TagTypes.getById(new ByteTag(buffer).getValue());
        int length = new IntTag(buffer).getValue();
        list = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            switch (type) {
                case BYTE:
                    list.add(new ByteTag(buffer));
                    break;
                case SHORT:
                    list.add(new ShortTag(buffer));
                    break;
                case INT:
                    list.add(new IntTag(buffer));
                    break;
                case LONG:
                    list.add(new LongTag(buffer));
                    break;
                case FLOAT:
                    list.add(new FloatTag(buffer));
                    break;
                case DOUBLE:
                    list.add(new DoubleTag(buffer));
                    break;
                case BYTE_ARRAY:
                    list.add(new ByteArrayTag(buffer));
                    break;
                case STRING:
                    list.add(new StringTag(buffer));
                    break;
                case LIST:
                    list.add(new ListTag(buffer));
                    break;
                case COMPOUND:
                    list.add(new CompoundTag(true, buffer));
                    break;
            }
        }

    }

    @Override
    public TagTypes getType() {
        return TagTypes.LIST;
    }

    @Override
    public void writeBytes(OutByteBuffer buffer) {
        new ByteTag((byte) type.getId()).writeBytes(buffer);

        new IntTag(list.size()).writeBytes(buffer);

        for (Tag tag : list) {
            tag.writeBytes(buffer);
        }
    }

    public List<Tag> getValue() {
        return list;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Tag tag : list) {
            builder.append(tag);
        }
        builder.delete(builder.length() - 1, builder.length()); // delete last comma
        builder.append("]");

        return builder.toString();
    }
}
