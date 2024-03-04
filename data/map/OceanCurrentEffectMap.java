package data.map;

import java.awt.Color;

public class OceanCurrentEffectMap extends MapBase {

	public OceanCurrentEffectMap(float[][] f) {
		super(f);
		// TODO Auto-generated constructor stub
	}
	
	public OceanCurrentEffectMap(int xSize, int ySize) {
		super(xSize, ySize);
	}
	
	public OceanCurrentEffectMap(MapBase a) {
		super(a.getContent());
	}
	
	@Override
	public Color getColorAt(int x, int y) {
		try {
			return content[x][y] < 0.5 ? new Color(0f, 0f, (float) (2 * (0.5-content[x][y]))) : new Color((int) (255 * 2 * (content[x][y] - 0.5)), 0, 0);
		} catch(Exception e) {
			System.out.println(content[x][y]);
			e.printStackTrace();
		}
		return Color.GREEN;
		//return super.getColorAt(x, y);
	}
}
