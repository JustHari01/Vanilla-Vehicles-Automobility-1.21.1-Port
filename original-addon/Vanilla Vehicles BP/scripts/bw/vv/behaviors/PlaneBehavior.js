import { GameMode, system } from "@minecraft/server";
import { BaseBehavior } from "./BaseBehavior";
import { VehicleRegistry } from "../core/vehicleRegistry";
import { planeAnimCorrector, smoothValue, smoothAngle, easeNegative } from "../utils/math";
import { isBlockUnderneath } from "../utils/entities";
import { PLANE_CONFIG } from "../config/constants";
import { PLAYER_DRIVE_ANIMATION_VARIABLES } from "../config/constants";

export class PlaneBehavior extends BaseBehavior {
    constructor(vehicle, player) {
        super(vehicle, player);

        this.vehicle = vehicle;
        this.player = player;
        
        // Plane-specific properties from config
        this.totalSpeed = PLANE_CONFIG.TOTAL_SPEED;
        this.verticalSpeedFactor = PLANE_CONFIG.VERTICAL_SPEED_FACTOR
        this.maxSpeed = null;
        this.acceleration = PLANE_CONFIG.ACCELERATION;
        this.verticalFactor = PLANE_CONFIG.VERTICAL_FACTOR;
        this.takeoffTime = PLANE_CONFIG.TAKEOFF_TIME;
        this.disableFlightCooldown = PLANE_CONFIG.DISABLE_FLIGHT_COOLDOWN;
        this.trickCooldown = PLANE_CONFIG.TRICK_COOLDOWN;
        this.jumpCooldown = PLANE_CONFIG.JUMP_COOLDOWN;
        this.groundOffset = 1;
        this.tickCounter = 0;
        this.lastJumpTick = -this.jumpCooldown;
        this.velocity = null;
        this.targetVerticalSpeed = null;
        this.inputVector = null;
        this.cameraClearTimeout = null; // Add this line
        this.trick = this.vehicle.getProperty('bw_vv:trick');
        this.boost = this.vehicle.getProperty('bw_vv:boost');
        this.boostSpeedMultiplier = 1.9;

        // Crash detection properties
        this.crashDetectionFrames = 0;
        this.lastHorizontalSpeed = 0;
        this.lastTotalSpeed = 0;
        this.stallFrames = 0;

        // Input control state
        this.controls = {
            forward: false,
            backward: false
        };

        // Smoothing factors from config
        this.smoothingFactorHorizontal = PLANE_CONFIG.SMOOTHING_FACTOR_HORIZONTAL;
        this.smoothingFactorHorizontal_quicker = PLANE_CONFIG.SMOOTHING_FACTOR_HORIZONTAL_QUICKER;
        this.smoothingFactorVertical = PLANE_CONFIG.SMOOTHING_VERTICAL_OFF;
        this.smoothingFactorTimeout = null;
        this.wasConditionMet = false;

        // Extended histories for plane
        this.histories = {
            ...this.histories,
            xView: [],
            yView: [],
            yView_anim: [],
            zView: [],
            quickXQuicker: [],
            quickZQuicker: []
        };

        // Cached calculations
        this.cachedRotations = {};
        this.lastRotationUpdate = 0;

        // Cache player data
        this.lastPlayerRotation = { x: 0, y: 0 };
        this.lastVehicleRotation = { x: 0, y: 0 };
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

        this.tickCounter++;
        this.trickCooldown = Math.max(0, this.trickCooldown - 1);

        const rider = this.getRider();

        this.velocity = this.vehicle.getVelocity();
        this.lastVelocity = this.velocity;
        this.horizontalSpeed = Math.hypot(this.velocity.x, this.velocity.z);
        
        // Early exit conditions
        if (!rider && this.horizontalSpeed === 0) return;
        if (!rider) {
            if (this.state === 'flying') this.enterCrashMode();
            if (this.state === 'takeoff') this.enterStationaryMode();
        }

        this.updateControls();
        this.updateRotationCache();
        this.updatePlaneProperties();
        this.updateSmoothingFactor();
        this.calculateSmoothValues();
        this.handleStateLogic();
        this.handleJump();
        this.trimHistories();
        //this.updateActionBar();
    }
    // Process input from player (similar to steamboat)
    updateControls() {
        const rider = this.getRider();
        if (!rider) {
            this.controls.forward = false;
            this.controls.backward = false;
            return;
        }

        // Get movement input vector from player or use cached
        if (this.player && this.player.inputInfo) {
            this.inputVector = this.player.inputInfo.getMovementVector();
        } else {
            this.inputVector = this.lastPlayerState.inputVector;
        }
        
        if (!this.inputVector) {
            this.controls.forward = false;
            this.controls.backward = false;
            return;
        }

        // Map input vector to control states
        this.controls.forward = this.inputVector.y > 0.1;
        this.controls.backward = this.inputVector.y < -0.1;
    }

