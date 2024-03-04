package generator;

import data.map.CrustMap;
import data.map.HeightMap;
import generator.tectonics.HeightGenerator;
import generator.tectonics.TectonicsGenerator;

public class GeneratorHandler {
	
	public static CrustMap generateTectonicMap(int xSize, int ySize, int plateCount, float oceanPerc, long seed) {
		return TectonicsGenerator.createTectonicMap(xSize, ySize, plateCount, oceanPerc, seed);
	}
	
	public static HeightMap generateTectonicLayoutMap(CrustMap m, int age) {
		return HeightGenerator.createTectonicLayoutHeightMap(m, age);
	}
	
}
