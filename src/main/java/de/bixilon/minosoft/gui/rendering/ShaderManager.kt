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

package de.bixilon.minosoft.gui.rendering

import de.bixilon.minosoft.gui.rendering.system.base.shader.Shader.Companion.loadAnimated
import de.bixilon.minosoft.util.KUtil.minosoft

class ShaderManager(
    val renderWindow: RenderWindow,
) {
    val genericColorShader = renderWindow.renderSystem.createShader(minosoft("generic/color"))
    val genericTextureShader = renderWindow.renderSystem.createShader(minosoft("generic/texture"))
    val genericTexture2dShader = renderWindow.renderSystem.createShader(minosoft("generic/texture_2d"))


    fun postInit() {
        genericColorShader.load()
        genericTextureShader.loadAnimated()
        genericTexture2dShader.loadAnimated()
    }
}
