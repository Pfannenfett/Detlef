package data.map;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;

import data.util.MapUtils;

public abstract class MapBase implements IPrintableMap {
	float[][] content;
	HashSet<float[]> changes;
	float[][] changelog;
	
	private int shiftX = 0;
	private int shiftY = 0;
	
	public void delete() {
		content = null;
		changelog = null;
		changes = null;
	}
	
	public MapBase(float[][] f) {
		content = f.clone();
		changelog = new float[f.length][f[0].length];
	}
	
	public MapBase(int xSize, int ySize) {
		content = new float[xSize][ySize];
		changelog = new float[xSize][ySize];
	}
	
	public float[][] getContent() {
		return content;
	}
	
	public float[][] clone() {
		float[][] ret = new float[content.length][content[0].length];
		for(int i = 0; i < ret.length; i++) {
			for(int j = 0; j < ret[0].length; j++) {
				ret[i][j] = content[i][j];
			}
		}
		return ret;
	}
	
	public HeightMap toHeightMap() {
		return new HeightMap(this);
	}
	
	public MapBase getChangeLog() {
		HeightMap h = new HeightMap(changelog);
		changelog = new float[changelog.length][changelog[0].length];
		h.add(0.5F);
		return h;
	}
	
	public void setContent(float[][] content) {
		this.content = content;
		changelog = new float[content.length][content[0].length];
	}
	
