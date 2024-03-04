package data.map;

import java.awt.Color;

public class PrecipitationMap extends MapBase {

	public PrecipitationMap(int xSize, int ySize) {
		super(xSize, ySize);
		
	}
	
	public PrecipitationMap(float[][] f) {
		super(f);
	}
	
	public PrecipitationMap(MapBase a) {
		super(a.getContent());
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		if(content[x][y] == 0) return Color.LIGHT_GRAY;
		return new Color(0, 0, content[x][y]);
	}
}
