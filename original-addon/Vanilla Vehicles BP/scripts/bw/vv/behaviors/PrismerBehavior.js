import { BaseBehavior } from "./BaseBehavior";

export class PrismerBehavior extends BaseBehavior {
    constructor(vehicle, player) {
        super(vehicle, player);
        
        this.vehicle = vehicle;
        this.player = player;

        // Track water-based effect states
        this.wasInWater = false;
        this.hasAppliedEffects = false;

        // Cache rotation data
        this.lastPlayerRotation = { x: 0, y: 0 };
        this.lastVehicleRotation = { x: 0, y: 0 };

    }
    
    setPlayer(player) {
        // Cache current state before switching player
        if (this.player) {
            this.cachePlayerState();
        }
        this.player = player;
    }

    clearPlayer() {
        // Cache state before clearing
        if (this.player) {
            this.cachePlayerState();
        }
        this.player = null;
    }


    update() {
        if (this.removed || !this.updateBaseProperties()) return;

        const rider = this.getRider();
        const isInWater = this.vehicle.isInWater;
        const hasFuel = this.fuel > 0;

        // Handle water breathing and night vision effects
        if (rider && hasFuel && isInWater) {
            // Apply effects when all conditions are met
            if (!this.hasAppliedEffects) {
                this.player.addEffect('conduit_power', 999999, { amplifier: 9, showParticles: false });
                //this.player.addEffect('haste', 999999, { amplifier: 9, showParticles: false });
                this.hasAppliedEffects = true;
            }
        } else {
            // Remove effects if any condition is not met
            if (this.hasAppliedEffects) {
                this.removeEffectsFromRider(rider);
                this.hasAppliedEffects = false;
            }
        }

        // Handle knockback when leaving water
        if (this.wasInWater && !isInWater) {
            this.vehicle.applyImpulse({ x: 0, y: -0.14, z: 0 });
        }
        else if
        (!isInWater) {
            this.vehicle.applyImpulse({ x: 0, y: -0.14, z: 0 });
        }

        this.wasInWater = isInWater;
    }

    // Helper method to remove specific effects
    removeEffectsFromRider(rider) {
        
        try {
            this.player.removeEffect('conduit_power');
            this.player.removeEffect('haste');
        } catch (e) {
            console.warn("Failed to remove effects:", e);
        }
    }

    // Clean up when prismer is removed
    remove() {
        if (this.removed) {
            console.warn("Prevented repeated remove()");
            return;
        }
        
        this.removed = true;
        
        // Remove effects from rider before removing vehicle
        const rider = this.getRider();
        if (rider && this.hasAppliedEffects) {
            this.removeEffectsFromRider(rider);
        }
        
        try {
            this.vehicle.triggerEvent('event:remove');
        } catch (e) {
            console.warn("Failed to trigger event:remove", e);
        }
        
        this.cleanup_internal();
    }

    // Internal cleanup called by parent class
    cleanup_internal() {
        super.cleanup();
    }
}