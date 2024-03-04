package data.map;

import java.awt.Color;

public class TemperatureMap extends MapBase {

	public TemperatureMap(int xSize, int ySize) {
		super(xSize, ySize);
		
	}
	
	public TemperatureMap(MapBase a) {
		super(a.getContent());
	}
	
	public TemperatureMap(float[][] f) {
		super(f);
	}

	@Override
	public Color getColorAt(int x, int y) {
		float f = 1 - content[x][y];
		//System.out.println(f);
		Color c;
		if(f < 0.3333333333f) {
			float g = f > 0.1666666f ? 0.5f / 0.166666f * (f - 0.166666666f) : 0f;
			g = Math.min(Math.max(g, 0f), 1f);
			c = new Color(0.8333333333f - f, g, 0);
		}
		else if(f < 0.66666666666f) {
			float b = f > 0.566666f ? 0.5f / 0.1f * (f - 0.56666f) : 0f;
			float r = f < 0.433333f ? 0.5f / 0.1f * (0.43333f - f) : 0f;
			float g = f - 0.5f;
			g = Math.abs(g);
			g = 1 - 3 * g;
			b = Math.min(Math.max(b, 0f), 1f);
			r = Math.min(Math.max(r, 0f), 1f);
			c = new Color(r, g, b);
		}
		else {
			float g = f < 0.8333333f ? 0.5f / 0.166666f * (0.833333f - f) : 0f;
			g = Math.min(Math.max(g, 0f), 1f);
			c = new Color(0, g, f - 0.16666f);
		}
		if(f == 1) return Color.LIGHT_GRAY;
		return c;
	}
}
