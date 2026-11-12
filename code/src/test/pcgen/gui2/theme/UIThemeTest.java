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

import org.junit.jupiter.api.Test;

class UIThemeTest
{
	@Test
	void storedKeysRoundTrip()
	{
		for (UITheme theme : UITheme.values())
		{
			assertEquals(theme, UITheme.fromKey(theme.getKey()));
		}
	}

	@Test
	void parsingIgnoresCaseAndWhitespace()
	{
		assertEquals(UITheme.DARK, UITheme.fromKey(" Dark "));
	}

	@Test
	void missingOrUnknownFallsBackToDefault()
	{
		assertEquals(UITheme.DEFAULT, UITheme.fromKey(null));
		assertEquals(UITheme.DEFAULT, UITheme.fromKey("nimbus"));
	}

	@Test
	void darkStylesheetIsBundled()
	{
		assertEquals(true, ThemeManager.class.getResource("pcgen-dark.css") != null);
	}
}
