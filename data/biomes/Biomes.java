package data.biomes;

import java.awt.Color;
import java.util.ArrayList;

public class Biomes {
	public static final Biome OCEAN = new Biome("Ocean", Biome.OCEANCOLOR, Biome.ALLT, Biome.ALLP, Biome.UNDERWATER, Biome.OCEANS);
	public static final Biome RIVER = new Biome("River", Color.BLACK, Biome.ALLT, Biome.ALLP, Biome.LAND, Biome.PLAINS);
	
	public static final Biome ICEMOUNTAINS = new Biome("Ice Mountains", Biome.MOUNTAINCOLOR, Biome.ARCTIC, Biome.ALLP, Biome.PEAK, Biome.MOUNTAINS);
	public static final Biome SNOWPLAINS = new Biome("Snowplains", Biome.ICECOLOR, Biome.ARCTIC, Biome.ALLP, Biome.LOW, Biome.PLAINS);
	public static final Biome GLACIERS = new Biome("Glaciers", Biome.ICECOLOR, Biome.ARCTIC, Biome.ALLP, Biome.HIGH, Biome.HILLS);
	
	public static final Biome NORDICMOUNTAINS = new Biome("Ice Mountains", Biome.MOUNTAINCOLOR, Biome.COLD, Biome.ALLP, Biome.PEAK, Biome.MOUNTAINS);
	public static final Biome BOREALFOREST = new Biome("Borealforest", Biome.BOREALFORESTCOLOR, Biome.COLD, Biome.MODERATE, Biome.LOW, Biome.PLAINS);
	public static final Biome BOREALFORESTHILLS = new Biome("Borealforesthills", Biome.BOREALFORESTCOLOR, Biome.COLD, Biome.MODERATE, Biome.HIGH, Biome.HILLS);
	public static final Biome TAIGA = new Biome("Taiga", Biome.TAIGACOLOR, Biome.COLD, Biome.DRY, Biome.LOW, Biome.PLAINS);
	public static final Biome TAIGAHILLS = new Biome("Taigahills", Biome.TAIGACOLOR, Biome.COLD, Biome.DRY, Biome.HIGH, Biome.HILLS);
	public static final Biome TUNDRA = new Biome("Tundra", Biome.TUNDRACOLOR, Biome.COLD, Biome.WET, Biome.LOW, Biome.PLAINS);
	public static final Biome TUNDRAHILLS = new Biome("Tundrahills", Biome.TUNDRACOLOR, Biome.COLD, Biome.WET, Biome.HIGH, Biome.HILLS);
	
	public static final Biome ALPINEMOUNTAINS = new Biome("Ice Mountains", Biome.MOUNTAINCOLOR, Biome.TEMPERATE, Biome.ALLP, Biome.PEAK, Biome.MOUNTAINS);
	public static final Biome PLAINS = new Biome("Plains", Biome.PLAINCOLOR, Biome.TEMPERATE, Biome.MODERATE, Biome.LOW, Biome.PLAINS);
	public static final Biome HILLS = new Biome("Hills", Biome.PLAINCOLOR, Biome.TEMPERATE, Biome.MODERATE, Biome.HIGH, Biome.HILLS);
	public static final Biome FOREST = new Biome("Forest", Biome.FORESTCOLOR, Biome.TEMPERATE, Biome.DRY, Biome.LOW, Biome.PLAINS);
	public static final Biome WOODEDHILLS = new Biome("Woodedhills", Biome.FORESTCOLOR, Biome.TEMPERATE, Biome.DRY, Biome.HIGH, Biome.HILLS);
	public static final Biome SWAMP = new Biome("Swamp", Biome.SWAMPCOLOR, Biome.TEMPERATE, Biome.WET, Biome.LOW, Biome.PLAINS);
	public static final Biome LUSHHILLS = new Biome("Lushhills", Biome.SWAMPCOLOR, Biome.TEMPERATE, Biome.WET, Biome.HIGH, Biome.HILLS);
	
