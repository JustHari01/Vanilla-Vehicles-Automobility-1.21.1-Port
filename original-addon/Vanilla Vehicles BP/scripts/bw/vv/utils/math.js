export const mathClamp = (v, min, max) =>
  Math.min(max, Math.max(min, v));

export const logDecreaseNegative = (y, k) => {
    // y in [-1, 1]; for y < 0 compress magnitude logarithmically.
    // Maps 0 -> 0, -1 -> -1, with concave log curve controlled by k (>0).
    return y >= 0 ? y : -Math.log1p(k * (-y)) / Math.log1p(k);
};

export const easeNegative = (y, gamma) => (y >= 0 ? y : -Math.pow(-y, gamma));

export function planeAnimCorrector(y) {
  return mathClamp(y + (y > 0.05 ? 0.05 : -0.1), -1, 1);
}
/**
 * Smoothing utilities for vehicle movement and rotation
 * Provides functions to smooth values over time to reduce jitter
 * Handles special cases like angle wrapping for rotation smoothing
 */

export const smoothValue = (newValue, history, factor) => {
    history.push(newValue);
    if (history.length > factor) history.shift();
    return history.reduce((sum, val) => sum + val, 0) / history.length;
};

export const smoothAngle = (newAngle, history, factor) => {
    const lastAngle = history.length > 0 ? history[history.length - 1] : newAngle;
    let delta = newAngle - lastAngle;
    
    // Handle 180° wrap-around
    if (delta > 180) delta -= 360;
    if (delta < -180) delta += 360;
    
    const smoothedAngle = lastAngle + delta;
    history.push(smoothedAngle);
    if (history.length > factor) history.shift();
    
    return history.reduce((sum, angle) => sum + angle, 0) / history.length;
};