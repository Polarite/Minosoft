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

package de.bixilon.minosoft.gui.rendering.gui.hud.elements.chat

import de.bixilon.kotlinglm.vec2.Vec2i
import de.bixilon.kutil.concurrent.pool.DefaultThreadPool
import de.bixilon.minosoft.config.profile.delegate.watcher.SimpleProfileDelegateWatcher.Companion.profileWatchRendering
import de.bixilon.minosoft.gui.rendering.gui.GUIRenderer
import de.bixilon.minosoft.gui.rendering.gui.elements.Element
import de.bixilon.minosoft.modding.event.events.InternalMessageReceiveEvent
import de.bixilon.minosoft.modding.event.invoker.CallbackEventInvoker

class InternalChatElement(guiRenderer: GUIRenderer) : AbstractChatElement(guiRenderer) {
    private val chatProfile = profile.chat.internal
    private var active = false
        set(value) {
            field = value
            messages._active = value
            messages.forceSilentApply()
            forceApply()
        }
    override var skipDraw: Boolean // skips hud draw and draws it in gui stage
        get() = chatProfile.hidden || active
        set(value) {
            chatProfile.hidden = !value
        }
    override val activeWhenHidden: Boolean
        get() = true

    init {
        messages.prefMaxSize = Vec2i(chatProfile.width, chatProfile.height)
        chatProfile::width.profileWatchRendering(this, profile = profile, context = renderWindow) { messages.prefMaxSize = Vec2i(it, messages.prefMaxSize.y) }
        chatProfile::height.profileWatchRendering(this, profile = profile, context = renderWindow) { messages.prefMaxSize = Vec2i(messages.prefMaxSize.x, it) }
        forceSilentApply()
    }


    override fun init() {
        connection.registerEvent(CallbackEventInvoker.of<InternalMessageReceiveEvent> {
            if (profile.chat.internal.hidden) {
                return@of
            }
            DefaultThreadPool += { messages += it.message.text }
        })
    }

    override fun forceSilentApply() {
        messages.silentApply()
        _size = Vec2i(messages.prefMaxSize.x, messages.size.y + ChatElement.CHAT_INPUT_MARGIN * 2)
        cacheUpToDate = false
    }

    override fun onOpen() {
        active = true
        messages.onOpen()
    }

    override fun onClose() {
        active = false
        messages.onClose()
    }

    override fun getAt(position: Vec2i): Pair<Element, Vec2i>? {
        if (position.x < ChatElement.CHAT_INPUT_MARGIN) {
            return null
        }
        val offset = Vec2i(position)
        offset.x -= ChatElement.CHAT_INPUT_MARGIN

        val messagesSize = messages.size
        if (offset.y < messagesSize.y) {
            if (offset.x > messagesSize.x) {
                return null
            }
            return Pair(messages, offset)
        }
        offset.y -= messagesSize.y
        return null
    }

    override fun onChildChange(child: Element) {
        forceSilentApply()
        super.onChildChange(child)
    }
}
