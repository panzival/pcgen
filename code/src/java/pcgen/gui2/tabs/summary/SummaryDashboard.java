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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 */
package pcgen.gui2.tabs.summary;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.Border;

import pcgen.facade.core.CharacterFacade;

/**
 * At-the-table overview shown at the top of the Summary tab: the character's
 * identity and XP, a row of combat numbers with a session hit point tracker,
 * clickable ability and save rolls, and a roll log.
 */
public final class SummaryDashboard extends JPanel
{
	private static final int MAX_LOG = 8;
	private static final Pattern FIRST_NUMBER = Pattern.compile("[+-]?\\d+");
	private static final Random DICE = new SecureRandom();

	private final JLabel nameLabel = new JLabel();
	private final JLabel subtitleLabel = new JLabel();
	private final JProgressBar xpBar = new JProgressBar();
	private final JLabel xpLabel = new JLabel();
	private final JLabel hpValue = new JLabel();
	private final JProgressBar hpBar = new JProgressBar();
	private final JPanel statCards = new JPanel(new GridLayout(1, 0, 8, 0));
	private final JPanel abilityChips = new JPanel(new GridLayout(1, 0, 8, 0));
	private final JPanel logList = new JPanel();
	private final Timer refreshTimer;

	private State state;

	/**
	 * Per-character session state: damage taken and the roll log. Not saved
	 * with the character.
	 */
	public static final class State
	{
		private final CharacterFacade character;
		private int damage;
		private int lastSerial = Integer.MIN_VALUE;
		private final Deque<String[]> log = new ArrayDeque<>();

		public State(CharacterFacade character)
		{
			this.character = character;
		}
	}

	public SummaryDashboard()
	{
		super(new BorderLayout(16, 0));
		setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

		JPanel main = new JPanel();
		main.setOpaque(false);
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.add(buildHeader());
		main.add(Box.createVerticalStrut(10));
		main.add(buildCardsRow());
		main.add(Box.createVerticalStrut(8));
		abilityChips.setOpaque(false);
		abilityChips.setAlignmentX(LEFT_ALIGNMENT);
		main.add(abilityChips);
		add(main, BorderLayout.CENTER);
		add(buildLogPanel(), BorderLayout.EAST);

		refreshTimer = new Timer(700, e -> refreshIfChanged());
	}

	public void install(State newState)
	{
		this.state = newState;
		state.lastSerial = Integer.MIN_VALUE;
		refreshIfChanged();
		renderLog();
		refreshTimer.start();
	}

	public void uninstall()
	{
		refreshTimer.stop();
		state = null;
	}

