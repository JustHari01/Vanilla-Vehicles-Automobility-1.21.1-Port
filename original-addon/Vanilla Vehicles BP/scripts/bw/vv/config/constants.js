
//Global configuration and constants for vehicles.

//--- Configuration: General Physics Settings ---//
export const PHYSICS_CONFIG = {
    VELOCITY_SMOOTHING_FACTOR: 3.5,       // Number of values to average for vertical velocity smoothing
    POSITION_SMOOTHING_FACTOR: 2,         // Number of Y positions to average for position smoothing
    MAX_HISTORY_SIZE: 150,                // Max length of smoothing history buffers
    BOOST_FUEL_CONSUMPTION: 0.0023,         // Fuel units consumed per tick during boost
    BOOST_IMPULSE_MAGNITUDE: 0.15          // Forward impulse applied when boosting
};

//--- Configuration: Plane Behavior Settings ---//
export const PLANE_CONFIG = {
    TOTAL_SPEED: 1.1,                     // Maximum flight speed
    ACCELERATION: 0.007,                  // Forward acceleration per tick
    VERTICAL_SPEED_FACTOR: 0.6,             //How influenced the plane direction is by up/down movement
    VERTICAL_FACTOR: 0.8,                // Influence of vertical look direction on how far the plane stops moving horizontally when player looks up/down
    TAKEOFF_TIME: 40,                    // Duration of takeoff state before plane becomes stationary if player doesn't succeed in taking off (in ticks = 3.5s)
    DISABLE_FLIGHT_COOLDOWN: 25,          // Cooldown after takeoff before plane can switch to stationary mode. (also used to lower vertical look resposiveness immediately after takeoff)
    TRICK_COOLDOWN: 30,                   // Cooldown between aerial tricks
    JUMP_COOLDOWN: 10,                    // Ticks between valid jump presses
    SMOOTHING_FACTOR_HORIZONTAL: 30,      // Smoothing for stable turn direction (left/right view)
    SMOOTHING_FACTOR_HORIZONTAL_QUICKER: 15, // Smoothing for reactive steering
    SMOOTHING_VERTICAL_ON: 50,
    SMOOTHING_VERTICAL_OFF: 20,
    SMOOTHING_VERTICAL_TAILOFF: 25
};

export const PRISMER_CONFIG = {
    TOTAL_SPEED: 1.1,                     // Maximum flight speed
    ACCELERATION: 0.007,                  // Forward acceleration per tick
    VERTICAL_SPEED_FACTOR: 0.6,             //How influenced the plane direction is by up/down movement
    VERTICAL_FACTOR: 0.8,                // Influence of vertical look direction on how far the plane stops moving horizontally when player looks up/down
    TAKEOFF_TIME: 40,                    // Duration of takeoff state before plane becomes stationary if player doesn't succeed in taking off (in ticks = 3.5s)
    DISABLE_FLIGHT_COOLDOWN: 25,          // Cooldown after takeoff before plane can switch to stationary mode. (also used to lower vertical look resposiveness immediately after takeoff)
    TRICK_COOLDOWN: 30,                   // Cooldown between aerial tricks
    JUMP_COOLDOWN: 10,                    // Ticks between valid jump presses
    SMOOTHING_FACTOR_HORIZONTAL: 30,      // Smoothing for stable turn direction (left/right view)
    SMOOTHING_FACTOR_HORIZONTAL_QUICKER: 15, // Smoothing for reactive steering
    SMOOTHING_VERTICAL_ON: 50,
    SMOOTHING_VERTICAL_OFF: 20,
    SMOOTHING_VERTICAL_TAILOFF: 25
};

export const BALLOON_CONFIG = {
    MAX_HORIZONTAL_SPEED: 0.55,
    HORIZONTAL_ACCEL: 0.002,
    VERTICAL_ACCEL: 0.006,
    MAX_VERTICAL_ASCEND_SPEED: 0.13,
    VERTICAL_DESCEND_SPEED: -0.03,
    WIND_OFFSET: 0, // 0 = no offset. 1 = rotate clockwise (N→E→S→W), etc.
    HEAT_TIME: 60 // 5 seconds = 100 ticks (20 ticks/second)
};

