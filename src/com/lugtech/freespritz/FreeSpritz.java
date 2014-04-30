/* Made by Mingwei Samuel */
package com.lugtech.freespritz;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

public class FreeSpritz extends JFrame {
	
	// CONSTANTS //
	private static final long serialVersionUID = -1703702421521172587L;
	private static final float VERSION = 1.0f;
	private static final Color CURSOR_COLOR = new Color(0.5f, 0.0f, 0.0f);
	private static final int BUTTON_SIZE = 120;
	private static final int WORD_MAX = 25;
	private static final Short[] WPM_SPEEDS = { 200, 300, 400, 500, 600, 700, 800, 900, 1000 };
	private static final short WPM_DEFAULT = 400;
	private static final float MILLIS_PER_MINUTE = 60000f;
	private static final float DELAY_MULTIPLIER_EOS = 2.2f; // end of sentence
	private static final float DELAY_MULTIPLIER_COMMA = 1.8f; //
	private static final float DELAY_MULTIPLIER_COLONS = 2.0f;
	
	// SWING UI //
	private final JTextArea textIn;
	private final JScrollPane textInPane;
	
	private final JLabel label;
	
	private final JSeparator line;
	
	private final JLabel cursor;
	private final JLabel spritzLabel;
	
	private final JButton play;
	private final JButton stop;
	private final JComboBox<Short> wpms;
	
	// VARIABLES //
	private final Object lock = new Object();
	private short wpm = WPM_DEFAULT;
	private Reader reader = null;
	