    updateRotationCache() {
        const currentTick = this.tickCounter;
        if (this.lastRotationUpdate !== 0 && currentTick - this.lastRotationUpdate < 1) return;

        this.lastRotationUpdate = currentTick;
        
        // Use cached values if player is not available
        const playerRot = this.player ? this.player.getRotation() : this.lastPlayerRotation;
        const vehicleRot = this.vehicle.getRotation();
        
        this.cachedRotations = {
            player: playerRot,
            vehicle: vehicleRot
        };
        
        // Update cache
        this.lastPlayerRotation = playerRot;
        this.lastVehicleRotation = vehicleRot;
    }

    updatePlaneProperties() {
        
        const previousTrick = this.trick; // Store previous trick state
        
        this.state = this.vehicle.getProperty('bw_vv:plane_state');
        this.trick = this.vehicle.getProperty('bw_vv:trick');
        this.boost = this.vehicle.getProperty('bw_vv:boost');
        
        // Trigger performTrick when trick switches to 'active'
        if (this.trick === 'active' && previousTrick !== 'active') {
            this.performTrick()
        }
        // Determine maxSpeed based on state and surface
        if (this.state === 'flying') {
            this.maxSpeed = this.totalSpeed;
        } else if (this.state === 'takeoff' || this.state === 'stationary') {
            const blockType = isBlockUnderneath(this.vehicle);
            const isIce = ['minecraft:ice', 'minecraft:packed_ice', 'minecraft:blue_ice'].includes(blockType);
            this.maxSpeed = isIce ? 0.03 * this.totalSpeed : 0.3 * this.totalSpeed;
        }

        if (this.boost === 'active') {
            this.maxSpeed *= this.boostSpeedMultiplier;
        }

        this.groundOffset = this.vehicle.isOnGround ? 1 : 0.2;

        // Calculate navigation direction
        const { player = { y: 0 }, vehicle = { y: 0 } } = this.cachedRotations ?? {};

        // Calculate yaw change safely
        let yawChange = ((vehicle.y - player.y) + 360) % 360;
        if (yawChange > 180) yawChange -= 360;

        this.navigation = yawChange > 1 ? "left" : yawChange < -1 ? "right" : null;
    }

    _syncYViewWindow() {
        const limit = Math.max(1, Math.floor(this.smoothingFactorVertical));
        const lists = [this.histories?.yView, this.histories?.yView_anim];

        for (const arr of lists) {
            if (Array.isArray(arr) && arr.length > limit) {
                arr.splice(0, arr.length - limit); // drop oldest so the new window applies immediately
            }
        }
    }

    updateSmoothingFactor() {
        const conditionMet = (this.disableFlightCooldown <= 0 && (this.viewDir.y < -0.8 || this.viewDir.y > 0.9));

        if (!conditionMet && this.wasConditionMet && this.smoothingFactorTimeout === null) {
            this.smoothingFactorTimeout = PLANE_CONFIG.SMOOTHING_VERTICAL_TAILOFF;
        }

        if (this.smoothingFactorTimeout !== null) {
            this.smoothingFactorTimeout--;
            if (this.smoothingFactorTimeout <= 0) {
                this.smoothingFactorVertical = conditionMet ? PLANE_CONFIG.SMOOTHING_VERTICAL_ON : PLANE_CONFIG.SMOOTHING_VERTICAL_OFF;
                this.smoothingFactorTimeout = null;
                this._syncYViewWindow()
            }
        } else if (conditionMet) {
            this.smoothingFactorVertical = PLANE_CONFIG.SMOOTHING_VERTICAL_ON;
            this._syncYViewWindow()
        }

        this.wasConditionMet = conditionMet;
    }

