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

import javafx.scene.paint.Color;

/**
 * Keeps user-configurable text colors (qualified, not qualified, source status and so on)
 * readable on the current theme's background. The stored preference is never changed; only
 * the color used for drawing is adjusted.
 */
public final class ThemeColors
{
	/** Minimum relative luminance for text drawn on the dark theme's background. */
	static final double MIN_DARK_THEME_LUMINANCE = 0.40;

	private ThemeColors()
	{
	}

	/**
	 * @param stored the color saved in the user's preferences
	 * @return the color to draw text with under the current theme
	 */
	public static Color forDisplay(Color stored)
	{
		return forTheme(stored, ThemeManager.getCurrentTheme());
	}

	/**
	 * @param stored the color saved in the user's preferences
	 * @param theme the theme being drawn
	 * @return {@code stored}, lightened just enough to read on a dark background when
	 * {@code theme} is dark
	 */
	static Color forTheme(Color stored, UITheme theme)
	{
		if (stored == null || theme != UITheme.DARK || luminance(stored) >= MIN_DARK_THEME_LUMINANCE)
		{
			return stored;
		}
		// Blend toward white in small steps so hue is kept as far as possible.
		Color adjusted = stored;
		for (int step = 1; step <= 20 && luminance(adjusted) < MIN_DARK_THEME_LUMINANCE; step++)
		{
			adjusted = stored.interpolate(Color.WHITE, step / 20.0);
		}
		return adjusted;
	}

	/**
	 * @return WCAG relative luminance of {@code color}, from 0 (black) to 1 (white)
	 */
	static double luminance(Color color)
	{
		return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen())
			+ 0.0722 * linear(color.getBlue());
	}

	private static double linear(double channel)
	{
		return (channel <= 0.03928) ? (channel / 12.92) : Math.pow((channel + 0.055) / 1.055, 2.4);
	}
}
