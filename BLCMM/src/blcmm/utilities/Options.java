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
package blcmm.utilities;

import blcmm.gui.theme.Theme;
import blcmm.gui.theme.ThemeManager;
import blcmm.utilities.options.*;
import general.utilities.StringTable;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to control dealing with the main application options/settings.
 *
 * @author LightChaosman
 */
public class Options {

    public static Options INSTANCE;

    /**
     * Filename to use.
     */
    private final static String DEFAULT_FILENAME = "general.options";

    /**
     * Enum to store the textual keys for our options. This is probably a bit of
     * overkill, but it prevents us from having to worry about keeping the same
     * string synchronized between a few different places in the code.
     */
    public enum OptionNames {
        theme,
        fontsize,
        truncateCommands2,
        truncateCommandLength,
        highlightBVCErrors,
        structuralEdits,
        developerMode,
        leafSelectionAllowed,
        hasSeenExportWarning,
        showConfirmPartialCategory,
        sessionsToKeep,
        backupsPerSession,
        secondsBetweenBackups,
        OELeftPaneVisible,
        mainWindowWidth,
        mainWindowHeight,
        mainWindowMaximized,
        editWindowWidth,
        editWindowHeight,
        oeWindowWidth,
        oeWindowHeight,
        oeWindowMaximized,
        fileHistory,
        lastImport,
        filenameTruncationLength,
        propagateMUTNotification,
        BL2Bookmarks,
        TPSBookmarks,
        popupStatus,
        showHotfixNames,
        dragAndDroppableCode,
        showDeleteConfirmation
    }

    /**
     * A list of old option names which shouldn't be re-saved if we encounter
     * them in the options file. This is a bit silly -- at the moment, there's
     * no reason to keep *any* option that we don't explicitly have defined in
     * the main list. If we end up allowing Plugins to store options, though, we
     * may want to hold on to option names which we don't know about, in case a
     * plugin is temporarily unavailable or something.
     */
    private final HashSet<String> IGNORE_OPTIONS = new HashSet(Arrays.asList(
            "contentEdits",
            "truncateCommands"
    ));

    /**
     * HashMap to hold our options
     */
    private final HashMap<String, Option> OPTION_MAP = new HashMap<>();

    /**
     * ArrayList to hold the order in which we should process options
     */
    private final ArrayList<String> optionOrder = new ArrayList<>();

    /**
     * Structure to save information about any option found in the options file
     * which we don't explicitly know about. Possibly useful if, in the future,
     * there are plugins which add options which aren't loaded yet, or
     * something. At the moment this should probably always be empty, but we're
     * keeping track regardless.
     */
    private final HashMap<String, String> UNKNOWN_OPTIONS = new HashMap<>();

    /**
     * List of Strings which contain load errors which should be reported to the
     * user on app startup. Will be populated by loadFromFile() if necessary.
     */
    private final ArrayList<String> loadErrors = new ArrayList<>();