	public void shift(int x, int y) {
		float[][] nc = new float[content.length + x][content[0].length + y];
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				if(i+x<0||j+y<0) continue;
				nc[i+x][j+y] = content[i][j];
			}
		}
		content = nc;
	}
	
	public void coordinateShift(int x, int y) {
		shiftX = x;
		shiftY = y;
	}
	
	public int[] findCoordsForHeight(float low, float high) {
		for(int i = 0; i < content.length; i += 16) {
			for(int j = 0; j < content[i].length; j += 16) {
				if(content[i][j] > low && content[i][j] < high) {
					int[] ret = {i, j};
					return ret;
				}
			}
		}
		
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[i].length; j++) {
				if(content[i][j] > low && content[i][j] < high) {
					int[] ret = {i, j};
					return ret;
				}
			}
		}
		int[] nothingFound = {-1, -1};
		return nothingFound;
	}
	
	public float getValueAt(int x, int y) {
		try {
			return content[x][y];
		} catch(IndexOutOfBoundsException e) {
			return -1F;
		}
	}
	
	public float getSafeValueAt(int x, int y) {
		try {
			return content[x - shiftX][y - shiftY];
		} catch(IndexOutOfBoundsException e) {
			return 0f;
		}
	}
	
	public void setValueAt(int x, int y, float value) {
		try {content[x][y] = Math.max(Math.min(value, 1f), 0f); } catch(ArrayIndexOutOfBoundsException e) {}
	}
	
	public void invert() {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[i].length; j++) {
				content[i][j] = 1f - content[i][j];
			}
		}
	}
	
	public void change(int x, int y, float delta) {
		if(changes == null) changes = new HashSet<>();
		float[] f = {x, y, delta};
		changes.add(f);
	}
	
	public void perfChanges() {
		Iterator i = changes.iterator();
		while(i.hasNext()) {
			float[] f = (float[]) i.next();
			int x = (int) f[0];
			int y = (int) f[1];
			setValueAt(x, y, getValueAt(x, y) + f[2]);
			changelog[x][y] = changelog[x][y] + f[2];
		}
		changes = new HashSet<>();
	}
	
	public void perfChanges(HashSet<float[]> c) {
		Iterator i = c.iterator();
		while(i.hasNext()) {
			float[] f = (float[]) i.next();
			int x = (int) f[0];
			int y = (int) f[1];
			if(!isInBounds(x, y)) continue;
			float h = getValueAt(x, y) + f[2];
			setValueAt(x, y, h);
			changelog[x][y] += f[2];
		}
	}
	
	public synchronized void perfChanges(float[][] c) {
		for(int i = 0; i < c.length; i++) {
			float[] f = c[i];
			if(f == null) continue;
			int x = (int) f[0];
			int y = (int) f[1];
			if(!isInBounds(x, y)) continue;
			float h = getValueAt(x, y) + f[2];
			setValueAt(x, y, h);
			changelog[x][y] += f[2];
		}
		this.notify();
	}
	
	
	public void setUncappedValueAt(int x, int y, float value) {
		content[x][y] = value;
	}
	
	public void fillWith(float f) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = f;
			}
		}
	}
	
	public Mask above(float f) {
		Mask m = new Mask(getXSize(), getYSize());
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				m.setValueAt(i, j, content[i][j] > f);
			}
		}
		return m;
	}
	
	public Mask above(MapBase map) {
		Mask m = new Mask(getXSize(), getYSize());
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				m.setValueAt(i, j, content[i][j] > map.getValueAt(i, j));
			}
		}
		return m;
	}
	
	public Mask below(float f) {
		Mask m = new Mask(getXSize(), getYSize());
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				m.setValueAt(i, j, content[i][j] < f);
			}
		}
		return m;
	}
	
	public Mask below(MapBase map) {
		Mask m = new Mask(getXSize(), getYSize());
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				m.setValueAt(i, j, content[i][j] < map.getValueAt(i, j));
			}
		}
		return m;
	}
	
	public void applyMask(Mask mask, float defaultHeight) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				if(!mask.getValueAt(i, j)) content[i][j] = defaultHeight;
			}
		}
	}
	
	public void applyMask(Mask mask) {
		applyMask(mask, 0);
	}
	
	public void add(float f) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] += f;
			}
		}
	}
	
	public void cap() {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				float f = content[i][j];
				f = Math.min(1, f);
				f = Math.max(0, f);
				content[i][j] = f;
			}
		}
	}
	
	public void mult(float f) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] *= f;
			}
		}
	}
	
	public void pow(float f) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = (float) Math.pow(content[i][j], f);
			}
		}
	}
	
	public void pow(float[][] f) {
		if(f.length != content.length || f[0].length != content[0].length) {
			throw new IllegalArgumentException("Maps you tried to pow have different Sizes, Arglength: " + f.length + " != contentLength: " + content.length);
		}
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = (float) Math.pow(content[i][j], f[i][j]);
			}
		}
	}
	
	public void addDimin(float f) {
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				content[i][j] = MapUtils.addDimin(f, content[i][j]);
			}
		}
	}
	
	@Override
	public int getXSize() {
		return content.length;
	}
	
	@Override
	public int getYSize() {
		return content[0].length;
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		float f = Math.max(Math.min(content[x][y], 1f), 0f);
		return new Color((int) (f * 16777216));
	}
	
	public float[] get4Nb(int x, int y) {
		float[] ret = new float[4];
		ret[0] = 2;
		ret[1] = 2;
		ret[2] = 2;
		ret[3] = 2;
		try { ret[0] = getValueAt(x, y + 1); } catch(Exception e) {ret[0] = 2;}
		try { ret[1] = getValueAt(x + 1, y); } catch(Exception e) {ret[1] = 2;}
		try { ret[2] = getValueAt(x, y - 1); } catch(Exception e) {ret[2] = 2;}
		try { ret[3] = getValueAt(x - 1, y); } catch(Exception e) {ret[3] = 2;}
		return ret;
	}
	
	public float[] get8Nb(int x, int y) {
		float[] ret = new float[8];
		ret[0] = 2;
		ret[1] = 2;
		ret[2] = 2;
		ret[3] = 2;
		ret[4] = 2;
		ret[5] = 2;
		ret[6] = 2;
		ret[7] = 2;
		try { ret[0] = getValueAt(x, y + 1); } catch(Exception e) {ret[0] = 2;}
		try { ret[1] = getValueAt(x + 1, y); } catch(Exception e) {ret[1] = 2;}
		try { ret[2] = getValueAt(x, y - 1); } catch(Exception e) {ret[2] = 2;}
		try { ret[3] = getValueAt(x - 1, y); } catch(Exception e) {ret[3] = 2;}
		try { ret[4] = getValueAt(x + 1, y + 1); } catch(Exception e) {ret[4] = 2;}
		try { ret[5] = getValueAt(x + 1, y - 1); } catch(Exception e) {ret[5] = 2;}
		try { ret[6] = getValueAt(x - 1, y - 1); } catch(Exception e) {ret[6] = 2;}
		try { ret[7] = getValueAt(x - 1, y + 1); } catch(Exception e) {ret[7] = 2;}
		return ret;
	}
	
	public byte getLowestNBbySlope(int x, int y) {
		float h = getValueAt(x, y);
		float[] nbs = get8Nb(x, y);
		float minSlope = 1200F;
		byte ret = -1;
		for(byte i = 0; i < nbs.length; i++) {
			float nh = nbs[i];
			if(nh > 1 || nh < 0) continue;
			nh -= h;
			if(i >= 4) {
				nh /= 1.4F;
			}
			if(nh < minSlope) {
				minSlope = nh;
				ret = i;
			}
		}
		if(ret == -1) {
			System.out.println(h + " at " + x + "/" + y);
			for(byte i = 0; i < nbs.length; i++) {
				System.out.println(nbs[i]);
			}
		}
		return ret;	
	}
	
	public boolean isInBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < getXSize() && y < getYSize();
	}
	
	public float getMax() {
		float ret = -100000;
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				if(ret < content[i][j]) ret = content[i][j];
			}
		}
		return ret;
	}
	
	public float getMin() {
		float ret = 1100000;
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				if(ret > content[i][j]) ret = content[i][j];
			}
		}
		return ret;
	}
	
	public HeightMap max(HeightMap m) {
		HeightMap h = new HeightMap(content.length, content[0].length);
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				h.setValueAt(i, j, Math.max(content[i][j], m.getValueAt(i, j)));
			}
		}
		return h;
	}
	
	public HeightMap min(HeightMap m) {
		HeightMap h = new HeightMap(content.length, content[0].length);
		for(int i = 0; i < content.length; i++) {
			for(int j = 0; j < content[0].length; j++) {
				h.setValueAt(i, j, Math.min(content[i][j], m.getValueAt(i, j)));
			}
		}
		return h;
	}
}
