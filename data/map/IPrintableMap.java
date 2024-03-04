package data.map;

import java.awt.Color;

public interface IPrintableMap {
	public Color getColorAt(int x, int y);
	public int getXSize();
	public int getYSize();
}