export const MECH_CONFIG = {
    // Movement
    MAX_SPEED: 1.5,
    ACCELERATION: 0.05,
    
    // Boost System
    BOOST_IMPULSE_STRONG: 1.0,      // Initial strong impulse when boost starts
    BOOST_IMPULSE_SUSTAIN: 0.05,     // Continuous impulse while boost is active
    BOOST_COOLDOWN_SECONDS: 7,

    // Projectile
    PROJECTILE_COOLDOWN_SECONDS: 3,
};
//--- Configuration: Steamboat Behavior Settings ---//
export const STEAMBOAT_CONFIG_OLD = {
    MAX_SPEED: 0.13,                      // Max speed of the steamboat
    ACCELERATION: 0.0008,                 // Steamboat acceleration per tick
    GROUND_OFFSET: 1,                     // Impulse scaling when on ground
    JUMP_COOLDOWN: 2,                     // Delay between valid jump presses
    SMOOTHING_FACTOR_HORIZONTAL_QUICKER: 70 // Smoother (slower) turning responsiveness
};

export const STEAMBOAT_CONFIG = {
    // Basic movement properties
    BASE_SPEED: 0.10,              // Base forward/backward speed (slightly faster than sailboat)
    MAX_SPEED: 0.12,                // Maximum achievable speed (faster than sailboat)
    ACCELERATION: 0.006,           // How quickly boat accelerates per tick
    DECELERATION: 0.018,           // How quickly boat decelerates per tick
    TURN_SPEED: 8,               // Degrees per tick turning speed (more responsive than sailboat)
    
    // Smoothing factors for fluid movement
    SPEED_SMOOTHING_FACTOR: 4,     // Higher = smoother speed transitions
    ROTATION_SMOOTHING_FACTOR: 6,  // Higher = smoother rotation transitions

};

export const SAILBOAT_CONFIG = {
    // Basic movement properties
    BASE_SPEED: 0.15,              // Base forward/backward speed
    MAX_SPEED: 0.25,               // Maximum achievable speed
    ACCELERATION: 0.01,            // How quickly boat accelerates per tick
    DECELERATION: 0.015,           // How quickly boat decelerates per tick
    TURN_SPEED: 2.0,               // Degrees per tick turning speed
    
    // Wind effect properties
    WIND_SPEED_BONUS: 0.4,         // Speed multiplier bonus when sailing with wind (40% increase)
    WIND_SPEED_PENALTY: 0.5,       // Speed reduction when sailing against wind (50% decrease)
    WIND_ANGLE_FRAME: 45,          // Total angle range in degrees where wind effects apply
    WIND_TRANSITION_FRAME: 15,     // Angle range where wind effects start to apply (gradual transition)
    
    // Smoothing factors for fluid movement
    SPEED_SMOOTHING_FACTOR: 8,     // Higher = smoother speed transitions
    ROTATION_SMOOTHING_FACTOR: 6,  // Higher = smoother rotation transitions
    
};

export const FUEL_TITLE_DELAY_TICKS = 20;



//--- Entity Type and Item ID Constants ---//

export const VEHICLE_TYPES = {
    MINECAR: "bw_vv:minecar",
    LOGCAR: "bw_vv:logcar",
    PLANE: "bw_vv:plane",
    STEAMBOAT: "bw_vv:steamboat",
    MOBTRUCK: "bw_vv:mobtruck",
    HOT_AIR_BALLOON: "bw_vv:hot_air_balloon",
    MOBTRUCK_BACKSEAT: "bw_vv:mobtruck_backseat",
    PRISMER: "bw_vv:prismer",
    MECH: "bw_vv:mech"
};
export const ITEM_IDS = {
    TESLA_HELMET: "bw_vv:tesla_helmet",
    NETHER_STAR: "minecraft:nether_star"
};

