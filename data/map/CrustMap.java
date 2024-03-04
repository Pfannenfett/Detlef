package data.map;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;

import generator.tectonics.Plate;

public class CrustMap implements IPrintableMap{

	Plate[][] content;
	public CrustMap(int xSize, int ySize) {
		content = new Plate[xSize][ySize];
	}
	
	
	public Plate getPlateAt(int x, int y) {
		try {
			return content[x][y];
		} catch(IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	public HashSet<Plate> getAllPlates() {
		HashSet<Plate> ret = new HashSet<>();
		Plate p;
		for(int i = 0; i < getXSize(); i++) {
			for(int j = 0; j < getYSize(); j++) {
				p = content[i][j];
				if(!ret.contains(p)) {
					ret.add(p);
				}
			}
		}	
		return ret;
	}
	
	public float[][] getPlateCompletely(Plate p) {
		float[][] ret = new float[getXSize()][getYSize()];
		for(int i = 0; i < getXSize(); i++) {
			for(int j = 0; j < getYSize(); j++) {
				if(content[i][j] == p) {
					if(p.isOceanPlate()) {
						ret[i][j] = 0.3F;
					} else {
						ret[i][j] = 0.6F;
					}
				} else {
					ret[i][j] = -1;
				}
			}
		}
		return ret;
	}
	
	public void setPlateAt(int x, int y, Plate value) {
		content[x][y] = value;
	}

	@Override
	public Color getColorAt(int x, int y) {
		return getPlateAt(x, y).getColor();
	}

	@Override
	public int getXSize() {
		return content.length;
	}

	@Override
	public int getYSize() {
		return content[0].length;
	}
	
	public Plate[][] getContent() {
		return content;
	}

}
