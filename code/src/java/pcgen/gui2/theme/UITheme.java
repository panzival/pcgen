/*
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package pcgen.gui2.theme;

import java.util.Locale;

import pcgen.system.LanguageBundle;

/**
 * The visual themes PCGen can use for its Swing and JavaFX user interface.
 */
public enum UITheme
{
	/** Modern flat light theme (FlatLaf). */
	LIGHT("in_Prefs_themeLight"),
	/** Modern flat dark theme (FlatLaf plus a dark JavaFX stylesheet). */
	DARK("in_Prefs_themeDark"),
	/** The operating system's native look, as PCGen used before themes existed. */
	SYSTEM("in_Prefs_themeSystem");

	/** Theme used when no preference has been saved. */
	public static final UITheme DEFAULT = LIGHT;

	private final String labelKey;

	UITheme(String labelKey)
	{
		this.labelKey = labelKey;
	}

	/**
	 * @return the localized name shown to the user
	 */
	public String getDisplayName()
	{
		return LanguageBundle.getString(labelKey);
	}

	/**
	 * @return the value stored in the settings file
	 */
	public String getKey()
	{
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * Parses a stored settings value, falling back to {@link #DEFAULT} for missing or unknown values.
	 *
	 * @param key the stored value, may be null
	 * @return the matching theme
	 */
	public static UITheme fromKey(String key)
	{
		if (key != null)
		{
			for (UITheme theme : values())
			{
				if (theme.getKey().equalsIgnoreCase(key.trim()))
				{
					return theme;
				}
			}
		}
		return DEFAULT;
	}
}
