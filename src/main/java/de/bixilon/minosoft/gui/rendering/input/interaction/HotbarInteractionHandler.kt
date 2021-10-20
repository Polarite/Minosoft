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

package de.bixilon.minosoft.gui.rendering.input.interaction

import de.bixilon.minosoft.Minosoft
import de.bixilon.minosoft.config.key.KeyAction
import de.bixilon.minosoft.config.key.KeyBinding
import de.bixilon.minosoft.config.key.KeyCodes
import de.bixilon.minosoft.data.registries.other.containers.PlayerInventory
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.modding.events.input.MouseScrollEvent
import de.bixilon.minosoft.modding.event.EventInitiators
import de.bixilon.minosoft.modding.event.events.SelectHotbarSlotEvent
import de.bixilon.minosoft.modding.event.invoker.CallbackEventInvoker
import de.bixilon.minosoft.protocol.packets.c2s.play.HotbarSlotSetC2SP
import de.bixilon.minosoft.util.KUtil.toResourceLocation

class HotbarInteractionHandler(
    val renderWindow: RenderWindow,
) {
    private val connection = renderWindow.connection

    private var currentScrollOffset = 0.0


    fun selectSlot(slot: Int) {
        // ToDo: Rate limit?
        if (connection.player.selectedHotbarSlot == slot) {
            return
        }
        connection.player.selectedHotbarSlot = slot
        connection.sendPacket(HotbarSlotSetC2SP(slot))
        connection.fireEvent(SelectHotbarSlotEvent(connection, EventInitiators.CLIENT, slot))
    }


    fun init() {
        for (i in 1..PlayerInventory.HOTBAR_SLOTS) {
            renderWindow.inputHandler.registerKeyCallback("minosoft:hotbar_slot_$i".toResourceLocation(), KeyBinding(
                mutableMapOf(
                    KeyAction.PRESS to mutableSetOf(KeyCodes.KEY_CODE_MAP["$i"]!!),
                ),
            )) {
                selectSlot(i - 1)
            }
        }

        connection.registerEvent(CallbackEventInvoker.of<MouseScrollEvent> {
            currentScrollOffset += it.offset.y

            val limit = Minosoft.config.config.game.controls.hotbarScrollSensitivity
            var nextSlot = connection.player.selectedHotbarSlot
            if (currentScrollOffset >= limit && currentScrollOffset > 0) {
                nextSlot--
            } else if (currentScrollOffset <= -limit && currentScrollOffset < 0) {
                nextSlot++
            } else {
                return@of
            }
            currentScrollOffset = 0.0
            if (nextSlot < 0) {
                nextSlot = PlayerInventory.HOTBAR_SLOTS - 1
            } else if (nextSlot > PlayerInventory.HOTBAR_SLOTS - 1) {
                nextSlot = 0
            }

            selectSlot(nextSlot)
        })
    }
}
