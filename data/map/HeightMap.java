package data.map;

import java.awt.Color;

public class HeightMap extends MapBase {
	private int t = 0;
	private boolean isUpperMap = true;

	public HeightMap(int xSize, int ySize) {
		super(xSize, ySize);
	}
	
	public HeightMap(float[][] temp) {
		super(temp);
	}
	
	public HeightMap(MapBase b) {
		this(b.getContent());
	}
	
	public boolean isUpperMap() {
		return isUpperMap;
	}
	
	public void setUpperMap(boolean isUpperMap) {
		this.isUpperMap = isUpperMap;
	}
	
	public int getConvertedHeightAt(int x, int y, int maxHeight) {
		return (int) (super.getValueAt(x, y) * maxHeight);
	}
	
	public void drawCircle() {
		int xh = getXSize() / 2;
		int yh = getYSize() / 2;
		for(int i = 0; i < getXSize(); i++) {
			for(int j = 0; j < getYSize(); j++) {
				int x = i - xh;
				int y = j - yh;
				if(x * x + y * y < xh * yh * 0.25F) {
					setValueAt(i, j, 0.9F);
				}
			}
		}
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		int i = (int) (content[x][y] * 255);
		i = (int) Math.max(Math.min(i, 255), 0);
		//System.out.print(content[x][y] + " ");
		if(t == 1023) {
			//System.out.println();
			t = 0;
		}
		t++;
		return new Color(i, i, i);
	}
}
