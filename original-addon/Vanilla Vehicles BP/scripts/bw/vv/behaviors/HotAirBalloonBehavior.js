import { GameMode, system } from "@minecraft/server";
import { BaseBehavior } from "./BaseBehavior";
import { getWind } from "../core/windSystem.js";
import { BALLOON_CONFIG } from "../config/constants";

export class BalloonBehavior extends BaseBehavior {
    constructor(vehicle, player) {
        super(vehicle, player);
        
        this.vehicle = vehicle;
        this.player = player;

        // Balloon-specific properties from config
        this.maxverticalAscendSpeed = BALLOON_CONFIG.MAX_VERTICAL_ASCEND_SPEED
        this.maxhorizontalBalloonSpeed = BALLOON_CONFIG.MAX_HORIZONTAL_SPEED;
        this.horizontalAccel = BALLOON_CONFIG.HORIZONTAL_ACCEL;
        this.verticalAccel = BALLOON_CONFIG.VERTICAL_ACCEL;
        this.verticalAscendSpeed = BALLOON_CONFIG.VERTICAL_ASCEND_SPEED;
        this.verticalDescendSpeed = BALLOON_CONFIG.VERTICAL_DESCEND_SPEED;
        this.windOffset = BALLOON_CONFIG.WIND_OFFSET || 0;
        this.horizontalBalloonSpeed=0;
        // State properties
        this.airborne = false;
        this.rider = false;
        this.verticalDir = "down";
        this.vx=0;
        this.vy=0;
        this.noRiderTicks = 0; // Track how long there's been no rider
        this.noRiderDelay = 40; // 2 seconds default
        
        // Input control state
        this.controls = {
            jumping: false
        };
    }
    
    setPlayer(player) {
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

        this.heated = this.vehicle.getProperty('bw_vv:heated')
        
        this.rider = this.getRider();

        // Must be fueled
        if (this.fuel <= 0) {
            // Out of fuel - descend but don't reset speed until landing
            this.verticalDir = "down";
        }

        this.updateControls();
        
        // Handle takeoff when jump pressed
        if (!this.airborne && this.controls.jumping && this.heated && this.fuel > 0) {
            this.enterAirborneMode();
        }
        
        if (this.airborne) {
            this.checkGroundCollision();
            this.handleAirborneBehavior();
            
        }

        //this.feedback();
    }

    updateControls() {
        const rider = this.getRider();
        if (!rider) {
            this.controls.jumping = false;
            return;
        }

        this.controls.jumping = this.player ? this.player.isJumping : this.lastPlayerState.isJumping;
    }

    checkGroundCollision() {
        if (!this.vehicle || !this.vehicle.isValid) return;
        // Check if balloon has landed while airborne
 if (this.airborneGraceTicks > 0) {
        this.airborneGraceTicks--;
        return;
    }
    
    try {
        if (this.vehicle.isOnGround || this.vehicle.isInWater) {
            this.enterGroundedMode();
        }
    } catch (error) {
        this.removed = true;
    }
    }
    feedback() {
        this.player?.onScreenDisplay.setActionBar(
            `Speed: ${this.horizontalBalloonSpeed.toFixed(2)}`
        );
    }
    handleAirborneBehavior() {
        // Vertical movement control
        if (this.rider && this.controls.jumping && this.fuel > 0) {
            this.verticalDir = "up";
        } else {
            this.verticalDir = "down";
        }

        // Vertical velocity
        const vy = (this.verticalDir === "up") 
            ? this.verticalAscendSpeed 
            : this.verticalDescendSpeed;

        // Wind direction with offset
        let wind = getWind();
        const windVec = this.cardinalToVector(wind);

        // Accelerate toward max speed
        
        this.horizontalBalloonSpeed = Math.min(this.horizontalBalloonSpeed + this.horizontalAccel, this.maxhorizontalBalloonSpeed)
        this.verticalAscendSpeed = Math.min(this.verticalAscendSpeed + this.verticalAccel, this.maxverticalAscendSpeed)

        this.vx = windVec.x;
        this.vz = windVec.z;

        // Apply movement
        try {
            this.vehicle.applyKnockback(
                { 
                    x: this.vx * this.horizontalBalloonSpeed,
                    z: this.vz * this.horizontalBalloonSpeed
                }, 
                vy
            );
        } catch (error) {
            // Handle any movement errors silently
        }
    }

    // Rotate cardinal direction clockwise N→E→S→W
    rotateDirection(baseDir, offset) {
        const dirs = ["E", "S", "W", "N"];
        let idx = dirs.indexOf(baseDir.toUpperCase());
        if (idx === -1) idx = 0;
        return dirs[(idx + offset) % 4];
    }

    // Convert cardinal direction to vector
    cardinalToVector(dir) {
        switch (dir.toUpperCase()) {
            case "N": return { x: 1, z: 0 };
            case "S": return { x: -1, z: 0 };
            case "E": return { x: 0, z: -1 };
            case "W": return { x: 0, z: 11 };
            default:  return { x: 0, z: 0 };
        }
    }

    // State transition methods
    enterAirborneMode() {
        this.vehicle.triggerEvent('event:enter_flight_mode');
        //this.player.sendMessage(`§aEntering airborne mode!`, );
        this.verticalAscendSpeed = 0;
        this.horizontalBalloonSpeed = 0; // Stop horizontal movement on takeoff
        this.airborneGraceTicks = 10;
        this.airborne = true;
        this.vehicle.setProperty('bw_vv:balloon_state', "airborne");
        // Optional: trigger event or animation
        // this.vehicle.triggerEvent('event:enter_airborne_mode');
    }

    enterGroundedMode() {
        this.vehicle.triggerEvent('event:enter_ground_mode');
        //this.player.sendMessage(`§cLanded - ready to take off again`);
        this.airborne = false;
        this.horizontalBalloonSpeed = 0; // Stop horizontal movement on landing
        this.horizontalBalloonSpeed = 0; // Stop horizontal movement on landing
        this.vehicle.setProperty('bw_vv:balloon_state', "grounded");
        // Optional: trigger event or animation
        // this.vehicle.triggerEvent('event:enter_grounded_mode');
    }

    remove() {
        this.removed = true;
        
        // Eject any riders before removal
        this.vehicle.getComponent('minecraft:rideable')?.ejectRiders();
        const isCreative = this.player ? 
            (this.player.getGameMode() === GameMode.Creative) : false;
        this.vehicle.triggerEvent(isCreative ? 'event:remove_creative' : 'event:remove');

        this.cleanup_internal();
    }
    
    cleanup_internal() {
        super.cleanup();
    }
}