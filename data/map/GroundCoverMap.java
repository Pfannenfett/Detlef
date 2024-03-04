package data.map;

import java.awt.Color;
import java.util.ArrayList;

public class GroundCoverMap implements IPrintableMap {
	private byte[][] values;
	
	public static final byte VOID = 0;
	public static final byte GRASS = 1;
	public static final byte RIVER = 2;
	public static final byte SNOW = 3;
	public static final byte SAND = 4;
	
	public static final Color VOID_COLOR = new Color(0, 0, 0);
	public static final Color GRASS_COLOR = new Color(0, 255, 0);
	public static final Color RIVER_COLOR = new Color(0, 0, 255);
	public static final Color SNOW_COLOR = new Color(255, 255, 255);
	public static final Color SAND_COLOR = new Color(255, 255, 0);
	
	private static ArrayList<Color> colors = new ArrayList<>();
	
	static {
		colors.add(VOID_COLOR);
		colors.add(GRASS_COLOR);
		colors.add(RIVER_COLOR);
		colors.add(SNOW_COLOR);
		colors.add(SAND_COLOR);
	}
	
	public GroundCoverMap(int xSize, int ySize) {
		values = new byte[xSize][ySize];
	}
	
	public GroundCoverMap(byte[][] values) {
		this.values = values;
	}
	
	public void setValues(Mask m, byte value) {
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				if(m.getValueAt(i, j)) values[i][j] = value;
			}
		}
	}
	
	public void setValueAt(int x, int y, byte value) {
		values[x][y] = value;
	}
	
	public byte getValueAt(int x, int y) {
		return values[x][y];
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		byte b = getValueAt(x, y);
		return colors.get(b);
	}

	@Override
	public int getXSize() {
		return values.length;
	}

	@Override
	public int getYSize() {
		return values[0].length;
	}

}