    calculateSmoothValues() {
        // Fallback to avoid undefined
        const playerRot = this.cachedRotations.player ?? { x: 0, y: 0, z: 0 };
        const vehicleRot = this.cachedRotations.vehicle ?? { x: 0, y: 0, z: 0 };
        // Determine view direction based on state
        const quickerViewDir = this.state === 'takeoff' ?
            this.cachedRotations.player.y : this.cachedRotations.vehicle.y;

        const quickView = this.calculateQuickView(quickerViewDir);

        // Smooth values
        const conditionalViewDir_anim = (this.vehicle.isOnGround || this.vehicle.isInWater)
            ? 0.1
            : planeAnimCorrector(this.viewDir.y); //determines how quickly the plane moves downward when the player looks down
        const conditionalViewDir = (this.vehicle.isOnGround || this.vehicle.isInWater)
            ? 0.1
            : this.viewDir.y; //determines how quickly the plane moves downward when the player looks down

        this.smooth_y_viewDir = smoothValue(conditionalViewDir, this.histories.yView, this.smoothingFactorVertical);
        this.smooth_y_viewDir_animation = smoothValue(conditionalViewDir_anim, this.histories.yView_anim, this.smoothingFactorVertical);
        this.smooth_x_viewDir = smoothValue(this.viewDir.x, this.histories.xView, this.smoothingFactorHorizontal);
        this.smooth_z_viewDir = smoothValue(this.viewDir.z, this.histories.zView, this.smoothingFactorHorizontal);
        this.quicker_smooth_x_Rot = smoothValue(quickView.x, this.histories.quickX, this.smoothingFactorHorizontal_quicker);
        this.quicker_smooth_z_Rot = smoothValue(quickView.z, this.histories.quickZ, this.smoothingFactorHorizontal_quicker);
        this.quicker_smooth_z_Rot_degrees = smoothAngle(quickerViewDir, this.histories.quickDegrees, this.smoothingFactorHorizontal_quicker);

        // Update vehicle plane angle
        this.vehicle.setProperty('bw_vv:plane_angle', -this.smooth_y_viewDir * 0.9);
    }

    handleStateLogic() {
        // Fuel check
        if (this.fuel <= 0) {
            if (this.state === 'takeoff') {
                this.enterStationaryMode();
                this.speed = 0;
            } else if (this.state === 'flying') {
                this.enterCrashMode();
                this.speed = 0;
            }
        }

        switch (this.state) {
            case 'takeoff':
                // Continue takeoff if forward input is held, otherwise go to stationary
                if (this.controls.forward) {
                    this.speed = Math.min(this.speed + this.acceleration, this.maxSpeed);
                    this.applyImpulse();
                    this.vehicle.setRotation({ x: 0, y: this.quicker_smooth_z_Rot_degrees });

                    if (--this.takeoffTime <= 0) {
                        if (this.horizontalSpeed * 1.2 >= this.maxSpeed) {
                            this.enterFlightMode();
                        } else {
                            this.enterStationaryMode();
                        }
                    }
                } else {
                    // No forward input during takeoff - go to stationary
                    this.enterStationaryMode();
                }
                break;

            case 'stationary':
                // Start takeoff if forward input is pressed
                if (this.controls.forward && this.fuel > 0) {
                    this.enterTakeoffMode();
                } else {
                    this.speed = Math.max(0, this.speed - this.acceleration);
                    this.applyImpulse();
                    this.vehicle.setRotation({ x: 0, y: this.quicker_smooth_z_Rot_degrees });
                }
                break;

                case 'flying':
                    // Flying mode doesn't require input to maintain flight
                    this.disableFlightCooldown--;

                    if (this.disableFlightCooldown <= 0) {
                        if (this.vehicle.isOnGround || this.vehicle.isInWater) {
                            this.enterStationaryMode();
                            break;
                        }
                        
                        // Improved crash detection
                        if (this.checkCrashCondition()) {
                            this.remove();
                            break;
                        }
                    }

                    this.handleFlightControls();
                    break;

            case 'crashing':
                if (this.level === 1 && this.vehicle.isOnGround) {
                    this.remove();
                } else if (this.vehicle.isInWater) {
                    this.enterStationaryMode();
                } else if (this.level > 1 && this.vehicle.isOnGround) {
                    this.enterStationaryMode();
                }
                break;
        }
    }