	private JComponent buildHeader()
	{
		JPanel header = new JPanel(new BorderLayout(24, 0));
		header.setOpaque(false);
		header.setAlignmentX(LEFT_ALIGNMENT);
		JPanel ident = new JPanel(new GridLayout(2, 1));
		ident.setOpaque(false);
		nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 22f));
		subtitleLabel.setForeground(muted());
		ident.add(nameLabel);
		ident.add(subtitleLabel);
		header.add(ident, BorderLayout.WEST);

		JPanel xp = new JPanel(new BorderLayout(0, 4));
		xp.setOpaque(false);
		xpLabel.setForeground(muted());
		xpLabel.setFont(xpLabel.getFont().deriveFont(11f));
		xpBar.setPreferredSize(new Dimension(320, 6));
		xp.add(xpLabel, BorderLayout.NORTH);
		xp.add(xpBar, BorderLayout.CENTER);
		JPanel xpWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8));
		xpWrap.setOpaque(false);
		xpWrap.add(xp);
		header.add(xpWrap, BorderLayout.CENTER);
		header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
		return header;
	}

	private JComponent buildCardsRow()
	{
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setOpaque(false);
		row.setAlignmentX(LEFT_ALIGNMENT);

		JPanel hpCard = card();
		hpCard.setLayout(new BorderLayout(0, 4));
		hpCard.add(caption("HIT POINTS"), BorderLayout.NORTH);
		hpValue.setFont(numberFont(28f));
		JPanel hpButtons = new JPanel(new GridLayout(2, 2, 4, 4));
		hpButtons.setOpaque(false);
		hpButtons.add(hpButton("−5", -5, "Take 5 damage"));
		hpButtons.add(hpButton("+5", 5, "Heal 5"));
		hpButtons.add(hpButton("−1", -1, "Take 1 damage"));
		hpButtons.add(hpButton("+1", 1, "Heal 1"));
		JPanel hpMid = new JPanel(new BorderLayout(10, 0));
		hpMid.setOpaque(false);
		hpMid.add(hpValue, BorderLayout.CENTER);
		hpMid.add(hpButtons, BorderLayout.EAST);
		hpCard.add(hpMid, BorderLayout.CENTER);
		hpBar.setPreferredSize(new Dimension(10, 5));
		hpCard.add(hpBar, BorderLayout.SOUTH);
		hpCard.setPreferredSize(new Dimension(250, 96));
		row.add(hpCard, BorderLayout.WEST);

		statCards.setOpaque(false);
		row.add(statCards, BorderLayout.CENTER);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
		return row;
	}

	private JComponent buildLogPanel()
	{
		JPanel panel = card();
		panel.setLayout(new BorderLayout(0, 6));
		panel.add(caption("ROLL LOG · click a number to roll"), BorderLayout.NORTH);
		logList.setOpaque(false);
		logList.setLayout(new BoxLayout(logList, BoxLayout.Y_AXIS));
		panel.add(logList, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(290, 10));
		return panel;
	}

	private void refreshIfChanged()
	{
		if (state == null || !isShowing() && state.lastSerial != Integer.MIN_VALUE)
		{
			return;
		}
		int serial = state.character.getChangeSerial();
		if (serial == state.lastSerial)
		{
			return;
		}
		state.lastSerial = serial;
		CharacterFacade pc = state.character;

		nameLabel.setText(pc.evaluateToken("NAME"));
		subtitleLabel.setText(join(" · ", pc.evaluateToken("RACE"), pc.evaluateToken("CLASSLIST"),
			pc.evaluateToken("ALIGNMENT"), pc.evaluateToken("PLAYERNAME")));

		int xp = parse(pc.evaluateToken("EXP.CURRENT"));
		int next = parse(pc.evaluateToken("EXP.NEXT"));
		xpBar.setMaximum(Math.max(1, next));
		xpBar.setValue(Math.min(xp, Math.max(1, next)));
		xpLabel.setText(String.format("XP  %,d / %,d  ·  %,d to next level", xp, next, Math.max(0, next - xp)));

		renderHp();

		statCards.removeAll();
		statCards.add(statCard("AC", pc.evaluateToken("AC.Total"),
			"T" + pc.evaluateToken("AC.Touch") + " FF" + pc.evaluateToken("AC.Flatfooted"), null));
		statCards.add(statCard("INIT", pc.evaluateToken("INITIATIVEMOD"), "", "Initiative"));
		statCards.add(statCard("BAB", pc.evaluateToken("ATTACK.MELEE.BASE"),
			"M" + firstAttack(pc.evaluateToken("ATTACK.MELEE.TOTAL")) + " R"
				+ firstAttack(pc.evaluateToken("ATTACK.RANGED.TOTAL")),
			"Base attack"));
		for (int i = 0; i < 3; i++)
		{
			String name = pc.evaluateToken("CHECK." + i + ".NAME");
			if (!name.isEmpty())
			{
				String abbr = name.length() > 4 ? name.substring(0, 4) : name;
				statCards.add(statCard(abbr.toUpperCase(), pc.evaluateToken("CHECK." + i + ".TOTAL"), "",
					name + " save"));
			}
		}
		statCards.add(statCard("SPEED", parse(pc.evaluateToken("MOVE.0.RATE")) + "ft", pc.evaluateToken("MOVE.0.NAME"), null));

		abilityChips.removeAll();
		for (int i = 0; i < 12; i++)
		{
			String abbr = pc.evaluateToken("STAT." + i + ".NAME");
			if (abbr.isEmpty())
			{
				break;
			}
			abilityChips.add(abilityChip(abbr, pc.evaluateToken("STAT." + i),
				pc.evaluateToken("STAT." + i + ".MOD")));
		}
		revalidate();
		repaint();
	}

	private void renderHp()
	{
		int max = parse(state.character.evaluateToken("HP"));
		int current = max - state.damage;
		hpValue.setText(current + " / " + max);
		hpBar.setMaximum(Math.max(1, max));
		hpBar.setValue(Math.max(0, current));
		float pct = max <= 0 ? 1f : (float) current / max;
		Color color = pct > 0.5f ? good() : pct > 0.25f ? warn() : bad();
		hpValue.setForeground(color);
		hpBar.setForeground(color);
	}

	private JButton hpButton(String text, int delta, String tip)
	{
		JButton b = new JButton(text);
		b.setToolTipText(tip + " (this session only)");
		b.setMargin(new Insets(1, 6, 1, 6));
		b.setFocusable(false);
		b.addActionListener(e -> {
			if (state != null)
			{
				int max = parse(state.character.evaluateToken("HP"));
				state.damage = Math.max(0, Math.min(max + 10, state.damage - delta));
				renderHp();
			}
		});
		return b;
	}

	private JComponent statCard(String label, String value, String sub, String rollName)
	{
		JPanel c = card();
		c.setLayout(new GridLayout(3, 1));
		c.add(caption(label));
		JLabel v = new JLabel(value);
		v.setFont(numberFont(value.length() > 3 ? 15f : 22f));
		c.add(v);
		JLabel s = new JLabel(sub);
		s.setForeground(muted());
		s.setFont(s.getFont().deriveFont(10f));
		c.add(s);
		c.setToolTipText(label + " " + value + (sub.isEmpty() ? "" : " (" + sub + ")"));
		if (rollName != null)
		{
			makeRollable(c, rollName, value);
		}
		return c;
	}

	private JComponent abilityChip(String abbr, String score, String mod)
	{
		JPanel c = card();
		c.setLayout(new BorderLayout(8, 0));
		JLabel a = caption(abbr);
		c.add(a, BorderLayout.WEST);
		JLabel m = new JLabel(mod, SwingConstants.CENTER);
		m.setFont(numberFont(18f));
		c.add(m, BorderLayout.CENTER);
		JLabel s = new JLabel(score);
		s.setForeground(muted());
		c.add(s, BorderLayout.EAST);
		makeRollable(c, abbr + " check", mod);
		return c;
	}

	private void makeRollable(JComponent c, String rollName, String bonusText)
	{
		c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		c.setToolTipText("Roll " + rollName);
		c.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				roll(rollName, parse(bonusText));
			}
		});
	}

	private void roll(String what, int bonus)
	{
		if (state == null)
		{
			return;
		}
		int die = 1 + DICE.nextInt(20);
		String detail = "d20 " + die + (bonus >= 0 ? " + " : " − ") + Math.abs(bonus)
			+ (die == 20 ? "  · natural 20" : die == 1 ? "  · natural 1" : "");
		state.log.addFirst(new String[]{String.valueOf(die + bonus), what, detail, String.valueOf(die)});
		while (state.log.size() > MAX_LOG)
		{
			state.log.removeLast();
		}
		renderLog();
	}

	private void renderLog()
	{
		logList.removeAll();
		if (state == null || state.log.isEmpty())
		{
			JLabel empty = new JLabel("No rolls yet");
			empty.setForeground(muted());
			logList.add(empty);
		}
		else
		{
			for (String[] entry : state.log)
			{
				JPanel row = new JPanel(new BorderLayout(10, 0));
				row.setOpaque(false);
				row.setAlignmentX(LEFT_ALIGNMENT);
				JLabel total = new JLabel(entry[0], SwingConstants.RIGHT);
				total.setFont(numberFont(18f));
				total.setPreferredSize(new Dimension(36, 22));
				if ("20".equals(entry[3]))
				{
					total.setForeground(warn());
				}
				else if ("1".equals(entry[3]))
				{
					total.setForeground(bad());
				}
				row.add(total, BorderLayout.WEST);
				JLabel text = new JLabel("<html>" + entry[1] + "<br><font size=-2>" + entry[2] + "</font></html>");
				row.add(text, BorderLayout.CENTER);
				row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
				logList.add(row);
				logList.add(Box.createVerticalStrut(4));
			}
		}
		logList.revalidate();
		logList.repaint();
	}

	private static JPanel card()
	{
		JPanel p = new JPanel();
		Color line = UIManager.getColor("Component.borderColor");
		if (line == null)
		{
			line = Color.GRAY;
		}
		Border border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(line),
			BorderFactory.createEmptyBorder(8, 10, 8, 8));
		p.setBorder(border);
		Color bg = UIManager.getColor("TextField.background");
		if (bg != null)
		{
			p.setBackground(bg);
		}
		return p;
	}

	private static JLabel caption(String text)
	{
		JLabel l = new JLabel(text);
		l.setForeground(muted());
		l.setFont(l.getFont().deriveFont(Font.BOLD, 10.5f));
		return l;
	}

	private static Font numberFont(float size)
	{
		return new Font(Font.MONOSPACED, Font.BOLD, Math.round(size)).deriveFont(size);
	}

	private static Color muted()
	{
		Color c = UIManager.getColor("Label.disabledForeground");
		return c == null ? Color.GRAY : c;
	}

	private static boolean isDark()
	{
		Color bg = UIManager.getColor("Panel.background");
		return bg != null && (bg.getRed() + bg.getGreen() + bg.getBlue()) / 3 < 128;
	}

	private static Color good()
	{
		return isDark() ? new Color(0x9be36b) : new Color(0x2e7d32);
	}

	private static Color warn()
	{
		return isDark() ? new Color(0xffd66b) : new Color(0xb26a00);
	}

	private static Color bad()
	{
		return isDark() ? new Color(0xff8a94) : new Color(0xc62828);
	}

	static int parse(String text)
	{
		if (text == null)
		{
			return 0;
		}
		Matcher m = FIRST_NUMBER.matcher(text.replace(",", ""));
		return m.find() ? Integer.parseInt(m.group()) : 0;
	}

	private static String firstAttack(String attacks)
	{
		int slash = attacks.indexOf('/');
		return slash < 0 ? attacks : attacks.substring(0, slash);
	}

	private static String join(String sep, String... parts)
	{
		StringBuilder sb = new StringBuilder();
		for (String p : parts)
		{
			if (p != null && !p.isBlank())
			{
				if (sb.length() > 0)
				{
					sb.append(sep);
				}
				sb.append(p);
			}
		}
		return sb.toString();
	}
}
