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
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * An Integer setting.
 *
 * @author apocalyptech
 */
public class IntOption extends Option<Integer> {

    private final int minValue;

    private final int maxValue;

    /**
     * Constructor for an integer option which will not be displayed on the
     * settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     */
    public IntOption(String name, int defaultData) {
        super(name, defaultData);
        this.minValue = -1;
        this.maxValue = -1;
    }

    /**
     * Constructor for an integer option. If displayDesc is null, the option
     * will not be shown on the settings panel.
     *
     * @param name Key for the option
     * @param defaultData Default value for the option
     * @param displayDesc Display description on the settings panel
     * @param minValue Minimum value for the spin button
     * @param maxValue Maximum value for the spin button
     * @param callback Callback to use when the option is changed
     * @param tooltip Tooltip to show on the control
     */
    public IntOption(String name,
            int defaultData,
            String displayDesc,
            int minValue,
            int maxValue,
            String callback,
            String tooltip) {
        super(name, defaultData, displayDesc, callback, tooltip);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * @return the minValue
     */
    public int getMinValue() {
        return minValue;
    }

    /**
     * @return the maxValue
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Sets the data for this option, enforcing our min/max levels if they have
     * been set.
     *
     * @param newData The new value to set.
     */
    @Override
    public void setData(Integer newData) {
        if (this.isDisplayOnSettingsPanel()) {
            if (newData < this.minValue) {
                newData = this.minValue;
            } else if (newData > this.maxValue) {
                newData = this.maxValue;
            }
        }
        super.setData(newData);
    }

    /**
     * Converts the given string (read from a config file) into the correct data
     * type for this Option.
     *
     * @param stringData The string data to convert
     * @return The data in its proper format
     */
    @Override
    public Integer stringToData(String stringData) {
        return Integer.parseInt(stringData);
    }

    /**
     * Converts the current data for this option into a String suitable for
     * saving to a text-based options file.
     *
     * @return A string representation of our data
     */
    @Override
    public String dataToString() {
        return Integer.toString(this.getData());
    }

    /**
     * Return a JComponent for this option, for use in the settings panel.
     *
     * @param panel The ToolSettingsPanel object we are being added to
     * @return A suitable JComponent
     */
    @Override
    public JComponent getGUIComponent(ToolSettingsPanel panel) {
        IntOption option = this;
        JSpinner spin = new JSpinner(new SpinnerNumberModel(
                (int) this.getData(),
                (int) this.getMinValue(),
                (int) this.getMaxValue(),
                1));
        spin.addChangeListener(ce -> {
            setData((int) spin.getValue());
            panel.callBack(option, spin);
            Options.INSTANCE.save();
        });
        return spin;
    }

}
