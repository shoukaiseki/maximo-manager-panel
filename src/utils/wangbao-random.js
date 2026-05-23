/**
 * @param min       >= min
 * @param max       <  max
 * @return {number}
 */
export function getRandomInt(min=0xffffffff,max=0x7fffffff) {
    min = Math.ceil(min);
    max = Math.floor(max);
    return Math.floor(Math.random() * (max - min)) + min;
}
