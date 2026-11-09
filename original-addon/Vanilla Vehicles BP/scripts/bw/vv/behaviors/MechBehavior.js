import { GameMode, system, TicksPerSecond, EffectTypes } from "@minecraft/server";
import { BaseBehavior } from "./BaseBehavior";
import { MECH_CONFIG } from "../config/constants";
import { mathClamp } from "../utils/math";

export class MechBehavior extends BaseBehavior {
    constructor(vehicle, player) {
        super(vehicle, player);
        
        this.vehicle = vehicle;
        this.player = player;
        
        // Cached calculations
        this.cachedRotations = {};
        this.lastRotationUpdate = 0;

        // Cache player data
        this.lastPlayerRotation = { x: 0, y: 0 };
        this.lastVehicleRotation = { x: 0, y: 0 };

        // Mech-specific properties from config
        this.maxSpeed = MECH_CONFIG.MAX_SPEED;
        this.acceleration = MECH_CONFIG.ACCELERATION;
        this.boostImpulseSustain = MECH_CONFIG.BOOST_IMPULSE_SUSTAIN || 0.3;
        this.boostImpulseStrong = MECH_CONFIG.BOOST_IMPULSE_STRONG
        this.projectileCooldown = 1 * TicksPerSecond; // 3 seconds
        
        // State properties
        this.previousBoostStatus = 'ready';
        
        // Boost system
        this.boostScheduleId = null;
        
        // Projectile system
        this.projectilePosition = 'center'; // Cycles: center -> left -> right -> center
        this.projectileCooldownTicks = 0;
        this.projectileFireScheduleId = null;
        this.previousWeaponsState = false;
        this.previousWeaponsFireTick = -1;
        
        // Mining system
        this.miningActive = false;
        this.miningStopScheduleId = null;
        this.weapons = null;
        this.lastMiningHitTick = 0;
        this.miningToolSpeeds = {
            'wood': 1.0,
            'stone': 1.5,
            'copper': 2.0,
            'iron': 2.5,
            'diamond': 3.5,
            'netherite': 5.0
        };
        
        // Controls
        this.controls = {
            jumping: false,
            hitting: false
        };
        this.powerJumpActive = false;
        this.powerJumpStartTick = 0;
        this.powerJumpMinDuration = 0.4 * TicksPerSecond; // 1 second
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


    handleBoostStatus() {
        const currentBoostStatus = this.vehicle.getProperty('bw_vv:boost');
        
        // Detect transition from non-active to active
        if (this.previousBoostStatus !== 'active' && currentBoostStatus === 'active') {
            this.startBoostEffects();
        }
        
        // Detect transition from active to cooldown
        if (this.previousBoostStatus === 'active' && currentBoostStatus === 'cooldown') {
            this.stopBoostEffects();
        }
        
        this.previousBoostStatus = currentBoostStatus;
    }

    startBoostEffects() {
        // Add Feather Falling effect to player
        try {
            this.vehicle.addEffect(EffectTypes.get('slow_falling'), 999999, {
                amplifier: 1,
                showParticles: true
            });
            const viewDir = this.vehicle.getViewDirection();
            const strongImpulse = {
                x: viewDir.x * this.boostImpulseStrong,
                y: 0,
                z: viewDir.z * this.boostImpulseStrong
            };
            this.vehicle.applyImpulse(strongImpulse);
        } catch (error) {
            // Handle error silently
        }
        
        // Schedule sustained impulse
        this.boostScheduleId = system.runInterval(() => {
            const boostStatus = this.vehicle.getProperty('bw_vv:boost');
            
            // Stop if boost is no longer active
            if (boostStatus !== 'active') {
                this.stopBoostEffects();
                return;
            }
            
            const viewDir = this.vehicle.getViewDirection();
            const sustainImpulse = {
                x: viewDir.x * this.boostImpulseSustain,
                y: 0,
                z: viewDir.z * this.boostImpulseSustain
            };
            
            try {
                this.vehicle.applyImpulse(sustainImpulse);
            } catch (error) {
                this.stopBoostEffects();
            }
        }, 1);
    }

    stopBoostEffects() {
        if (this.boostScheduleId !== null) {
            system.clearRun(this.boostScheduleId);
            this.boostScheduleId = null;
        }
        
        // Remove Feather Falling effect from player
        try {
            this.vehicle.removeEffect(EffectTypes.get('slow_falling'));
        } catch (error) {
            // Handle error silently
        }
    }

    update() {
        if (this.removed || !this.updateBaseProperties()) return;
        this.weapons = this.vehicle.getProperty('bw_vv:weapons');
        const rider = this.getRider();
        if (!rider) return;
        
        // Update controls
        this.updateControls();
        
        // Handle boost status changes
        this.handleBoostStatus();
        
        this.handlePowerJumpLand();
        
        // Handle hitting (projectile or mining)
        if (this.controls.hitting && rider) {
            this.handleHit();
        }
        
        //this.feedback();
    }

    updateControls() {
        const rider = this.getRider();
        if (!rider) {
            this.controls.jumping = false;
            this.controls.hitting = false;
            return;
        }

        this.controls.jumping = this.player.isJumping;
        
        // Detect hitting - you may need to use a different method depending on your setup
        // This assumes you have a way to detect when player attacks while riding
        const currentTick = system.currentTick;
        const weapons = this.vehicle.getProperty('bw_vv:weapons') === 'active';

        // Detect rising edge: false → true
        if (!this.previousWeaponsState && weapons) {
            this.controls.hitting = true;
            this.lastHitTick = currentTick;
        } else {
            this.controls.hitting = false;
        }
        this.previousWeaponsState = weapons;
    }

    updateAirborneStatus() {
        try {
            if (!this.vehicle.isOnGround && !this.vehicle.isInWater) {
                this.airborne = true;
            } else {
                this.airborne = false;
            }
        } catch (error) {
            this.removed = true;
        }
    }

    handlePowerJumpLand() {
        try {
            const velocity = this.vehicle.getVelocity();
            const currentTick = system.currentTick;
            
            // Check if mech is in a power jump (airborne with upward velocity > 0.1)
            if (!this.vehicle.isOnGround && velocity.y > 0.01) {
                if (!this.powerJumpActive) {
                    // Start tracking power jump
                    this.powerJumpActive = true;
                    this.powerJumpStartTick = currentTick;
                }
            }
            
            // Check for landing after a sufficient power jump
            if (this.powerJumpActive && this.vehicle.isOnGround) {
                const jumpDuration = currentTick - this.powerJumpStartTick;
                
                if (jumpDuration >= this.powerJumpMinDuration) {
                    // Trigger power landing effects
                    this.performPowerLand();
                }
                
                // Reset power jump state
                this.powerJumpActive = false;
                this.powerJumpStartTick = 0;
            }
        } catch (error) {
            // Handle error silently
        }
    }

    performPowerLand() {
        try {
            const mechLocation = this.vehicle.location;
            const radius = 4;
            
            // Set power_land property to true
            this.vehicle.setProperty('bw_vv:power_land', true);
            
            // Get all entities in range
            const nearbyEntities = this.vehicle.dimension.getEntities({
                location: mechLocation,
                maxDistance: radius,
                excludeTypes: ['minecraft:item'] // Don't affect dropped items
            });
            
            // Damage and knockback each mob
            for (const entity of nearbyEntities) {
                // Skip self and rider
                if (entity.id === this.vehicle.id || entity.id === this.player.id) continue;
                
                // Calculate knockback direction (away from mech)
                const dx = entity.location.x - mechLocation.x;
                const dz = entity.location.z - mechLocation.z;
                const distance = Math.sqrt(dx * dx + dz * dz);
                
                // Normalize and apply knockback
                if (distance > 0) {
                    const knockbackStrength = 2;
                    const knockback = {
                        x: (dx / distance) * knockbackStrength,
                        y: 0.5, // Upward component
                        z: (dz / distance) * knockbackStrength
                    };
                    
                    try {
                        entity.applyKnockback(dx, dz, knockbackStrength, 0.5);
                    } catch (e) {
                        // Fallback to applyImpulse if applyKnockback fails
                        entity.applyImpulse(knockback);
                    }
                }
                
                // Apply damage
                try {
                    entity.applyDamage(15, {
                        cause: 'entityAttack',
                        damagingEntity: this.vehicle
                    });
                } catch (e) {
                    // Handle error silently
                }
            }
            
            // Reset power_land property after 1 second
            system.runTimeout(() => {
                try {
                    this.vehicle.setProperty('bw_vv:power_land', false);
                } catch (e) {
                    // Handle error silently
                }
            }, 1 * TicksPerSecond);
            
        } catch (error) {
            // Handle error silently
        }
    }

    handleHit() {
        const spikerType = this.vehicle.getProperty('bw_vv:spiker');
        
        if (spikerType && spikerType !== 'empty') {
            // Mining mode
            this.handleMining(spikerType);
        } else {
            // Projectile mode
            this.handleProjectile();
        }
    }

    handleProjectile() {

    const currentTick = system.currentTick;

    // Prevent firing more than once in the same tick
    if (currentTick === this.previousWeaponsFireTick) return;

        // Cancel any pending fire schedule
        if (this.projectileFireScheduleId !== null) {
            system.clearRun(this.projectileFireScheduleId);
        }
        
        // Schedule projectile fire after 2 seconds
        this.projectileFireScheduleId = system.runTimeout(() => {
            this.fireProjectile();
            this.projectileFireScheduleId = null;
        }, 1 * TicksPerSecond);
          this.previousWeaponsFireTick = currentTick;
    }

    fireProjectile() {
        
        try {
            // Get spawn location (slightly in front of player's head)
            const playerHead = this.player && this.player.inputInfo ? this.player.getHeadLocation() : null;
            const viewDirection = this.player ? this.player.getViewDirection() : { x: 0, y: 0, z: 0 };
            
            // Create horizontal-only view direction (ignore vertical look)
            const horizontalViewDirection = {
                x: viewDirection.x,
                y: 0,
                z: viewDirection.z
            };
            
            // Normalize the horizontal view direction
            const horizontalMagnitude = Math.sqrt(horizontalViewDirection.x ** 2 + horizontalViewDirection.z ** 2);
            const normalizedHorizontal = {
                x: horizontalViewDirection.x / horizontalMagnitude,
                y: 0,
                z: horizontalViewDirection.z / horizontalMagnitude
            };
            
            // Calculate right vector (perpendicular to horizontal view direction)
            const rightVector = {
                x: normalizedHorizontal.z,
                y: 0,
                z: -normalizedHorizontal.x
            };
            
            // Offset spawn position: 1.6 block forward (horizontal only) and 1.5 blocks to the right
            const spawnLocation = {
                x: playerHead.x + normalizedHorizontal.x * 2 - rightVector.x * 1.3,
                y: playerHead.y - 0.9,
                z: playerHead.z + normalizedHorizontal.z * 2 - rightVector.z * 1.3
            };
            
            const shootViewOffset_horizontal = 0.1;
            const shootViewOffset_vertical = 0.35;
            const clampedViewDir = {
                x: mathClamp(viewDirection.x,-80,90)*2 + rightVector.x*shootViewOffset_horizontal,
                y: viewDirection.y*2 + shootViewOffset_vertical,
                z: mathClamp(viewDirection.z,-20,70)*2 +  rightVector.z*shootViewOffset_horizontal
            };

            // Spawn projectile entity
            const projectile = this.vehicle.dimension.spawnEntity(
                'bw_vv:mech_projectile',
                spawnLocation
            );
            
            // Set the projectile's shooter (for proper collision detection)
            const projectileComp = projectile.getComponent('minecraft:projectile');
            if (projectileComp) {
                // The projectile component will handle the velocity based on shooter's view direction
                projectileComp.shoot(clampedViewDir, {
                    uncertainty: 0, // Perfect accuracy, adjust if needed,
                    airInertia: 1.0,
                    gravity: 0.0001,
                    owner: this.vehicle
                });
                
            }
            
            // Start cooldown
            
        } catch (error) {
            // Handle error silently
        }
    }

    handleMining(spikerType) {
        const currentTick = system.currentTick;
        this.lastMiningHitTick = currentTick;
        
        if (!this.miningActive) {
            this.miningActive = true;
            this.vehicle.setProperty('bw_vv:mining_status', true);
            this.startMining(spikerType);
        }
        
        // Cancel any pending stop schedule
        if (this.miningStopScheduleId !== null) {
            system.clearRun(this.miningStopScheduleId);
        }
        
        // Schedule mining stop after 0.5 seconds of no hits
        this.miningStopScheduleId = system.runTimeout(() => {
            this.stopMining();
        }, 0.5 * TicksPerSecond);
    }

    startMining(spikerType) {
        const miningSpeed = this.miningToolSpeeds[spikerType] || 1.0;
        const ticksPerBreak = Math.max(1, Math.floor(TicksPerSecond / miningSpeed));
        
        this.miningScheduleId = system.runInterval(() => {
            if (!this.miningActive) {
                system.clearRun(this.miningScheduleId);
                return;
            }
            
            this.mineBlocksInRadius();
        }, ticksPerBreak);
    }

    mineBlocksInRadius() {
        try {
            const mechLocation = this.vehicle.location;
            const viewDirection = this.player ? this.player.getViewDirection() : { x: 0, y: 0, z: 0 };
            
            // Determine forward direction (simplified to cardinal directions)
            let offsetX = 0, offsetZ = 0;
            if (Math.abs(viewDirection.x) > Math.abs(viewDirection.z)) {
                offsetX = viewDirection.x > 0 ? 2 : -2;
            } else {
                offsetZ = viewDirection.z > 0 ? 2 : -2;
            }
            
            // Mine 3x3 area in front of mech (from ground to 1 block above)
            for (let x = -1; x <= 1; x++) {
                for (let y = 0; y <= 1; y++) { // Ground level to 1 block above
                    for (let z = -1; z <= 1; z++) {
                        const blockLocation = {
                            x: Math.floor(mechLocation.x + offsetX + x),
                            y: Math.floor(mechLocation.y + y),
                            z: Math.floor(mechLocation.z + offsetZ + z)
                        };
                        
                        const block = this.vehicle.dimension.getBlock(blockLocation);
                        if (block && !block.isAir && block.isValid()) {
                            // Break the block
                            block.setType('minecraft:air');
                        }
                    }
                }
            }
        } catch (error) {
            // Handle error silently
        }
    }

    stopMining() {
        this.miningActive = false;
        this.vehicle.setProperty('bw_vv:mining_status', false);
        
        if (this.miningScheduleId !== null) {
            system.clearRun(this.miningScheduleId);
            this.miningScheduleId = null;
        }
        
        this.miningStopScheduleId = null;
    }

    feedback() {
        const boostStatus = this.vehicle.getProperty('bw_vv:boost');
        const spikerType = this.vehicle.getProperty('bw_vv:spiker');
        
        let statusText = `Boost: ${boostStatus}`;
        
        if (spikerType && spikerType !== 'empty') {
            statusText += ` | Weapons: ${this.weapons}`;
        } else {
            statusText += ` | Projectile: ${this.vehicle.getProperty('bw_vv:weapons')}`;
        }
        
        if (this.player) {
            this.player.onScreenDisplay.setActionBar(statusText);
        }
    }

    remove() {
        this.removed = true;
        
        // Clear all scheduled tasks
        if (this.boostScheduleId !== null) {
            system.clearRun(this.boostScheduleId);
        }
        if (this.projectileFireScheduleId !== null) {
            system.clearRun(this.projectileFireScheduleId);
        }
        if (this.miningScheduleId !== null) {
            system.clearRun(this.miningScheduleId);
        }
        if (this.miningStopScheduleId !== null) {
            system.clearRun(this.miningStopScheduleId);
        }
        
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