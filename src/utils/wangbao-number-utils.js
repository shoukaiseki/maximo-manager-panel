/**
 * 取反,1 -> -1 ,-1 -> 1
 * @returns number|null
 */
export function negateNumber(num) {
    if(num==null){
        return null;
    }
    return (0.0-num)+'';
}

/**
 *
 * @param num
 * @param length
 * @returns {string|null}  length=3
 *                         num =null 返回 null
 *                         num = 8   返回 "008"
 *                         num = 1000 返回 "1000"
 */
export function integerPrefixZero(num, length) {
    if(num==null){
        return null;
    }
    if((''+num.length)>=length){
        return num+''
    }
    return ( "0000000000000000" + num ).substr( -length );
}
