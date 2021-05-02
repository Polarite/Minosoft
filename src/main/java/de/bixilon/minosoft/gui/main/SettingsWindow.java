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

package de.bixilon.minosoft.gui.main;

import de.bixilon.minosoft.data.locale.LocaleManager;
import de.bixilon.minosoft.data.locale.Strings;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsWindow implements Initializable {
    public GridPane tabGeneral;
    public Tab general;
    public Tab download;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        this.general.setText(LocaleManager.translate(Strings.SETTINGS_GENERAL));
        this.download.setText(LocaleManager.translate(Strings.SETTINGS_DOWNLOAD));
    }
}