	public static final Biome DESERTMOUNTAINS = new Biome("Ice Mountains", Biome.MOUNTAINCOLOR, Biome.WARM, Biome.ALLP, Biome.PEAK, Biome.MOUNTAINS);
	public static final Biome SAVANNA = new Biome("Savanna", Biome.SAVANNACOLOR, Biome.WARM, Biome.MODERATE, Biome.LOW, Biome.PLAINS);
	public static final Biome MEDITERRANEAN = new Biome("Mediterranean", Biome.SAVANNACOLOR, Biome.WARM, Biome.MODERATE, Biome.HIGH, Biome.HILLS);
	public static final Biome DESERT = new Biome("Desert", Biome.DESERTCOLOR, Biome.WARM, Biome.DRY, Biome.LOW, Biome.PLAINS.getDuneVariant());
	public static final Biome DESERTHILLS = new Biome("Dunes", Biome.DESERTCOLOR, Biome.WARM, Biome.DRY, Biome.HIGH, Biome.HILLS);
	public static final Biome OASIS = new Biome("Oasis", Biome.MESACOLOR, Biome.WARM, Biome.WET, Biome.LOW, Biome.PLAINS);
	public static final Biome MESA = new Biome("Mesa", Biome.MESACOLOR, Biome.WARM, Biome.WET, Biome.HIGH, Biome.HILLS.getMesaVariant());
	
	public static final Biome JUNGLEMOUNTAINS = new Biome("Ice Mountains", Biome.MOUNTAINCOLOR, Biome.HOT, Biome.ALLP, Biome.PEAK, Biome.MOUNTAINS);
	public static final Biome JUNGLE = new Biome("Jungle", Biome.JUNGLECOLOR, Biome.HOT, Biome.MODERATE, Biome.LOW, Biome.PLAINS);
	public static final Biome JUNGLEHILLS = new Biome("Junglehills", Biome.JUNGLECOLOR, Biome.HOT, Biome.MODERATE, Biome.HIGH, Biome.HILLS);
	public static final Biome REDDESERT = new Biome("Red Desert", Biome.REDDESERTCOLOR, Biome.HOT, Biome.DRY, Biome.LOW, Biome.PLAINS);
	public static final Biome REDDUNES = new Biome("Red Dunes", Biome.REDDESERTCOLOR, Biome.HOT, Biome.DRY, Biome.HIGH, Biome.HILLS);
	public static final Biome JUNGLESWAMP = new Biome("Jungleswamp", Biome.JUNGLECOLOR.darker(), Biome.HOT, Biome.WET, Biome.LOW, Biome.PLAINS);
	public static final Biome LUSHTROPICHILLS = new Biome("Lush Tropic Hills", Biome.JUNGLECOLOR.darker(), Biome.HOT, Biome.WET, Biome.HIGH, Biome.HILLS);
	
	public static final ArrayList<Biome> biomeList = new ArrayList<>();
	static {
		biomeList.add(OCEAN);
		biomeList.add(ICEMOUNTAINS);
		biomeList.add(SNOWPLAINS);
		biomeList.add(GLACIERS);
		biomeList.add(NORDICMOUNTAINS);
		biomeList.add(BOREALFOREST);
		biomeList.add(BOREALFORESTHILLS);
		biomeList.add(TAIGA);
		biomeList.add(TAIGAHILLS);
		biomeList.add(TUNDRA);
		biomeList.add(TUNDRAHILLS);
		biomeList.add(ALPINEMOUNTAINS);
		biomeList.add(PLAINS);
		biomeList.add(HILLS);
		biomeList.add(FOREST);
		biomeList.add(WOODEDHILLS);
		biomeList.add(SWAMP);
		biomeList.add(LUSHHILLS);
		biomeList.add(DESERTMOUNTAINS);
		biomeList.add(SAVANNA);
		biomeList.add(MEDITERRANEAN);
		biomeList.add(DESERT);
		biomeList.add(DESERTHILLS);
		biomeList.add(OASIS);
		biomeList.add(MESA);
		biomeList.add(JUNGLEMOUNTAINS);
		biomeList.add(JUNGLE);
		biomeList.add(JUNGLEHILLS);
		biomeList.add(REDDESERT);
		biomeList.add(REDDUNES);
		biomeList.add(JUNGLESWAMP);
		biomeList.add(LUSHTROPICHILLS);
	}
}
