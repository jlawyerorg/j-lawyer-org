/**
 * Copyright 2013 Theodor Costache
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License. 
 */
package de.costache.calendar.util;

import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * 
 * @author theodorcostache
 * 
 */
public class GraphicsUtil {
	/**
	 * 
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 * @param width
	 */
	public static void drawString(Graphics g, String s, int x, int y, int width, int height) {
		// FontMetrics gives us information about the width,
		// height, etc. of the current Graphics object's Font.
		FontMetrics fm = g.getFontMetrics();

		int lineHeight = fm.getHeight();
		int textHeight = 0;
		int curX = x;
		int curY = y;

		String[] words = s.split(" ");

		for (String word : words) {
			// Find out thw width of the word.
			int wordWidth = fm.stringWidth(word + " ");

			// If text exceeds the width, then move to next line.
			if (curX + wordWidth >= x + width) {
				curY += lineHeight;
				textHeight += lineHeight;
				curX = x;
			}
			int charIdx = word.length();
			boolean textTrimmed = false;
			while (charIdx > 0 && wordWidth >= width) {
				charIdx -= 1;
				word = word.substring(0, charIdx);
				wordWidth = fm.stringWidth(word + " ");
				textTrimmed = true;
			}
			word = word.length() > 3 && textTrimmed ? word.substring(0, word.length() - 3) + "..."
					: word.length() > 2 ? word : "";

			if (textHeight + lineHeight > height) {
				g.drawString("...", curX, curY);
				break;
			}
			g.drawString(word, curX, curY);

			// Move over to the right for next word.
			curX += wordWidth;
		}
	}

	/**
	 * 
	 * @param g
	 * @param s
	 * @param x
	 * @param y
	 * @param width
	 */
	public static void drawTrimmedString(Graphics g, String s, int x, int y, int width) {
		// FontMetrics gives us information about the width,
		// height, etc. of the current Graphics object's Font.

        if(s == null)
            s = "" + s;

		FontMetrics fm = g.getFontMetrics();

		int wordWidth = fm.stringWidth(s + " ");

		int charIdx = s.length();
		boolean textTrimmed = false;
		while (charIdx > 0 && wordWidth >= width) {
			charIdx -= 1;
			s = s.substring(0, charIdx);
			wordWidth = fm.stringWidth(s + " ");
			textTrimmed = true;
		}
		s = s.length() > 3 && textTrimmed ? s.substring(0, s.length() - 3) + "..." : s.length() > 2 ? s : "";

		g.drawString(s, x, y);

	}
}