    applyImpulse() {
        if (!this.vehicle.isOnGround || this.speed > 0.01) {
            this.vehicle.applyImpulse({
                x: this.quicker_smooth_x_Rot * this.speed * this.groundOffset,
                y: 0,
                z: this.quicker_smooth_z_Rot * this.speed * this.groundOffset
            });
        }
    }

    checkCrashCondition() {
        const totalSpeed = Math.hypot(this.velocity.x, this.velocity.y, this.velocity.z);
        const speedThreshold = this.maxSpeed * 0.2; // 20% of max speed
        
        // Don't crash during intentional vertical movement (climbing or diving)
        const isIntentionalVertical = Math.abs(this.smooth_y_viewDir) > 0.7 && 
                                    Math.abs(this.velocity.y) > this.maxSpeed * 0.3;
        
        // Don't crash during tricks (rapid directional changes)
        const isDuringTrick = this.trickCooldown > 0 || this.trick === 'active' || this.trick === 'cooldown';
        
        // Detect severe deceleration (impact)
        const deceleration = this.lastTotalSpeed - totalSpeed;
        const severeDeceleration = deceleration > this.maxSpeed * 0.2; // Lost 20% speed in one tick
        
        // Check if plane is stalling (low speed while not climbing/diving intentionally)
        const isStalling = totalSpeed < speedThreshold && !isIntentionalVertical;
        
        if (isStalling && !isDuringTrick) {
            this.stallFrames++;
            // Require sustained stall for 10 ticks (0.5 seconds) to confirm crash
            if (this.stallFrames > 3) {
                return true;
            }
        } else {
            this.stallFrames = 0;
        }
        
        // Immediate crash on severe impact
        if (severeDeceleration && !isDuringTrick && !isIntentionalVertical) {
            this.crashDetectionFrames++;
            // Require 3 consecutive frames of severe deceleration
            if (this.crashDetectionFrames >= 3) {
                return true;
            }
        } else {
            this.crashDetectionFrames = 0;
        }
        
        // Update tracking variables
        this.lastHorizontalSpeed = this.horizontalSpeed;
        this.lastTotalSpeed = totalSpeed;
        
        return false;
    }

    handleFlightControls() {
        const pitchFactor = Math.abs(this.smooth_y_viewDir) * this.verticalFactor;
        const yaw = Math.atan2(this.smooth_x_viewDir, this.smooth_z_viewDir) * (180 / Math.PI);
        
        // Check for extreme values that might indicate a crash condition
        const isExtremeValue = Math.abs(this.smooth_y_viewDir_animation) > 1;
        
        let pitch;
        if (isExtremeValue) {
            // Use a fallback pitch calculation to avoid NaN
            pitch = this.smooth_y_viewDir_animation > 1 ? -90 : 90; // Max pitch up/down
        } else {
            pitch = -Math.asin(this.smooth_y_viewDir_animation) * (180 / Math.PI);
        }

        this.vehicle.setRotation({ x: pitch * 1.2, y: -yaw });//visual

        const targetSpeed = this.maxSpeed * (1 - pitchFactor);
        this.targetVerticalSpeed = this.maxSpeed * this.smooth_y_viewDir * this.verticalSpeedFactor;

        this.vehicle.applyKnockback(
            { 
                x: this.smooth_x_viewDir * targetSpeed, 
                z: this.smooth_z_viewDir * targetSpeed 
            }, 
            this.targetVerticalSpeed
        );//behvavior that actually flies the Plane
    }

