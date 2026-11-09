/**
 * Registry for active vehicle behavior instances
 * Manages creation, retrieval, and cleanup of vehicle behaviors
 * Ensures single instance per vehicle and prevents memory leaks
 */

import { PlaneBehavior } from "../behaviors/PlaneBehavior";
import { SteamBoatBehavior } from "../behaviors/SteamboatBehavior";
import { BalloonBehavior } from "../behaviors/HotAirBalloonBehavior";
import { MechBehavior } from "../behaviors/MechBehavior";
import { PrismerBehavior } from "../behaviors/PrismerBehavior";
import { VEHICLE_TYPES } from "../config/constants";

export class VehicleRegistry {
    static activeMechBehaviors = new Map();
    static activePlaneBehaviors = new Map();
    static activeSteamboatBehaviors = new Map();
    static activeHotAirBalloonBehaviors = new Map();
    static activePrismerBehaviors = new Map();

    static initialize() {
        // Any initialization logic if needed
    }

    static createBehavior(vehicle, player) {
        switch (vehicle.typeId) {
            case VEHICLE_TYPES.PLANE: {
                let behavior = this.activePlaneBehaviors.get(vehicle.id);
                if (!behavior) {
                    behavior = new PlaneBehavior(vehicle, player);
                    this.activePlaneBehaviors.set(vehicle.id, behavior);
                } else {
                    behavior.setPlayer(player);
                }
                return behavior;
            }

            case VEHICLE_TYPES.STEAMBOAT: {
                let behavior = this.activeSteamboatBehaviors.get(vehicle);
                if (!behavior) {
                    behavior = new SteamBoatBehavior(vehicle, player);
                    this.activeSteamboatBehaviors.set(vehicle, behavior);
                } else {
                    behavior.setPlayer(player);
                }
                return behavior;
            }

            case VEHICLE_TYPES.MECH: {
                let behavior = this.activeMechBehaviors.get(vehicle);
                if (!behavior) {
                    behavior = new MechBehavior(vehicle, player);
                    this.activeMechBehaviors.set(vehicle, behavior);
                } else {
                    behavior.setPlayer(player);
                }
                return behavior;
            }

            case VEHICLE_TYPES.HOT_AIR_BALLOON: {
                let behavior = this.activeHotAirBalloonBehaviors.get(vehicle);
                if (!behavior) {
                    behavior = new BalloonBehavior(vehicle, player);
                    this.activeHotAirBalloonBehaviors.set(vehicle, behavior);
                } else {
                    behavior.setPlayer(player);
                }
                return behavior;
            }

            case VEHICLE_TYPES.PRISMER: {
                let behavior = this.activePrismerBehaviors.get(vehicle);
                if (!behavior) {
                    behavior = new PrismerBehavior(vehicle, player);
                    this.activePrismerBehaviors.set(vehicle, behavior); // Fixed: was setting to Hot Air Balloon map
                } else {
                    behavior.setPlayer(player);
                }
                return behavior;
            }

            default:
                return null;
        }
    }

    static clearPlayerFromBehavior(vehicle) {
        const behavior = this.getBehavior(vehicle);
        if (behavior) {
            // Cache the current player state before clearing
            behavior.cachePlayerState();
            behavior.clearPlayer();
        }
    }
    static getBehavior(vehicle) {
        return this.activePlaneBehaviors.get(vehicle.id) || 
               this.activeSteamboatBehaviors.get(vehicle.id) || 
               this.activeHotAirBalloonBehaviors.get(vehicle.id) || 
               this.activeMechBehaviors.get(vehicle.id) || 
               this.activePrismerBehaviors.get(vehicle.id);
    }
}