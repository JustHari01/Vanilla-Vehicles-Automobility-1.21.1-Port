import { system } from "@minecraft/server";
import { BaseBehavior } from "./BaseBehavior";
import { VehicleRegistry } from "../core/vehicleRegistry";
import { smoothValue, smoothAngle } from "../utils/math";
import { STEAMBOAT_CONFIG } from "../config/constants";

export class SteamBoatBehavior extends BaseBehavior {
    constructor(vehicle, player) {
        super(vehicle, player);
        
        this.vehicle = vehicle;
        this.player = player;

        // Cache player data
        this.lastPlayerRotation = { x: 0, y: 0 };
        this.lastVehicleRotation = { x: 0, y: 0 };

        // SteamBoat-specific speed and movement properties from config
        this.baseSpeed = STEAMBOAT_CONFIG.BASE_SPEED; // Base movement speed
        this.maxSpeed = STEAMBOAT_CONFIG.MAX_SPEED; // Maximum achievable speed
        this.acceleration = STEAMBOAT_CONFIG.ACCELERATION; // How quickly boat accelerates
        this.deceleration = STEAMBOAT_CONFIG.DECELERATION; // How quickly boat slows down
        this.turnSpeed = STEAMBOAT_CONFIG.TURN_SPEED; // How fast the boat turns
        this.inputVector = null;
        
    let initialRotation = 0;
    try {
        const vehicleRotation = this.vehicle.getRotation();
        initialRotation = vehicleRotation.y;
    } catch (error) {
        console.warn("Failed to get vehicle rotation:", error);
    }
    
    // Current movement state
    this.currentSpeed = 0;
    this.targetSpeed = 0;
    this.currentRotation = initialRotation;  // Use vehicle's actual current rotation
    this.targetRotation = initialRotation; 
        
        // Control states - track WASD input with dynamic intensity
        this.controls = {
            forward: false,        // W key state
            backward: false,       // S key state
            left: false,           // A key state (boolean for property)
            right: false,          // D key state (boolean for property)
            leftIntensity: 0,      // A key intensity (0-1 for dynamic turning)
            rightIntensity: 0      // D key intensity (0-1 for dynamic turning)
        };
        
        // Movement direction for property updates
        this.travelDirection = 'center'; // Current travel direction (center, left, right)
        
        // Smoothing factors from config for smooth movement
        this.speedSmoothingFactor = STEAMBOAT_CONFIG.SPEED_SMOOTHING_FACTOR;
        this.rotationSmoothingFactor = STEAMBOAT_CONFIG.ROTATION_SMOOTHING_FACTOR;
        
        // History arrays for smoothing calculations
        this.histories = {
            ...this.histories,
            speed: [],          // Speed history for smoothing
            rotation: []        // Rotation history for smoothing
        };
        
        // Tick counter for internal timing
        this.tickCounter = 0;
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
        
        const rider = this.getRider();
        
        // Early exit if no rider and boat is stationary
        if (!rider && this.currentSpeed === 0) return;
        
        // Stop boat if no rider
        if (!rider) {
            this.targetSpeed = 0;
            this.controls = { 
                forward: false, 
                backward: false, 
                left: false, 
                right: false, 
                leftIntensity: 0, 
                rightIntensity: 0 
            };
        }
        
        this.updateControls();
        this.updateMovement();
        this.updateRotation();
        //this.updateProperties();
        this.applyMovement();
        this.trimHistories();
        //this.updateActionBar();
    }

    // Process WASD input from player
    updateControls() {
        const rider = this.getRider();
        if (!rider) return;

        // Get movement input vector from player
        this.inputVector = rider.inputInfo.getMovementVector();
        if (!this.inputVector) return;

        // Map input vector to control states with raw values for dynamic turning
        this.controls.backward = this.inputVector.y < -0.1;
        this.controls.forward = this.inputVector.y > 0.1;
        this.controls.leftIntensity = Math.max(0, this.inputVector.x); // 0 to 1 for left intensity
        this.controls.rightIntensity = Math.max(0, -this.inputVector.x);  // 0 to 1 for right intensity
        
        // Keep boolean states for property updates
        this.controls.left = this.controls.leftIntensity > 0.1;
        this.controls.right = this.controls.rightIntensity > 0.1;

        // Calculate target speed based on forward/backward input - only if fuel available
        if (this.fuel <= 0) {
            // No fuel - force stop
            this.targetSpeed = 0;
        } else {
            // Fuel available - process input normally
            if (this.controls.forward) {
                this.targetSpeed = this.baseSpeed;
            } else if (this.controls.backward) {
                this.targetSpeed = -this.baseSpeed * 0.6; // Slower reverse speed
            } else {
                this.targetSpeed = 0;
            }
        }

        // Update travel direction based on left/right input (test each direction separately)
        // Note: Turning is still allowed without fuel (momentum/drift turning)
        if (this.controls.left && !this.controls.right) {
            this.travelDirection = 'left';
        } else if (this.controls.right && !this.controls.left) {
            this.travelDirection = 'right';
        } else {
            this.travelDirection = 'center';
        }
    }

