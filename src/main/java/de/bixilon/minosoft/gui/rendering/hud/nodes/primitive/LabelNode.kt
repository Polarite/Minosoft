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

package de.bixilon.minosoft.gui.rendering.hud.nodes.primitive

import de.bixilon.minosoft.data.text.ChatComponent
import de.bixilon.minosoft.data.text.RGBColor
import de.bixilon.minosoft.gui.rendering.RenderConstants
import de.bixilon.minosoft.gui.rendering.RenderWindow
import de.bixilon.minosoft.gui.rendering.font.Font
import de.bixilon.minosoft.gui.rendering.hud.nodes.layout.AbsoluteLayout
import de.bixilon.minosoft.gui.rendering.hud.nodes.properties.NodeSizing
import glm_.vec2.Vec2i

class LabelNode(
    renderWindow: RenderWindow,
    sizing: NodeSizing = NodeSizing(minSize = Vec2i(0, Font.CHAR_HEIGHT)),
    text: ChatComponent = ChatComponent.valueOf(raw = ""),
    var background: Boolean = true,
) : AbsoluteLayout(renderWindow, sizing) {
    private var _text: ChatComponent = text

    var text: ChatComponent
        get() = _text
        set(value) {
            _text = value
            prepare()
            apply()
        }
    var sText: String
        get() = text.message
        set(value) {
            text = ChatComponent.valueOf(raw = value)
        }

    init {
        prepare()
    }

    private fun prepare() {
        clearChildren()
        val textSize = Vec2i(0, 0)
        text.prepareRender(Vec2i(1, 1), Vec2i(), renderWindow, this, 1, textSize)
        apply()

        if (background) {
            drawBackground(textSize + 1)
        }
    }

    private fun drawBackground(end: Vec2i, z: Int = 1, tintColor: RGBColor = RenderConstants.TEXT_BACKGROUND_COLOR) {
        addChild(Vec2i(0, 0), ImageNode(renderWindow, NodeSizing(minSize = end), renderWindow.WHITE_TEXTURE, 0, tintColor))
    }
}
