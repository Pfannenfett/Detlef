package generator.height;

import data.World;
import data.map.HeightMap;
import data.map.Mask;
import data.map.layout.CellularLayoutMap;

public interface ITerrainGenerator {
	public String identify();
	public boolean isLowerTerrain();
	public HeightMap generateTerrain(CellularLayoutMap clm, HeightMap layout, World world, long seed);
}