    // Update boat speed with acceleration/deceleration
    updateMovement() {
        // Apply acceleration or deceleration
        if (Math.abs(this.targetSpeed - this.currentSpeed) > 0.001) {
            const speedDifference = this.targetSpeed - this.currentSpeed;
            const acceleration = speedDifference > 0 ? this.acceleration : this.deceleration;
            
            this.currentSpeed += Math.sign(speedDifference) * Math.min(Math.abs(speedDifference), acceleration);
        }
        
        // Clamp speed to maximum limits
        this.currentSpeed = Math.max(-this.maxSpeed * 0.6, Math.min(this.maxSpeed, this.currentSpeed));
        
        // Smooth the speed for fluid movement
        this.currentSpeed = smoothValue(this.currentSpeed, this.histories.speed, this.speedSmoothingFactor);
    }

    // Update boat rotation based on left/right input with dynamic intensity
    updateRotation() {
        // Allow turning even without fuel (for momentum-based steering)
        // but reduce effectiveness when no fuel and low speed
const speedFactor = Math.abs(this.currentSpeed) / this.maxSpeed;
const baseTurnRate = this.turnSpeed * speedFactor;
const steeringMultiplier = this.inputVector.y < 0 ? -1 : 1;
        // Apply rotation based on input intensity and current speed
        if (this.controls.leftIntensity > 0) {
            this.targetRotation -= baseTurnRate * this.controls.leftIntensity * steeringMultiplier;
        }
        if (this.controls.rightIntensity > 0) {
            this.targetRotation += baseTurnRate * this.controls.rightIntensity * steeringMultiplier;
        }
        
        // Smooth rotation for fluid turning
        this.currentRotation = smoothAngle(this.targetRotation, this.histories.rotation, this.rotationSmoothingFactor);
        
        // Apply rotation to vehicle
        this.vehicle.setRotation({ x: 0, y: this.currentRotation });
    }

    // Update vehicle properties for animation and display
    updateProperties() {
        // Update travel direction property for animations
        this.vehicle.setProperty('bw_vv:travel_direction', this.travelDirection);
    }

    // Apply calculated movement to the boat
    applyMovement() {
        if (Math.abs(this.currentSpeed) < 0.001) return;
        
        const speedModifier = !this.vehicle.isInWater ? 0.1 : 1;
        // Calculate movement direction based on boat rotation
        const radians = this.currentRotation * (Math.PI / 180);
        const moveX = -Math.sin(radians) * this.currentSpeed * speedModifier;
        const moveZ = Math.cos(radians) * this.currentSpeed * speedModifier;
        
        // Apply movement impulse
        this.vehicle.applyImpulse({
            x: moveX,
            y: 0,
            z: moveZ
        });
    }

    // Display current boat status on action bar
    updateActionBar() {
        if (!this.player) return;
        
        const fuelStatus = this.fuel <= 0 ? "§cEMPTY" : this.fuel.toFixed(1);
        const speedStatus = this.fuel <= 0 && this.currentSpeed < 0.01 ? "§cSTOPPED" : this.currentSpeed.toFixed(2);
        
        this.player.onScreenDisplay.setActionBar(
            `Speed: ${speedStatus} | ` +
            `Input: ${this.inputVector.y} | ` +
            `Fuel: ${fuelStatus}${this.fuel <= 0 ? " - REFUEL NEEDED" : ""}`
        );
    }

    // Handle jump input (could be used for special actions)
    onJump() {
        const rider = this.getRider();
        if (!rider) return;
        
        // Could implement steamboat-specific jump actions here
        // For example: horn sound, emergency stop, etc.
    }

    // Clean up when boat is removed
    remove() {
        if (this.removed) {
            console.warn("Prevented repeated remove()");
            return;
        }
        
        this.removed = true;
        
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