	public FreeSpritz() {
		super("FreeSpritz v" + VERSION);
		
		label = new JLabel("Input text:");
		textIn = new JTextArea();
		textIn.setLineWrap(true);
		textIn.addFocusListener(new FocusListener() {
			
			@Override
			public void focusGained(FocusEvent event) {
				textIn.getHighlighter().removeAllHighlights();
			}
			
			@Override
			public void focusLost(FocusEvent event) {
			}
		});
		textInPane = new JScrollPane(textIn);
		
		line = new JSeparator();
		cursor = new JLabel("\u25bc", SwingConstants.CENTER);
		cursor.setForeground(CURSOR_COLOR);
		cursor.setFont(new Font("Lucida Console", Font.BOLD, 28));
		spritzLabel = new JLabel("", SwingConstants.CENTER);
		spritzLabel.setFont(new Font("Lucida Console", Font.BOLD, 28));
		setWord("", 0);
		
		play = new JButton("\u25b6");
		play.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				if ("\u25b6".equals(play.getText())) { // playing
					play.setText("\u2759\u2759"); // toggle to pause button
					if (reader == null) { // null => stopped
						reader = new Reader();
						new Thread(reader, "reader").start();
					}
					else
						reader.unpause();
				}
				else { // pausing
					play.setText("\u25b6"); // toggle to play button
					if (reader == null)
						return;
					reader.pause();
				}
			}
		});
		stop = new JButton("\u25a0");
		stop.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent event) {
				if (reader == null)
					return;
				textIn.getHighlighter().removeAllHighlights();
				textIn.setCaretPosition(0);
				reader.stop(); // stop reader and ...
				reader = null; // let the GC eat it
				play.setText("\u25b6");
			}
		});
		wpms = new JComboBox<Short>(WPM_SPEEDS);
		wpms.setEditable(true);
		wpms.setSelectedItem(wpm);
		wpms.setTransferHandler(null); //no copy/paste
		wpms.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyTyped(KeyEvent e) {
				char c = e.getKeyChar();
				if ((wpms.getEditor().getItem().toString().length() >= 4 && Character.isDigit(c)) || // if it is full (or)
						!(Character.isDigit(c) || c == KeyEvent.VK_BACK_SPACE || c == KeyEvent.VK_DELETE || c == KeyEvent.VK_ENTER)) { // if it is not a valid key
					getToolkit().beep(); // beep
					e.consume(); // boop
				}
			}
		});
		wpms.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent event) {
				if (event.getStateChange() == ItemEvent.SELECTED) {
					if ((short) event.getItem() < 1) {
						wpms.setSelectedIndex(WPM_DEFAULT);
						wpm = WPM_DEFAULT;
						return;
					}
					wpm = (short) event.getItem();
				}
			}
		});
		
		GroupLayout layout = new GroupLayout(this.getContentPane());
		this.getContentPane().setLayout(layout); // weird circular programming :S
		
		SequentialGroup hGroup = layout.createSequentialGroup();
		ParallelGroup hCenter = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		SequentialGroup hButtonGroup = layout.createSequentialGroup();
		
		hButtonGroup.addComponent(play, 0, BUTTON_SIZE, BUTTON_SIZE);
		hButtonGroup.addComponent(stop, 0, BUTTON_SIZE, BUTTON_SIZE);
		hButtonGroup.addGap(0, BUTTON_SIZE, Short.MAX_VALUE);
		hButtonGroup.addComponent(wpms, 0, 60, 60);
		
		hCenter.addComponent(textInPane, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE);
		hCenter.addComponent(label, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE);
		hCenter.addComponent(line, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE);
		hCenter.addComponent(cursor, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE);
		hCenter.addComponent(spritzLabel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 480, Short.MAX_VALUE);
		hCenter.addGroup(GroupLayout.Alignment.CENTER, hButtonGroup);
		
		hGroup.addContainerGap();
		hGroup.addGroup(hCenter);
		hGroup.addContainerGap();
		
		layout.setHorizontalGroup(hGroup);
		
		SequentialGroup vGroup = layout.createSequentialGroup();
		SequentialGroup vTop = layout.createSequentialGroup();
		ParallelGroup vBottomButtons = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
		
		vTop.addComponent(label);
		vTop.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
		vTop.addComponent(textInPane, GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE);
		vTop.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
		vTop.addComponent(line);
		vTop.addGap(0, 60, Short.MAX_VALUE);
		vTop.addComponent(cursor);
		vTop.addComponent(spritzLabel);
		vTop.addGap(0, 60, Short.MAX_VALUE);
		
		vBottomButtons.addComponent(play);
		vBottomButtons.addComponent(stop);
		vBottomButtons.addComponent(wpms);
		
		vGroup.addContainerGap();
		vGroup.addGroup(vTop);
		vGroup.addGroup(vBottomButtons);
		vGroup.addContainerGap();
		layout.setVerticalGroup(vGroup);
		
		this.pack();
		
		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
	}
	
	private void setWord(String word, int highlight) {
		if (0 > highlight || highlight >= word.length()) {
			if (word.isEmpty()) {
				spritzLabel.setText(" ");
				return;
			}
			throw new IllegalArgumentException("hightlight index may not be outside of the word");
		}
		
		StringBuilder builder = new StringBuilder();
		int offset = highlight * 2 - word.length() + 1;
		if (word.length() + Math.abs(offset) > WORD_MAX)
			throw new IllegalArgumentException("word is too long");
		if (offset < 0) {
			char[] chars = new char[-offset];
			Arrays.fill(chars, '\u00a0');
			builder.append(chars);
		}
		builder.append(word.substring(0, highlight));
		builder.append("<font color=\"" + String.format("#%02x%02x%02x", CURSOR_COLOR.getRed(), CURSOR_COLOR.getGreen(), CURSOR_COLOR.getBlue()) + "\">");
		builder.append(word.charAt(highlight));
		builder.append("</font>");
		builder.append(word.substring(highlight + 1));
		if (offset > 0) {
			char[] chars = new char[offset];
			Arrays.fill(chars, '\u00a0');
			builder.append(chars);
		}
		builder.insert(0, "<html>");
		builder.append("</html>");
		spritzLabel.setText(builder.toString());
	}
	
	public static void main(String[] args) {
		
		try {
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				new FreeSpritz().setVisible(true);
			}
		});
	}
	
	private class Reader implements Runnable {
		
		private final SortedMap<Integer, String> words = new TreeMap<Integer, String>();
		private final Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(1.0f, 1.0f, 0.5f));
		private volatile boolean stop = false;
		private volatile boolean pause = false;
		
		@Override
		public void run() {
			generateWords();
			
			Iterator<Integer> iter = words.keySet().iterator();
			while (iter.hasNext() && !stop) {
				try {
					while (pause) { // pause if needed
						synchronized (lock) {
							lock.wait();
						}
					}
					// main stuffs
					int index = iter.next();
					
					long delay = (long) (MILLIS_PER_MINUTE / wpm);
					String word = words.get(index);
					if (word.contains(".")) { // replace periods with bullets (more visible) and add delay
						delay *= DELAY_MULTIPLIER_EOS;
						word = word.replace('.', '\u2022');
					}
					else if (word.contains("?") || word.contains("!"))
						delay *= DELAY_MULTIPLIER_EOS;
					else if (word.contains(":") || word.contains(";"))
						delay *= DELAY_MULTIPLIER_COLONS;
					else if (word.contains(","))
						delay *= DELAY_MULTIPLIER_COMMA;
					display(word);
					highlight(word, index);
					Thread.sleep(delay);
				}
				catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}
		
		private void generateWords() {
			final String in = textIn.getText();
			StringBuilder word = new StringBuilder();
			if (textIn.getCaretPosition() == textIn.getText().length())
				textIn.setCaretPosition(0); //if we are at the end, reset to the top
			for (int i = textIn.getCaretPosition(); i < in.length(); i++) {
				if (Character.isWhitespace(in.charAt(i))) { // end of word
					words.put(i, word.toString()); // write the index (note: last char) and word
					word.setLength(0); // clear word
					continue;
				}
				word.append(in.charAt(i));
				// put word if it ends in a hyphen
				// hyphen u2012 to u2015
				if (in.charAt(i) == '-' || 0x2012 <= Character.getNumericValue(in.charAt(i)) && Character.getNumericValue(in.charAt(i)) <= 0x2015 || in.charAt(i) == '\u2053') { // swung
																																													// dash
					words.put(i, word.toString());
					word.setLength(0);
				}
			}
			words.put(in.length(), word.toString());
		}
		
		private void highlight(String word, int index) {
			try {
				textIn.getHighlighter().addHighlight(index - word.length(), index, painter);
				textIn.setCaretPosition(index);
			}
			catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
		
		private void display(String word) { // synchronized stops setWord() from being called concurrently
			int cursor;
			switch (word.length()) {
			case 0:
			case 1:
				cursor = 0;
				break;
			case 2:
			case 3:
			case 4:
			case 5:
				cursor = 1;
				break;
			case 6:
			case 7:
			case 8:
			case 9:
				cursor = 2;
				break;
			case 10:
			case 11:
			case 12:
			case 13:
				cursor = 3;
				break;
			default:
				cursor = 4;
			}
			if (word.length() + Math.abs(cursor * 2 - word.length() + 1) > WORD_MAX) { // if the word is too long, split in half (dumb splitting, should use hyphenation)
				display(word.substring(0, word.length() / 2) + "-");
				try { Thread.sleep((long) (MILLIS_PER_MINUTE / wpm)); }
				catch (InterruptedException ie) { ie.printStackTrace(); };
				display(word.substring(word.length() / 2));
				return;
			}
			setWord(word, cursor);
		}
		
		public void stop() {
			stop = true;
		}
		
		public void pause() {
			pause = true;
		}
		
		public void unpause() {
			pause = false;
			synchronized (lock) {
				lock.notifyAll();
			}
		}
	}
}
