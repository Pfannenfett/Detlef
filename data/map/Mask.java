package data.map;

import java.awt.Color;

public class Mask implements IPrintableMap {
	
	private boolean[][] content;
	
	public Mask(int xSize, int ySize) {
		content = new boolean[xSize][ySize];
	}
	
	public Mask(int xSize, int ySize, boolean fill) {
		content = new boolean[xSize][ySize];
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = fill;
			}
		}
	}
	
	public Mask(boolean[][] content) {
		this.content = content;	
	}
	
	public boolean[][] getContent() {
		return content;
	}
	
	public boolean isMixed() {
		boolean first = content[0][0];
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				if(content[i][j] != first) return true; 
			}
		}
		return false;
	}
	
	public void delete() {
		content = null;
	}
	
	public Mask clone() {
		boolean[][] cloned = new boolean[content.length][content[0].length];
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				cloned[i][j] = content[i][j];
			}
		}
		Mask m = new Mask(cloned);
		return m;
	}
	
	public HeightMap toHeightMap() {
		return toHeightMap(1f, 0f);
	}
	
	public HeightMap toHeightMap(float trueHeight, float falseHeight) {
		HeightMap ret = new HeightMap(content.length, content[0].length);
		for(int i = 0; i < getXSize(); i++) {
			for(int j = 0; j < getYSize(); j++) {
				ret.setValueAt(i, j, content[i][j] ? trueHeight : falseHeight);
			}
		}
		return ret;
	}
	
	public void and(Mask m) {
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				content[i][j] = content[i][j] && m.getValueAt(i, j);
			}
		}
	}
	
	public void or(Mask m) {
		for(int i = 0; i < m.getXSize(); i++) {
			for(int j = 0; j < m.getYSize(); j++) {
				content[i][j] = content[i][j] || m.getValueAt(i, j);
			}
		}
	}
	
	public void invert() {
		for(int i = 0; i < getXSize(); i++) {
			for(int j = 0; j <getYSize(); j++) {
				content[i][j] = !content[i][j];
			}
		}
	}
	
	public boolean isInBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < content.length && y < content[0].length;
	}
	
	public boolean[] get8NB(int x, int y) {
		boolean[] ret = new boolean[8];
		try { ret[0] = getValueAt(x, y + 1); } catch(Exception e) {ret[0] = false;}
		try { ret[1] = getValueAt(x + 1, y); } catch(Exception e) {ret[1] = false;}
		try { ret[2] = getValueAt(x, y - 1); } catch(Exception e) {ret[2] = false;}
		try { ret[3] = getValueAt(x - 1, y); } catch(Exception e) {ret[3] = false;}
		try { ret[4] = getValueAt(x + 1, y + 1); } catch(Exception e) {ret[4] = false;}
		try { ret[5] = getValueAt(x + 1, y - 1); } catch(Exception e) {ret[5] = false;}
		try { ret[6] = getValueAt(x - 1, y - 1); } catch(Exception e) {ret[6] = false;}
		try { ret[7] = getValueAt(x - 1, y + 1); } catch(Exception e) {ret[7] = false;}
		return ret;
	}
	
	public void setValueAt(int x, int y, boolean value) {
		try{
			content[x][y] = value;
		} catch(ArrayIndexOutOfBoundsException r) {
			
		}
	}
	
	public boolean getValueAt(int x, int y) {
		try{
			return content[x][y];
		} catch(Exception e) {
			return false;
		}
	}

	@Override
	public Color getColorAt(int x, int y) {
		return content[x][y] ? Color.WHITE : Color.BLACK;
	}

	@Override
	public int getXSize() {
		return content.length;
	}

	@Override
	public int getYSize() {
		return content[0].length;
	}

}