    /**
     * Construct a fresh Options object.
     */
    public Options() {

        // First up, for simplicity's sake: settings shown on the main
        // settings menu.  The order in which these are registered is the
        // order in which they'll show up in the panel.
        this.registerOption(new SelectionOption<>(
                OptionNames.theme.toString(), ThemeManager.getTheme("dark"),
                "Theme", "setTheme", "Change BLCMM's color theme",
                "checkThemeSwitchAllowed",
                ThemeManager.getAllInstalledThemes().toArray(new Theme[0]), s -> {
            Theme t = ThemeManager.getTheme(s);
            return t == null ? ThemeManager.getTheme("dark") : t;
        }
        ));

        this.registerOption(new IntOption(
                OptionNames.fontsize.toString(), 12,
                "Application font size", 8, 36, "updateFontSizes",
                "Application font size"));

        this.registerOption(new BooleanOption(
                OptionNames.truncateCommands2.toString(), true,
                "Truncate commands in tree", "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new IntOption(
                OptionNames.truncateCommandLength.toString(), 100,
                "Truncate length", 20, 900, "toggleTruncateCommands",
                "Truncate the value field on set commands, to "
                + "reduce horizontal window size."));

        this.registerOption(new BooleanOption(
                OptionNames.highlightBVCErrors.toString(), true,
                "Highlight Incomplete BVC Statements",
                "toggleHighlightBVCErrors",
                "Toggles highlighting of Incomplete BVC/ID/BVA/BVSC "
                + "tuples.  This is technically valid syntax, but discouraged "
                + "for style reasons."));

        this.registerOption(new BooleanOption(
                OptionNames.structuralEdits.toString(), false,
                "Enable structural edits", null,
                "Enables/Disables moving categories around inside the tree, "
                + "and deleting categories."));

        this.registerOption(new BooleanOption(
                OptionNames.developerMode.toString(), false,
                "Enable developer mode", "toggleDeveloperMode",
                "Enables/Disables changing actual mod code, and authoring "
                + "mods inside BLCMM."));

        this.registerOption(new BooleanOption(OptionNames.dragAndDroppableCode.toString(), true,
                "Enable Dragging & Dropping in Text", null,
                "Enables/Disables being able to Drag & Drop"
                + " text into text fields"));

        //Next, the launcher splash screen selector. This requires some extra magic;
        try {
            JarFile launcher = new JarFile(BLCMMUtilities.getLauncher());
            ByteArrayOutputStream result;
            try (BufferedInputStream bis = new BufferedInputStream(launcher.getInputStream(launcher.getJarEntry("resources/BGs.txt")))) {
                result = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = bis.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
            }
            String res = new String(result.toByteArray());
            StringTable table = StringTable.generateTable(res);
            this.registerOption(SelectionOption.createStringSelectionOption("splashImage", "Default", "Splash screen image", null, "The image shown on the launcher", table));
            launcher.close();
        } catch (Exception e) {
            //If we fail, we fail. One reason this can happen is if the launcher doesn't have the specified file in it yet
            e.printStackTrace();
        }

        // Next: options which don't show up on the settings panel.  Order
        // doesn't really matter here.
        // Has the user seen the export warning?
        this.registerOption(new BooleanOption(OptionNames.hasSeenExportWarning.toString(), false));
        this.registerOption(new BooleanOption(OptionNames.leafSelectionAllowed.toString(), false));

        // Show confirmation when checking partially checked categories?
        this.registerOption(new BooleanOption(OptionNames.showConfirmPartialCategory.toString(), true));

        // Backup session information
        this.registerOption(new IntOption(OptionNames.sessionsToKeep.toString(), 5));
        this.registerOption(new IntOption(OptionNames.backupsPerSession.toString(), 10));
        this.registerOption(new IntOption(OptionNames.secondsBetweenBackups.toString(), 120));

        // Is the left pane in OE visible?
        this.registerOption(new BooleanOption(OptionNames.OELeftPaneVisible.toString(), true));

        // Remembered geometry for various windows.
        this.registerOption(new IntOption(OptionNames.mainWindowWidth.toString(), 900));
        this.registerOption(new IntOption(OptionNames.mainWindowHeight.toString(), 630));
        this.registerOption(new BooleanOption(OptionNames.mainWindowMaximized.toString(), false));
        // Edit window is modal, and thus isn't really allowed to be maximized.
        this.registerOption(new IntOption(OptionNames.editWindowWidth.toString(), 830));
        this.registerOption(new IntOption(OptionNames.editWindowHeight.toString(), 560));
        this.registerOption(new IntOption(OptionNames.oeWindowWidth.toString(), 1150));
        this.registerOption(new IntOption(OptionNames.oeWindowHeight.toString(), 670));
        this.registerOption(new BooleanOption(OptionNames.oeWindowMaximized.toString(), false));

        //Remember previously openened files
        this.registerOption(new StringListOption(OptionNames.fileHistory.toString(), new String[]{}));

        // Remember previous import location
        this.registerOption(new FilenameOption(OptionNames.lastImport.toString(), ""));

        // Filename truncation length ("recent" menu and MainGUI window title)
        this.registerOption(new IntOption(OptionNames.filenameTruncationLength.toString(), 60));

        // Whether or not MUT coloration/notification propagates.  This is just
        // for CJ, who apparently won't shut up about it.  :)
        this.registerOption(new BooleanOption(OptionNames.propagateMUTNotification.toString(), true));

        // All of our Object Explorer Bookmarks of queries / objects
        this.registerOption(new StringListOption(OptionNames.BL2Bookmarks.toString(), new String[]{}));
        this.registerOption(new StringListOption(OptionNames.TPSBookmarks.toString(), new String[]{}));

        // The integer storing our 1-time popup messages
        this.registerOption(new IntOption(OptionNames.popupStatus.toString(), 0));

        // A flag determining if we show the hotfix naming checkbox
        this.registerOption(new BooleanOption(OptionNames.showHotfixNames.toString(), true));

        // A flag for if we disabled delete messages.
        this.registerOption(new BooleanOption(OptionNames.showDeleteConfirmation.toString(), true));
    }

