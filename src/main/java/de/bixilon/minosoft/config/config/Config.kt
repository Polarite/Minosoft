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

package de.bixilon.minosoft.config.config

import de.bixilon.minosoft.config.config.account.AccountConfig
import de.bixilon.minosoft.config.config.chat.ChatConfig
import de.bixilon.minosoft.config.config.debug.DebugConfig
import de.bixilon.minosoft.config.config.download.DownloadConfig
import de.bixilon.minosoft.config.config.game.GameConfig
import de.bixilon.minosoft.config.config.general.GeneralConfig
import de.bixilon.minosoft.config.config.network.NetworkConfig
import de.bixilon.minosoft.config.config.server.ServerConfig

data class Config(
    val general: GeneralConfig = GeneralConfig(),
    val game: GameConfig = GameConfig(),
    val chat: ChatConfig = ChatConfig(),
    val network: NetworkConfig = NetworkConfig(),
    val account: AccountConfig = AccountConfig(),
    val server: ServerConfig = ServerConfig(),
    val download: DownloadConfig = DownloadConfig(),
    val debug: DebugConfig = DebugConfig(),
)
