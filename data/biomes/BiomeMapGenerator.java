package data.biomes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Random;

import data.WorldSettings;
import data.map.HeightMap;
import data.map.Mask;
import data.util.MapUtils;
import generator.noise.NoiseGenerator;

public class BiomeMapGenerator {
	
	public static Random rng = new Random(WorldSettings.seed);
	
	public static Biome getBiomeFor(float height, float rf, float te, float decisionF) {
		ArrayList<Biome> biomes = Biomes.biomeList;
		//System.out.println("requesting biome for " + height + " " + rf + " " + te);
		ArrayList<Biome> valid = new ArrayList<>();
		for(int i = 0; i < biomes.size(); i++) {
			if(biomes.get(i).isValid(height, te, rf)) valid.add(biomes.get(i));
			if(biomes.get(i).isPartValid(height, te, rf , 0f, 0.02f, 0.2f) < decisionF) valid.add(biomes.get(i));
		}
		if(valid.size() == 0) System.out.println("didnt find biome for " + te + "t   " + rf + "r   " + height + "h");
		float index = valid.size();
		index *= decisionF;
		int d = (int) index;
		return valid.get(d);
	}
}
