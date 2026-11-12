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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ThemeColorsTest
{
	@Test
	void lightThemesKeepStoredColors()
	{
		assertEquals(Color.BLACK, ThemeColors.forTheme(Color.BLACK, UITheme.LIGHT));
		assertEquals(Color.BLUE, ThemeColors.forTheme(Color.BLUE, UITheme.SYSTEM));
	}

	@Test
	void darkThemeLightensUnreadableColors()
	{
		for (Color c : new Color[]{Color.BLACK, Color.BLUE, Color.RED, Color.MAGENTA, Color.color(0.5, 0, 0)})
		{
			Color shown = ThemeColors.forTheme(c, UITheme.DARK);
			assertTrue(ThemeColors.luminance(shown) >= ThemeColors.MIN_DARK_THEME_LUMINANCE, c + " -> " + shown);
		}
	}

	@Test
	void darkThemeKeepsAlreadyReadableColors()
	{
		Color yellow = Color.valueOf("0xB2B200");
		assertEquals(yellow, ThemeColors.forTheme(yellow, UITheme.DARK));
	}
}
