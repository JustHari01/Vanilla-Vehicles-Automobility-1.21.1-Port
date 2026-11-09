package io.github.justhari.vanillavehicles;

public enum VehicleKind {
    MINECAR("minecar", 1, .177, .009f, 1.3f, 1.0f, .22, -.14, Fuel.COAL),
    LOGCAR("logcar", 2, .19, .008f, 1.3f, 1.0f, .47, .12, Fuel.COAL),
    MOBTRUCK("mobtruck", 2, .194, .007f, 1.7f, 1.5f, .48, -.1, Fuel.REDSTONE),
    PLANE("plane", 1, 1.1, .006f, 2.2f, 1.7f, .56, -.2, Fuel.REDSTONE),
    BALLOON("hot_air_balloon", 1, .55, .04f, 1.4f, 1.2f, .3, -.2, Fuel.HEAT),
    STEAMBOAT("steamboat", 3, .12, .005f, 2.6f, 1.5f, 1, -1.2, Fuel.COAL),
    PRISMER("prismer", 1, .2, 0, 1.8f, .9f, 0, -.25, Fuel.CONDUIT),
    MECH("mech", 1, .09, 0, 1.8f, 3.0f, 1.6, -.1, Fuel.NONE);

    public enum Fuel { COAL, REDSTONE, HEAT, CONDUIT, NONE }
    public final String id;
    public final int seats;
    public final double speed;
    public final float fuelUse, width, height;
    public final double seatY, seatZ;
    public final Fuel fuel;
    VehicleKind(String id, int seats, double speed, float fuelUse, float width, float height, double seatY, double seatZ, Fuel fuel) {
        this.id=id; this.seats=seats; this.speed=speed; this.fuelUse=fuelUse;
        this.width=width; this.height=height; this.seatY=seatY; this.seatZ=seatZ; this.fuel=fuel;
    }
    public static VehicleKind named(String id) {
        for (var kind : values()) if (kind.id.equals(id)) return kind;
        throw new IllegalArgumentException("Unknown vehicle " + id);
    }
}