    /**
     * Registers an Option with ourselves.
     *
     * @param newOption The new Option to set
     * @return
     */
    public final boolean registerOption(Option newOption) {
        if (OPTION_MAP.containsKey(newOption.getName())) {
            return false;
        } else {
            OPTION_MAP.put(newOption.getName(), newOption);
            optionOrder.add(newOption.getName());
            return true;
        }
    }

    /**
     * Retrieves an Option, given an OptionNames enum entry.
     *
     * @param name The name of the option to return
     * @return The option
     */
    public Option getOption(OptionNames name) {
        return this.getOption(name.toString());
    }

    /**
     * Retrieves an Option, given its name.
     *
     * @param name The name of the option to return
     * @return The option
     */
    public Option getOption(String name) {
        return OPTION_MAP.getOrDefault(name, null);
    }

    /**
     * Returns an ArrayList of Options in the order in which they should be
     * displayed on the settings screen. Will omit any Option objects which are
     * not for display.
     *
     * @return The ArrayList of Options.
     */
    public ArrayList<Option> getDisplayedOptionList() {
        ArrayList<Option> retList = new ArrayList<>();
        Option o;
        for (String key : this.optionOrder) {
            o = this.getOption(key);
            if (o != null && o.isDisplayOnSettingsPanel()) {
                retList.add(o);
            }
        }
        return retList;
    }

    /**
     * Loads our options from the main options file, creating a new options file
     * if one is not already found. Returns true if the options file was created
     * for the first time.
     *
     * @return
     * @throws FileNotFoundException
     */
    public static boolean loadOptions() throws FileNotFoundException {
        INSTANCE = new Options();
        File f = new File(Options.DEFAULT_FILENAME);
        if (f.exists()) {
            // If the file exists already, attempt to load it.  Don't save
            // anything out, even if we encountered errors while trying to
            // load.
            INSTANCE.loadFromFile(f);
            return false;
        } else {
            // If the file doesn't exist, create it using our defaults.
            INSTANCE.save();
            return true;
        }
    }

    /**
     * Attempts to save our options to the default filename. Returns true if the
     * save was successful, false otherwise.
     *
     * @return True if the save was successful, false otherwise.
     */
    public boolean save() {
        return this.saveToFilename(Options.DEFAULT_FILENAME);
    }

