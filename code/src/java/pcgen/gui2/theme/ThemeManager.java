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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import pcgen.gui2.UIPropertyContext;
import pcgen.util.Logging;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;

/**
 * Applies the user's chosen {@link UITheme} to both UI toolkits PCGen uses: the Swing look and
 * feel, and a stylesheet on every JavaFX scene (including scenes embedded in Swing through JFXPanel).
 */
public final class ThemeManager
{
	private static final String THEME_KEY = "uiTheme"; //$NON-NLS-1$
	private static final String DARK_STYLESHEET = Objects.requireNonNull(
		ThemeManager.class.getResource("pcgen-dark.css"), "pcgen-dark.css missing").toExternalForm();

	/** Theme currently applied. Written on either UI thread, read on both. */
	private static volatile UITheme currentTheme = UITheme.DEFAULT;
	private static boolean javaFxListenerInstalled;

	private ThemeManager()
	{
	}

	/**
	 * @return the theme saved in the user's settings
	 */
	public static UITheme getConfiguredTheme()
	{
		return UITheme.fromKey(UIPropertyContext.getInstance().getProperty(THEME_KEY));
	}

	/**
	 * @return the theme currently applied to the UI
	 */
	public static UITheme getCurrentTheme()
	{
		return currentTheme;
	}

	/**
	 * Applies the saved theme during startup. Must be called after settings are loaded and the JavaFX
	 * toolkit is running, and before any PCGen window is created. Must not be called on a UI thread.
	 */
	public static void installAtStartup()
	{
		UITheme theme = getConfiguredTheme();
		currentTheme = theme;
		try
		{
			SwingUtilities.invokeAndWait(() -> installSwingLookAndFeel(theme));
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		catch (InvocationTargetException e)
		{
			Logging.errorPrint("Unable to apply UI theme " + theme, e.getCause());
		}
		Platform.runLater(ThemeManager::installJavaFxListener);
	}

	/**
	 * Saves the theme to the user's settings and switches the running UI to it. Safe to call from any
	 * thread.
	 *
	 * @param theme the theme to switch to
	 */
	public static void changeTheme(UITheme theme)
	{
		Objects.requireNonNull(theme);
		UIPropertyContext.getInstance().setProperty(THEME_KEY, theme.getKey());
		if (theme == currentTheme)
		{
			return;
		}
		currentTheme = theme;
		SwingUtilities.invokeLater(() -> {
			installSwingLookAndFeel(theme);
			FlatLaf.updateUI();
		});
		Platform.runLater(() -> Window.getWindows().forEach(ThemeManager::styleWindow));
	}

	private static void installSwingLookAndFeel(UITheme theme)
	{
		try
		{
			switch (theme)
			{
				case LIGHT -> UIManager.setLookAndFeel(configureFlatLaf(new FlatLightLaf()));
				case DARK -> UIManager.setLookAndFeel(configureFlatLaf(new FlatDarkLaf()));
				case SYSTEM -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		}
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException
			| UnsupportedLookAndFeelException e)
		{
			Logging.errorPrint("Unable to set look and feel for theme " + theme, e);
		}
	}

	private static LookAndFeel configureFlatLaf(FlatLaf laf)
	{
		// Rounded, roomier controls are the main visual difference from the legacy look.
		UIManager.put("Component.arc", 6); //$NON-NLS-1$
		UIManager.put("Button.arc", 8); //$NON-NLS-1$
		UIManager.put("TextComponent.arc", 6); //$NON-NLS-1$
		UIManager.put("ScrollBar.showButtons", false); //$NON-NLS-1$
		UIManager.put("ScrollBar.thumbArc", 999); //$NON-NLS-1$
		UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2)); //$NON-NLS-1$
		UIManager.put("TabbedPane.showTabSeparators", true); //$NON-NLS-1$
		UIManager.put("Table.showHorizontalLines", true); //$NON-NLS-1$
		return laf;
	}

	/**
	 * Watches JavaFX windows so every scene, now or later, gets the current theme's stylesheet.
	 * Runs on the JavaFX thread.
	 */
	private static void installJavaFxListener()
	{
		if (javaFxListenerInstalled)
		{
			return;
		}
		javaFxListenerInstalled = true;
		Window.getWindows().addListener((ListChangeListener<Window>) change -> {
			while (change.next())
			{
				change.getAddedSubList().forEach(ThemeManager::styleWindow);
			}
		});
		Window.getWindows().forEach(ThemeManager::styleWindow);
	}

	private static void styleWindow(Window window)
	{
		styleScene(window.getScene());
		// A window's scene can be set or replaced after it is shown.
		window.sceneProperty().addListener((obs, oldScene, newScene) -> styleScene(newScene));
	}

	private static void styleScene(Scene scene)
	{
		if (scene == null)
		{
			return;
		}
		if (currentTheme == UITheme.DARK)
		{
			if (!scene.getStylesheets().contains(DARK_STYLESHEET))
			{
				scene.getStylesheets().add(DARK_STYLESHEET);
			}
		}
		else
		{
			scene.getStylesheets().remove(DARK_STYLESHEET);
		}
	}

}