export const ITEM_TAGS = {
    PICKAXE: "minecraft:is_pickaxe",
    AXE: "minecraft:is_axe"
};

/**
 * Animation mapping data for player vehicle animations
 * Maps vehicle states and climb angles to specific animation identifiers
 * Used for triggering appropriate player animations based on vehicle behavior
 */

export const VEHICLE_CLIMB_STATE_ANIMATIONS = {
    "45_degree": {
        climb_down: "animation.bw_vv.player_vehicle_down45",
        climb_up: "animation.bw_vv.player_vehicle_up45",
        climb_neutral: "animation.bw_vv.player_vehicle_neutral",
        steamboat_captain: "animation.bw_vv.player_drive_steamboat",
        mech_forward: "animation.bw_vv.player_drive_mech_forward",
        mech_neutral: "animation.bw_vv.player_drive_mech_neutral",
        mech_backward: "animation.bw_vv.player_drive_mech_backward",
        mech_left: "animation.bw_vv.player_drive_mech_backward",
        mech_right: "animation.bw_vv.player_drive_mech_backward",
        mech_jump: "animation.bw_vv.player_drive_mech_jump",
        mech_land: "animation.bw_vv.player_drive_mech_land",
        mech_shoot: "animation.bw_vv.player_drive_mech_shoot",
        climb_neutral_f_u: "animation.bw_vv.player_vehicle_neutral_from_up45",
        climb_neutral_f_d: "animation.bw_vv.player_vehicle_neutral_from_down45",
        falling: "animation.bw_vv.player_vehicle_falling45",
        landed: "animation.bw_vv.player_vehicle_neutral_from_down45",
        in_water: "animation.bw_vv.player_vehicle_in_water"
    },
    "20_degree": {
        climb_down: "animation.bw_vv.player_vehicle_down20",
        climb_up: "animation.bw_vv.player_vehicle_up20",
        climb_neutral: "animation.bw_vv.player_vehicle_neutral",
        climb_neutral_f_u: "animation.bw_vv.player_vehicle_neutral_from_up20",
        climb_neutral_f_d: "animation.bw_vv.player_vehicle_neutral_from_down20",
        falling: "animation.bw_vv.player_vehicle_falling20",
        landed: "animation.bw_vv.player_vehicle_neutral_from_down20",
        in_water: "animation.bw_vv.player_vehicle_in_water",
        plane_flying: "animation.bw_vv.player_drive_plane",
        plane_crashing: "animation.bw_vv.player_drive_plane",
        prismer_swimming: "animation.bw_vv.player_drive_prismer"
    }
};

export const PLAYER_DRIVE_ANIMATION_VARIABLES = `
    variable.pre_body_rot = query.ride_body_y_rotation - query.head_y_rotation(0);
    variable.body_rot = -math.mod(variable.pre_body_rot, 360);
    (variable.body_rot > 180) ? (variable.body_rot = variable.body_rot - 360);
    (variable.body_rot < -180) ? (variable.body_rot = variable.body_rot + 360);
    variable.body_rot_clamped = math.clamp(variable.body_rot, -43, 43);

    v.rightArm_x = -32-20*math.sin(v.body_rot_clamped*1.2-2/3*380);
    v.rightArm_y = -35*math.cos(-v.body_rot_clamped*1.2-90)-10;
    v.rightArm_z = -this-20-30*math.cos(-v.body_rot_clamped+70)+10;

    v.leftArm_x = -32-20*math.cos(v.body_rot_clamped*1.2);
    v.leftArm_y = -35*math.cos(-v.body_rot_clamped*1.2-90)+10;
    v.leftArm_z = 20-30*math.cos(-v.body_rot_clamped+110)-10;
    
    variable.head_factor = 19;
    variable.head_factor_45 = 24;
    variable.player_rotation_custom = 0.79*(math.clamp(v.body_rot,-200,-50) + math.clamp(v.body_rot,50,200));
`;