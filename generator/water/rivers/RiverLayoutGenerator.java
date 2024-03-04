package generator.water.rivers;

import data.map.HeightMap;
import data.map.PrecipitationMap;

public class RiverLayoutGenerator {
	
	private static PrecipitationMap rainfall = null;
	private static RiverFlowField rff = null;
	
	public static RiverInfo createCellularLayout(HeightMap layout, float fromHeight, float toHeight, int minFlow, long seed) {
		rff = new RiverFlowField(layout.getXSize(), layout.getYSize(), 64, 0.8f, 0.1f, 0.1f, 0.0f);
		rff.init(layout, seed, fromHeight, toHeight);
		return rff.toRiverInfo(minFlow);
	}
	
	public static RiverInfo createTraceLayout(HeightMap layout, float fromHeight, float toHeight, long seed) {
		return RiverBuilder.riverMask(layout, seed, fromHeight + 0.1F, fromHeight, toHeight);
	}
	
	public static RiverFlowField getRff() {
		return rff;
	}
	
	public static void setRainfall(PrecipitationMap rainfall) {
		RiverLayoutGenerator.rainfall = rainfall;
	}
	
	public static PrecipitationMap getRainfall() {
		return rainfall;
	}
}
