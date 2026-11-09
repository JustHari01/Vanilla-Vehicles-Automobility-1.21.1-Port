/*
  Base class for all vehicle behaviors
  Provides common functionality for vehicle physics, rider management, and state updates
  Extended by specific vehicle types (Plane, Steamboat, etc.)
*/

import { system, EntityComponentTypes } from "@minecraft/server";

export class BaseBehavior {
    constructor(vehicle, player) {
        this.vehicle = vehicle;
        this.player = player;
        this.removed = false;
        this.intervalId = null;
        this.velocity = null;
        this.horizontalSpeed = null;
        this.speed = 0;
        this.fuel = 0;
        this.level = 0;
        this.state = null;
        this.viewDir = null;
        this.playerWasJumping = false;
        this.jumpTimeout = false;
        this.lastVelocity = { x: 0, y: 0, z: 0 };
        
        // Cache last known player state
        this.lastPlayerState = {
            viewDirection: { x: 0, y: 0, z: 0 },
            rotation: { x: 0, y: 0 },
            isJumping: false,
            inputVector: null
        };

        // History arrays for smoothing
        this.histories = {
            quickX: [],
            quickZ: [],
            quickDegrees: []
        };
        
        this.init();
    }

    init() {
        this.intervalId = system.runInterval(() => this.update());
    }

    updateBaseProperties() {
        if (!this.vehicle?.isValid) {
            this.cleanup();
            return false;
        }

        this.velocity = this.vehicle.getVelocity();
        this.horizontalSpeed = Math.hypot(this.velocity.x, this.velocity.z);
        this.fuel = this.vehicle.getProperty('bw_vv:fuel_progress') || 0;
        this.level = this.vehicle.getProperty('bw_vv:level') || 0;
        
        // Use cached values if player is not available
        if (this.player) {
            this.viewDir = this.player.getViewDirection();
            this.cachePlayerState();
        } else {
            this.viewDir = this.lastPlayerState.viewDirection;
        }
        
        return true;
    }

    cachePlayerState() {
        if (!this.player) return;
        
        try {
            this.lastPlayerState.viewDirection = this.player.getViewDirection();
            this.lastPlayerState.rotation = this.player.getRotation();
            this.lastPlayerState.isJumping = this.player.isJumping;
            
            // Cache input vector if available
            const inputInfo = this.player.inputInfo;
            if (inputInfo) {
                this.lastPlayerState.inputVector = inputInfo.getMovementVector();
            }
        } catch (e) {
            // Player no longer valid, keep last cached values
    }
    }

    getRider() {
        return this.vehicle.getComponent("rideable")?.getRiders()?.find(rider => rider.typeId === "minecraft:player");
    }

    calculateQuickView(rotation) {
        const rad = rotation * (Math.PI / 180);
        return {
            x: Math.cos(rad + Math.PI / 2),
            z: Math.sin(rad + Math.PI / 2)
        };
    }

    trimHistories(maxSize = 150) {
        Object.values(this.histories).forEach(history => {
            if (history.length > maxSize) {
                history.splice(0, history.length - maxSize);
            }
        });
    }

    handleJump() {
        const isJumping = this.player ? this.player.isJumping : this.lastPlayerState.isJumping;
        
        if (isJumping && !this.playerWasJumping && !this.jumpTimeout) {
            this.onJump();
        }
        this.playerWasJumping = isJumping;
    }

    cleanup() {
        if (this.intervalId) {
            system.clearRun(this.intervalId);
            this.intervalId = null;
        }
    }

    remove() {
        if (this.removed) return;
        
        this.removed = true;
        const rideable = this.vehicle.getComponent(EntityComponentTypes.Rideable);
        rideable?.ejectRiders();
        
        system.runTimeout(() => {
            try {
                this.vehicle.triggerEvent('event:remove');
            } catch (e) {
                console.warn("Failed to trigger event:remove", e);
            }
        }, 2);
        
        this.player.onScreenDisplay.setActionBar("Â§aRemoved");
        this.cleanup();
    }

    onJump() {
        // Override in subclasses
    }
}