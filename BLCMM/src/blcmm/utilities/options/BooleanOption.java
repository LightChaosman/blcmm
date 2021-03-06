/*
 * Copyright (C) 2018-2020  LightChaosman
 *
 * BLCMM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package blcmm.utilities.options;

import blcmm.gui.panels.ToolSettingsPanel;
import blcmm.utilities.Options;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 * A boolean option
 *
 * @author apocalyptech
 */
public class BooleanOption extends Option<Boolean> {

    /**
     * Constructor for a boolean option which will not be displayed on the
     * settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     */
    public BooleanOption(String name, boolean defaultData) {
        super(name, defaultData);
    }

    /**
     * Constructor for a boolean option. If displayDesc is null, the option will
     * not be shown on the settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     * @param displayDesc Display description on the settings panel
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     */
    public BooleanOption(String name,
            boolean defaultData,
            String displayDesc,
            String callback,
            String tooltip) {
        super(name, defaultData, displayDesc, callback, tooltip);
    }

    /**
     * Converts the given string (read from a config file) into the correct data
     * type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    @Override
    public Boolean stringToData(String stringData) {
        return Boolean.parseBoolean(stringData);
    }

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    @Override
    public String dataToString() {
        return Boolean.toString(this.getData());
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        BooleanOption option = this;
        JCheckBox check = new JCheckBox();
        check.setSelected(this.getData());
        check.setHorizontalAlignment(SwingConstants.RIGHT);
        check.addActionListener(ae -> {
            setData(check.isSelected());
            panel.callBack(option, check);
            Options.INSTANCE.save();
        });
        return check;
    }

}
