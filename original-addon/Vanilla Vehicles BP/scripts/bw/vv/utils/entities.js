/**
 * Entity utility functions for finding and manipulating game entities
 * Provides functions for finding vehicles, riders, and applying physics
 * Handles entity relationships and coordinate transformations
 */

import { world, EntityComponentTypes } from "@minecraft/server";
import { Vector } from "./Vector3";

export const getPlayerRider = (entity) => {
    const rideable = entity.getComponent("minecraft:rideable");
    return rideable?.getRiders()?.find(rider => rider.typeId === "minecraft:player") || null;
};

export const isBlockUnderneath = (entity) => {
    const { x, y, z } = entity.location;
    const blockPos = new Vector(Math.floor(x), Math.floor(y) - 1, Math.floor(z));
    return world.getDimension("overworld").getBlock(blockPos).typeId;
};

export const findRiddenVehicle = (player, family) => {
    const entities = player.dimension.getEntities({
        location: player.location,
        maxDistance: 5,
        families: family,
    });
    
    return entities.find(vehicle => 
        vehicle.getComponent("rideable")?.getRiders().includes(player)
    );
};

export const findVehicle = (family) => {
    try {
        if (!Array.isArray(family)) {
            console.warn("Family should be an array, received:", family);
            return [];
        }
        
        const dimension = world.getDimension("overworld");
        return dimension?.getEntities({ families: family }) || [];
    } catch (error) {
        console.error("Error in findVehicle function:", error);
        return [];
    }
};

export const getRidingEntity = (passenger) => {
    return passenger.dimension.getEntities().find(vehicle =>
        vehicle.getComponent("rideable")?.getRiders().includes(passenger)
    );
};

export const getRiderEntity = (entity) => {
    const riders = entity.getComponent(EntityComponentTypes.Rideable)?.getRiders();
    return riders?.find(rider => rider.typeId !== 'minecraft:player') || null;
};

export const syncBackseatRotation = () => {
    const mobtrucks = world.getDimension("overworld").getEntities({ type: "bw_vv:mobtruck" });
    
    for (const mobtruck of mobtrucks) {
        const backseat = mobtruck.getComponent("rideable")?.getRiders()
            ?.find(rider => rider.typeId === "bw_vv:mobtruck_backseat");
        
        if (backseat) {
            backseat.setRotation(mobtruck.getRotation());
        }
    }
};
const BOOST_MAGNITUDE = (vehicle) => {
    if (vehicle.typeId === "bw_vv:plane") return 0.0;
    return 0.4;
};

export const applyForwardImpulse = (vehicle) => {
    if (!vehicle.isOnGround) return;
    
    const rotation = vehicle.getRotation().y * (Math.PI / 180);
    const impulseMagnitude = BOOST_MAGNITUDE(vehicle);
    
    vehicle.applyImpulse(new Vector(
        -Math.sin(rotation) * impulseMagnitude,
        0,
        Math.cos(rotation) * impulseMagnitude
    ));
};