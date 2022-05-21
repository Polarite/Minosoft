/*
 * Minosoft
 * Copyright (C) 2020-2022 Moritz Zwerger
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * This software is not affiliated with Mojang AB, the original developer of Minecraft.
 */

package de.bixilon.minosoft.terminal.commands

import de.bixilon.jiibles.Table
import de.bixilon.kutil.collections.CollectionUtil.toSynchronizedList
import de.bixilon.minosoft.commands.nodes.LiteralNode
import de.bixilon.minosoft.protocol.network.connection.play.PlayConnection

object ConnectionManageCommand : Command {
    override var node = LiteralNode("connection")
        .addChild(LiteralNode("list", executor = {
            val table = Table(arrayOf("Id", "State", "Address"))
            for (connection in PlayConnection.ACTIVE_CONNECTIONS.toSynchronizedList()) {
                table += arrayOf(connection.connectionId, connection.state, connection.address)
            }
            it.print.print(table)
        }))
}
