package org.mcsg.double0negative.tabapi;

import org.bukkit.plugin.Plugin;

class TabHolder {
	int maxv = 0;
	int maxh = 0;
	int[][] tabPings;
	String[][] tabs;
	Plugin p;

	@SuppressWarnings("deprecation")
	public TabHolder(Plugin p) {
		this.p = p;
		this.tabs = new String[TabAPI.getHorizSize()][TabAPI.getVertSize()];
		this.tabPings = new int[TabAPI.getHorizSize()][TabAPI.getVertSize()];
	}

	public TabHolder getCopy() {
		TabHolder newCopy = new TabHolder(this.p);
		newCopy.tabs = copyStringArray(this.tabs);
		newCopy.tabPings = copyIntArray(this.tabPings);
		return newCopy;
	}

	private static String[][] copyStringArray(String[][] tab) {
		@SuppressWarnings("deprecation")
		int horzTabSize = TabAPI.getHorizSize();
		@SuppressWarnings("deprecation")
		int vertTabSize = TabAPI.getVertSize();
		String[][] temp = new String[horzTabSize][vertTabSize];
		for (int b = 0; b < vertTabSize; b++) {
			for (int a = 0; a < horzTabSize; a++) {
				temp[a][b] = tab[a][b];
			}
		}
		return temp;
	}

	private static int[][] copyIntArray(int[][] tab) {
		@SuppressWarnings("deprecation")
		int horzTabSize = TabAPI.getHorizSize();
		@SuppressWarnings("deprecation")
		int vertTabSize = TabAPI.getVertSize();
		int[][] temp = new int[horzTabSize][vertTabSize];
		for (int b = 0; b < vertTabSize; b++) {
			for (int a = 0; a < horzTabSize; a++) {
				temp[a][b] = tab[a][b];
			}
		}
		return temp;
	}
}