    performTrick() {
        if (this.trickCooldown > 0) return;
        if (!this.player) return; // Can't perform tricks without active player
        const animations_plane = {
            right: "animation.bw_vv.plane_trick_right",
            left: "animation.bw_vv.plane_trick_left"
        };
        const animations_player = {
            right: "animation.bw_vv.player_drive_plane_roll_right",
            left: "animation.bw_vv.player_drive_plane_roll_left"
        }
                system.runTimeout(() => {
            if (!this.removed && this.vehicle.isValid) {
                this.vehicle.setProperty('bw_vv:trick', 'ready');
            }
        }, 25); // Same as trickCooldown
        const verticalFactorKnockback = (1-this.targetVerticalSpeed*1.2)*0.4;
        const knockbacks = {
            right: [-this.smooth_z_viewDir*verticalFactorKnockback, this.smooth_x_viewDir*verticalFactorKnockback],
            left: [this.smooth_z_viewDir*verticalFactorKnockback, -this.smooth_x_viewDir*verticalFactorKnockback]
        };

        const dir = this.navigation;
        const anim_plane = animations_plane[dir];
        const anim_player = animations_player[dir];
        
        this.vehicle.setProperty('bw_vv:trick','cooldown')
        if (!dir || !anim_plane) return;

        this.vehicle.playAnimation(anim_plane);
        this.player.playAnimation(anim_player, { controller: "climb_animation_controller", blendOutTime: 0.2 });
        let animationVariables = PLAYER_DRIVE_ANIMATION_VARIABLES;

        system.runTimeout(() => {
            if (this.vehicle.isValid && this.player.isValid) {
                this.player.playAnimation('animation.bw_vv.player_drive_plane',{stopExpression: '!q.is_riding', controller: "climb_animation_controller", blendOutTime: 0.2});
            }
        }, 20); // 1 second
        
        // Clear any previous camera timeout
        if (this.cameraClearTimeout !== null) {
            system.clearRun(this.cameraClearTimeout);
            this.cameraClearTimeout = null;
        }
        
        this.player.camera.setCamera("minecraft:third_person", {
            easeOptions: {
                easeType: "InOutSine",
                easeTime: 1.0 // 1 second transition
            }
        });

        // Apply continuous knockback for ~1s (20 ticks)
        const [kx, kz] = knockbacks[dir];   // lock direction at trick start
        const DURATION_TICKS = 15;
        let ticks = 0;

        const intervalId = system.runInterval(() => {
            // Stop if duration elapsed or state no longer makes sense
            if (
                ticks++ >= DURATION_TICKS ||
                this.removed ||
                this.vehicle.isOnGround ||
                !this.getRider() ||
                this.state !== 'flying'
            ) {
                system.clearRun(intervalId);
                return;
            }

            this.vehicle.applyImpulse({x: kx, y: 0, z: kz});
        });

        // Gate re-use roughly for the same duration
        this.trickCooldown = 25;
        // Clear camera after trick cooldown + 2 seconds (30 ticks + 40 ticks = 70 ticks total)
        this.cameraClearTimeout = system.runTimeout(() => {
            if (this.player && !this.removed) {
                this.player.camera.clear();
            }
            this.cameraClearTimeout = null;
        }, 60); // 30 (trickCooldown) + 40 (2 seconds)
    }

    // State transition methods
    enterFlightMode() {
        this.vehicle.setProperty('bw_vv:plane_state', "flying");
        this.vehicle.triggerEvent('event:enter_flight_mode');
        this.disableFlightCooldown = PLANE_CONFIG.DISABLE_FLIGHT_COOLDOWN;
        //this.player.onScreenDisplay.setActionBar("§aFlight Mode");
    }

    enterTakeoffMode() {
        this.vehicle.setProperty('bw_vv:plane_state', "takeoff");
        this.vehicle.triggerEvent('event:enter_ground_mode');
        this.takeoffTime = PLANE_CONFIG.TAKEOFF_TIME;
        this.speed = 0.03;
        //this.player.onScreenDisplay.setActionBar("§aTakeoff!");
    }

    enterCrashMode() {
        this.vehicle.setProperty('bw_vv:plane_state', "crashing");
        this.vehicle.triggerEvent('event:enter_ground_mode');
        //this.player.onScreenDisplay.setActionBar("§cCrashing!");
    }

    enterStationaryMode() {
        this.vehicle.setProperty('bw_vv:plane_state', "stationary");
        this.vehicle.triggerEvent('event:enter_ground_mode');
        //this.player.onScreenDisplay.setActionBar("§fStationary.");
    }

    updateActionBar() {
        //actionbar
        if (!this.player) return; // Don't try to update actionbar without player
        const inputStatus = this.controls.forward ? "FORWARD" : this.controls.backward ? "BACKWARD" : "NONE";
        this.player.onScreenDisplay.setActionBar(
            `state: ${this.state} | input: ${inputStatus} | trick: ${this.trick} | speed: ${this.speed.toFixed(2)} | takeoff time: ${this.takeoffTime.toFixed(2)}`
        );
    }

    remove() {
        this.removed = true;  // Immediately blockz further attempts
        this.vehicle.dimension.createExplosion(
            this.vehicle.location,
            1,  // radius
            {
                breaksBlocks: false,
                causesFire: false
            }
        );
        if (this.player) {
            this.player.camera.clear();
        }
         // Eject any riders before removal
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