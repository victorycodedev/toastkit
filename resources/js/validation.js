export const allowed = {
    variant: ['neutral', 'success', 'error', 'warning', 'info'],
    position: ['top', 'center', 'bottom'], animation: ['fade', 'slide', 'scale', 'spring'], strategy: ['queue', 'stack']
};
export function nonEmpty(value, name) { if (typeof value !== 'string' || value.trim() === '') throw new TypeError(`${name} must be a non-empty string`); return value; }
export function oneOf(value, name) { if (!allowed[name].includes(value)) throw new TypeError(`Invalid toast ${name}: ${value}`); return value; }
export function positive(value, name) { if (!Number.isFinite(value) || value <= 0) throw new TypeError(`${name} must be greater than zero`); return value; }
export function positiveInteger(value, name) { if (!Number.isInteger(value) || value <= 0) throw new TypeError(`${name} must be a positive integer`); return value; }
export function nonNegative(value, name) { if (!Number.isFinite(value) || value < 0) throw new TypeError(`${name} must not be negative`); return value; }
export function color(value) {
    if (typeof value !== 'string' || !/^#(?:[\da-f]{3}|[\da-f]{6}|[\da-f]{8})$/i.test(value)) throw new TypeError('Colors must use #RGB, #RRGGBB, or #AARRGGBB hexadecimal format');
    return value.toUpperCase();
}