    /**
     * Attempts to save our options to the given filename. Returns true if the
     * save was successful, false otherwise.
     *
     * @param filename The filename to save to
     * @return True if the save was successful, false otherwise
     */
    public boolean saveToFilename(String filename) {
        try {
            Utilities.writeStringToFile(this.toString(), new File(filename));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Attempts to load our options from the given File object. If this returns
     * False, errors occurred while loading. Use getLoadErrors() to retrieve the
     * list of errors, for reporting to the user, in that case.
     *
     * @param f The File object to load from
     * @return True if we loaded without errors, or False if errors were
     * detected
     */
    public boolean loadFromFile(File f) {
        BufferedReader buffered = null;
        this.loadErrors.clear();
        try {
            buffered = new BufferedReader(new FileReader(f));
            String line = buffered.readLine();

            // If our header isn't "key,value", it's an old version of the
            // options file.  Don't bother parsing it, just cancel out.
            if (line != null && line.equals("key,value")) {
                String[] lineComponents;
                Option option;
                while ((line = buffered.readLine()) != null) {
                    lineComponents = line.split(",", 2);
                    if (lineComponents.length == 2) {
                        option = this.getOption(lineComponents[0]);
                        if (option == null) {
                            if (!IGNORE_OPTIONS.contains(lineComponents[0])) {
                                UNKNOWN_OPTIONS.put(lineComponents[0],
                                        lineComponents[1].trim());
                            }
                        } else {
                            try {
                                option.setData(option.stringToData(lineComponents[1].trim()));
                            } catch (Exception ex) {
                                this.loadErrors.add(String.format("Option '%s' could not be parsed: %s",
                                        option.getName(), ex.getMessage()));
                                Logger.getLogger(Options.class.getName()).log(Level.SEVERE,
                                        String.format("Error loading option '%s'", option.getName()),
                                        ex);
                            }
                        }
                    } else {
                        this.loadErrors.add(String.format(
                                "An invalid option line was skipped: <tt>%s</tt>",
                                line));
                    }
                }
            } else {
                if (line == null) {
                    this.loadErrors.add("Empty options file detected, restoring defaults.");
                } else {
                    this.loadErrors.add("Older-style options file detected, not reading any values.");
                }
            }
            buffered.close();
        } catch (FileNotFoundException ex) {
            this.loadErrors.add(String.format("The options file could not be found: %s",
                    ex.getMessage()));
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            this.loadErrors.add(String.format("The options file could not be loaded: %s",
                    ex.getMessage()));
            Logger.getLogger(Options.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            if (buffered != null) {
                try {
                    buffered.close();
                } catch (IOException e) {

                }
            }
        }
        return (this.loadErrors.isEmpty());
    }

    /**
     * Determine if there were load errors during our attempt to load options.
     *
     * @return True if there were errors, False otherwise
     */
    public boolean hasLoadErrors() {
        return (!this.loadErrors.isEmpty());
    }

    /**
     * Returns an ArrayList of errors which occurred while loading options.
     *
     * @return The list of errors, for reporting to the user.
     */
    public ArrayList<String> getLoadErrors() {
        return this.loadErrors;
    }

    /**
     * Restores all our options to their default values, and saves out the
     * options file.
     */
    public void restoreDefaults() {
        for (Option o : OPTION_MAP.values()) {
            o.restoreDefault();
        }
        this.save();
    }

    /**
     * Returns a string representation of our options.
     *
     * @return A string representing the options.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key,value\n");
        for (Option o : OPTION_MAP.values()) {
            sb.append(String.format("%s,%s\n", o.getName(), o.dataToString()));
        }
        for (Map.Entry<String, String> e : UNKNOWN_OPTIONS.entrySet()) {
            sb.append(String.format("%s,%s\n", e.getKey(), e.getValue()));
        }
        return sb.toString();
    }

    /**
     * Convenience function to get a boolean option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public boolean getBooleanOptionData(OptionNames optionName) {
        return this.getBooleanOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a boolean option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setBooleanOptionData(OptionNames optionName, boolean optionValue) {
        this.setBooleanOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get an integer option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public int getIntOptionData(OptionNames optionName) {
        return this.getIntOptionData(optionName.toString());
    }

    /**
     * Convenience function to set an integer option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setIntOptionData(OptionNames optionName, int optionValue) {
        this.setIntOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get a filename option by OptionNames enum entry.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public String getFilenameOptionData(OptionNames optionName) {
        return this.getFilenameOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a filename option by OptionNames enum entry.
     *
     * @param optionName The option whose value to set
     * @param newFile A File object describing the new filename
     */
    public void setFilenameOptionData(OptionNames optionName, File newFile) {
        this.setFilenameOptionData(optionName.toString(), newFile);
    }

    /**
     * Convenience function to get an selection option by name.
     *
     * @param <O>
     * @param optionName The option to retrieve
     * @param c
     * @return The current option data
     */
    public <O extends SelectionOptionData> O getSelectionOptionData(String optionName, Class<O> c) {
        return ((SelectionOption<O>) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set an selection option by name.
     *
     * @param <O> The class of the option value
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public <O extends SelectionOptionData> void setSelectionOptionData(String optionName, O optionValue) {
        ((SelectionOption<O>) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get an selection option by OptionNames enum
     * entry.
     *
     * @param <O>
     * @param optionName The option to retrieve
     * @param c
     * @return The current option data
     */
    public <O extends SelectionOptionData> O getSelectionOptionData(OptionNames optionName, Class<O> c) {
        return this.getSelectionOptionData(optionName.toString(), c);
    }

    /**
     * Convenience function to set an selection option by OptionNames enum
     * entry.
     *
     * @param <O>
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public <O extends SelectionOptionData> void setSelectionOptionData(OptionNames optionName, O optionValue) {
        this.setSelectionOptionData(optionName.toString(), optionValue);
    }

    /**
     * Convenience function to get a boolean option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public boolean getBooleanOptionData(String optionName) {
        return ((BooleanOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a boolean option by name.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setBooleanOptionData(String optionName, boolean optionValue) {
        ((BooleanOption) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get an integer option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public int getIntOptionData(String optionName) {
        return ((IntOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set an integer option by name.
     *
     * @param optionName The option whose value to set
     * @param optionValue The new option data.
     */
    public void setIntOptionData(String optionName, int optionValue) {
        ((IntOption) this.getOption(optionName)).setData(optionValue);
        this.save();
    }

    /**
     * Convenience function to get a filename option by name.
     *
     * @param optionName The option to retrieve
     * @return The current option data
     */
    public String getFilenameOptionData(String optionName) {
        return ((FilenameOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a filename option by name.
     *
     * @param optionName The option whose value to set
     * @param newFile A File object describing the new filename
     */
    public void setFilenameOptionData(String optionName, File newFile) {
        ((FilenameOption) this.getOption(optionName)).setData(newFile.getAbsolutePath());
        this.save();
    }

    /**
     * Convenience function to get a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to retrieve
     * @return The list of strings stored under this option
     */
    public String[] getStringListOptionData(OptionNames optionName) {
        return this.getStringListOptionData(optionName.toString());
    }

    /**
     * Convenience function to set a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to retrieve
     * @return The list of strings stored under this option name
     */
    public String[] getStringListOptionData(String optionName) {
        return ((StringListOption) this.getOption(optionName)).getData();
    }

    /**
     * Convenience function to set a Stringlist option by OptionNames enum
     * entry.
     *
     * @param optionName The option to set
     * @param list A list of strings
     */
    public void setStringListOptionData(OptionNames optionName, String[] list) {
        this.setStringListOptionData(optionName.toString(), list);
    }

    /**
     * Convenience function to set a Stringlist option by name.
     *
     * @param optionName The option to set
     * @param list A list of strings
     */
    public void setStringListOptionData(String optionName, String[] list) {
        ((StringListOption) this.getOption(optionName)).setData(list);
        this.save();
    }

    // What follows are convenience functions to allow the rest of the app
    // to use some more well-defined functions for accessing/setting our
    // options.  First up: user-settable options.  Note that the "set"
    // functions all save out the file as well.
    public Theme getTheme() {
        return this.getSelectionOptionData(OptionNames.theme, Theme.class);
    }

    public void setTheme(Theme theme) {
        this.setSelectionOptionData(OptionNames.theme, theme);
    }

    public int getFontsize() {
        return this.getIntOptionData(OptionNames.fontsize);
    }

    public void setFontSize(int fontsize) {
        this.setIntOptionData(OptionNames.fontsize, fontsize);
    }

    public boolean getHighlightBVCErrors() {
        return this.getBooleanOptionData(OptionNames.highlightBVCErrors);
    }

    public void setHighlightBVCErrors(boolean highlightErrors) {
        this.setBooleanOptionData(OptionNames.highlightBVCErrors, highlightErrors);
    }

    public boolean getTruncateCommands() {
        return this.getBooleanOptionData(OptionNames.truncateCommands2);
    }

    public void setTruncateCommands(boolean truncateCommands) {
        this.setBooleanOptionData(OptionNames.truncateCommands2, truncateCommands);
    }

    public int getTruncateCommandLength() {
        return this.getIntOptionData(OptionNames.truncateCommandLength);
    }

    public void setTruncateCommandLength(int truncateCommandLength) {
        this.setIntOptionData(OptionNames.truncateCommandLength, truncateCommandLength);
    }

    public boolean getStructuralEdits() {
        return this.getBooleanOptionData(OptionNames.structuralEdits);
    }

    public void setStructuralEdits(boolean selected) {
        this.setBooleanOptionData(OptionNames.structuralEdits, selected);
    }

    public boolean isInDeveloperMode() {
        return this.getBooleanOptionData(OptionNames.developerMode);
    }

    public void setContentEdits(boolean selected) {
        this.setBooleanOptionData(OptionNames.developerMode, selected);
    }

    public boolean getLeafSelectionAllowed() {
        return this.getBooleanOptionData(OptionNames.leafSelectionAllowed);
    }

    public void seLeafSelectionAllowed(boolean selected) {
        this.setBooleanOptionData(OptionNames.leafSelectionAllowed, selected);
    }

    // Next up: non-user-settable options.  Doing gets/sets for these even
    // though only getters make sense for most of them.
    public boolean getHasSeenExportWarning() {
        return this.getBooleanOptionData(OptionNames.hasSeenExportWarning);
    }

    public void setHasSeenExportWarning(boolean hasSeenExportWarning) {
        this.setBooleanOptionData(OptionNames.hasSeenExportWarning, hasSeenExportWarning);
    }

    public boolean getShowConfirmPartiaclCategory() {
        return this.getBooleanOptionData(OptionNames.showConfirmPartialCategory);
    }

    public void setShowConfirmPartiaclCategory(boolean b) {
        this.setBooleanOptionData(OptionNames.showConfirmPartialCategory, b);
    }

    public int getSessionsToKeep() {
        return this.getIntOptionData(OptionNames.sessionsToKeep);
    }

    public void setSessionsToKeep(int newValue) {
        this.setIntOptionData(OptionNames.sessionsToKeep, newValue);
    }

    public int getBackupsPerSession() {
        return this.getIntOptionData(OptionNames.backupsPerSession);
    }

    public void setBackupsPerSession(int newValue) {
        this.setIntOptionData(OptionNames.backupsPerSession, newValue);
    }

    public int getSecondsBetweenBackups() {
        return this.getIntOptionData(OptionNames.secondsBetweenBackups);
    }

    public void setSecondsBetweenBackups(int newValue) {
        this.setIntOptionData(OptionNames.secondsBetweenBackups, newValue);
    }

    public boolean getOELeftPaneVisible() {
        return this.getBooleanOptionData(OptionNames.OELeftPaneVisible);
    }

    public void setOELeftPaneVisible(boolean leftVisible) {
        this.setBooleanOptionData(OptionNames.OELeftPaneVisible, leftVisible);
    }

    public int getMainWindowWidth() {
        return this.getIntOptionData(OptionNames.mainWindowWidth);
    }

    public void setMainWindowWidth(int w) {
        this.setIntOptionData(OptionNames.mainWindowWidth, w);
    }

    public int getMainWindowHeight() {
        return this.getIntOptionData(OptionNames.mainWindowHeight);
    }

    public void setMainWindowHeight(int h) {
        this.setIntOptionData(OptionNames.mainWindowHeight, h);
    }

    public boolean getMainWindowMaximized() {
        return this.getBooleanOptionData(OptionNames.mainWindowMaximized);
    }

    public void setMainWindowMaximized(boolean maximized) {
        this.setBooleanOptionData(OptionNames.mainWindowMaximized, maximized);
    }

    public int getEditWindowWidth() {
        return this.getIntOptionData(OptionNames.editWindowWidth);
    }

    public void setEditWindowWidth(int w) {
        this.setIntOptionData(OptionNames.editWindowWidth, w);
    }

    public int getEditWindowHeight() {
        return this.getIntOptionData(OptionNames.editWindowHeight);
    }

    public void setEditWindowHeight(int h) {
        this.setIntOptionData(OptionNames.editWindowHeight, h);
    }

    public int getOEWindowWidth() {
        return this.getIntOptionData(OptionNames.oeWindowWidth);
    }

    public void setOEWindowWidth(int w) {
        this.setIntOptionData(OptionNames.oeWindowWidth, w);
    }

    public int getOEWindowHeight() {
        return this.getIntOptionData(OptionNames.oeWindowHeight);
    }

    public void setOEWindowHeight(int h) {
        this.setIntOptionData(OptionNames.oeWindowHeight, h);
    }

    public boolean getOEWindowMaximized() {
        return this.getBooleanOptionData(OptionNames.oeWindowMaximized);
    }

    public void setOEWindowMaximized(boolean maximized) {
        this.setBooleanOptionData(OptionNames.oeWindowMaximized, maximized);
    }

    public String[] getFileHistory() {
        return this.getStringListOptionData(OptionNames.fileHistory);
    }

    public void setFileHistory(String[] history) {
        this.setStringListOptionData(OptionNames.fileHistory, history);
    }

    public String getLastImport() {
        return this.getFilenameOptionData(OptionNames.lastImport);
    }

    public void setLastImport(File newImport) {
        this.setFilenameOptionData(OptionNames.lastImport, newImport);
    }

    public int getFilenameTruncationLength() {
        return this.getIntOptionData(Options.OptionNames.filenameTruncationLength);
    }

    public void setFilenameTruncationLength(int newLength) {
        this.setIntOptionData(Options.OptionNames.filenameTruncationLength, newLength);
    }

    public boolean getPropagateMUTNotification() {
        return this.getBooleanOptionData(Options.OptionNames.propagateMUTNotification);
    }

    public void setPropagateMUTNotification(boolean propagate) {
        this.setBooleanOptionData(Options.OptionNames.propagateMUTNotification, propagate);
    }

    // Takes boolean 'patch', to note BL2 / TPS bookmarks
    public String[] getOEBookmarks(boolean patch) {
        return this.getStringListOptionData(patch == true ? OptionNames.BL2Bookmarks : OptionNames.TPSBookmarks);
    }

    // Takes boolean 'patch', to note BL2 / TPS bookmarks
    public void setOEBookmarks(String[] bookmark, boolean patch) {
        this.setStringListOptionData(patch == true ? OptionNames.BL2Bookmarks : OptionNames.TPSBookmarks, bookmark);
    }

    public int getPopupStatus() {
        return this.getIntOptionData(OptionNames.popupStatus);
    }

    public void setPopupStatus(int status) {
        this.setIntOptionData(Options.OptionNames.popupStatus, status);
    }

    public boolean getShowHotfixNames() {
        return this.getBooleanOptionData(OptionNames.showHotfixNames);
    }

    public void setShowHotfixNames(boolean status) {
        this.setBooleanOptionData(Options.OptionNames.showHotfixNames, status);
    }

    public boolean getDragAndDropEnabled() {
        return this.getBooleanOptionData(OptionNames.dragAndDroppableCode);
    }

    public void setDragAndDroppableCode(boolean status) {
        this.setBooleanOptionData(OptionNames.dragAndDroppableCode, status);
    }

    public boolean getShowDeletionConfirm() {
        return this.getBooleanOptionData(OptionNames.showDeleteConfirmation);
    }

    public void setShowDeleteConfirmation(boolean status) {
        this.setBooleanOptionData(OptionNames.showDeleteConfirmation, status);
    }
}
