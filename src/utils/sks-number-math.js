/**
 * 数学计算,避免精度丢失
 * 例如 1-0.66= 0.33999999999999997
 */
import * as math from 'mathjs'

/**
 * 转换为number类型,如果不存在则返回0
 * @param str
 * @return {number}
 */
export function toNumberNullIsZero(str){
    let tmp = Number(str)
    return isNaN(tmp)?0:tmp
}

/**
 * 加法运算,null作为0处理
 * @param a
 * @param b
 * @returns {number}  a+b
 */
export function addNullIsZero(a, b) {
    let aTmp=a==null?math.bignumber(0):math.bignumber(a);
    let bTmp=b==null?math.bignumber(0):math.bignumber(b);
    const tmp=math.add(aTmp,bTmp);
    return Number(math.format(tmp));
}

/**
 * 求和运算,null作为0处理
 * @return {number}  a+b+c+d...+z
 */
export function sumNullIsZero(){
    const length = arguments.length
    if(length<1){
        return 0
    }
    let tmp=math.bignumber(0)
    for (let i=0; i < length; i++) {
        let num = math.bignumber(toNumberNullIsZero(arguments[i]))
        tmp=math.add(tmp,num)
    }
    return Number(math.format(tmp));
}

/**
 * 减法运算,null作为0处理
 *前面的为被减数,后面的为减数,第一个参数减去第二个参数
 * @param subtracted    a
 * @param subtrahend    b
 * @returns {number}  a-b
 */
export function subtractNullIsZero(subtracted, subtrahend) {
    let a=subtracted==null?math.bignumber(0):math.bignumber(subtracted);
    let b=subtrahend==null?math.bignumber(0):math.bignumber(subtrahend);
    const tmp=math.subtract(a,b);
    //如果不转换为Number 则 subtractNullIsZero(10000000,3333333.34) 结果会是 3.33333334e+6
    return Number(math.format(tmp));
}

/**
 * 乘法运算,null作为0处理
 * @param a
 * @param b
 * @param scale              小数位数
 * @param defaultValue       如果有个值为null或者0则返回该值
 * @returns {number}             否则返回乘后结果
 */
export function multiplyNullIsZero(a,b,scale=6,defaultValue=0.0){
    if(a===undefined||b===undefined){
        return defaultValue
    }
    if(a==0||b==0){
        return 0.0
    }
    let aTmp=a==null?math.bignumber(0):math.bignumber(a);
    let bTmp=b==null?math.bignumber(0):math.bignumber(b);
    const tmp=math.multiply(aTmp,bTmp);
    return bignumberToFixed(tmp,scale);
}



/**
 * a/b
 * @param a                  被除数
 * @param b                  除数
 * @param scale              小数位数
 * @param defaultValue       如果有个值为null或者0则返回该值
 * @returns {number}  a/b
 */
export function divideNullIsZero(a, b,scale=6,defaultValue=0.0) {
    if(a===undefined||b===undefined){
        return defaultValue
    }
    if(a==0||b==0){
        return 0.0
    }
    let aTmp=a==null?math.bignumber(0):math.bignumber(a);
    let bTmp=b==null?math.bignumber(0):math.bignumber(b);
    const tmp=math.divide(aTmp,bTmp);
    return bignumberToFixed(tmp,scale);
}

/**
 * bignumber 保留小数位数
 * @param tmp
 * @param scale
 * @returns {number}
 */
export function bignumberToFixed(tmp,scale=2){
    // console.log("bignumberToFixed.(tmp)",(tmp))
    let number = Number(math.format(tmp))

    // console.log("bignumberToFixed.math.format(tmp)",math.format(tmp))
    // console.log("bignumberToFixed.number",number)
    // console.log("bignumberToFixed.number.toFixed(scale)",number.toFixed(scale))
    let number1 = Number(number.toFixed(scale))
    // console.log("bignumberToFixed.number.return",number1)
    return number1;
}

/**
 * 计算table表格某个字段的总和
 * @param list              table数组
 * @param propName          属性名
 * @returns {number}
 */
export function sumFormTableList({list,propName}){
    if (list === undefined||propName===undefined) {
        return Number(0.0)
    }
    let totalValue=0.0;
    for (let item of list) {
        totalValue=addNullIsZero(totalValue,item[propName])
    }
    return totalValue

}

// import { sumFormTableList,bignumberToFixed,divideNullIsZero,multiplyNullIsZero,subtractNullIsZero,addNullIsZero } from '@/utils/sks-number-math'

/**
 *
 * @param targetValue
 * @param currentValue
 * @param scale                 小数位数:0或者1,2
 * @return {number}
 */
export function calcPercentageProgressValue(targetValue,currentValue,scale=0){
    let subVal=Math.pow(0.1,scale)
    if (subtractNullIsZero(currentValue, targetValue) >= 0) {
        return 100
    }
    let  val= bignumberToFixed(multiplyNullIsZero(divideNullIsZero(currentValue, targetValue),100),scale)
    console.log("targetValue=",targetValue,"currentValue=",currentValue)
    console.log("val",val)
    if(val>=100){
        val=100
    }
    if(val<0){
        return 0
    }
    if (val == 100) {
        if (subtractNullIsZero(targetValue,currentValue) > 0) {
            return subtractNullIsZero(100,subVal)
        }
        return 100
    }
    return val
}

/**
 * 计算含税价格
 * @param notTaxCost
 * @param taxRate
 */
export function calcTaxCost(notTaxCost,taxRate){
    //含税成本=不含税成本*(税率百分比+1)
    let taxRateTmp=taxRate?multiplyNullIsZero(taxRate,1.01):1
    console.log("calcNotTaxCost",notTaxCost,taxRate)
    return multiplyNullIsZero(taxRateTmp,notTaxCost)

}
/**
 * 计算不含税价格
 * @param taxCost
 * @param taxRate
 */
export function calcNotTaxCost(taxCost,taxRate){
    //不含税成本=含税成本*(1-税率百分比)
    let taxRateTmp=taxRate?subtractNullIsZero(1,multiplyNullIsZero(taxRate,0.01)):1
    console.log("calcNotTaxCost",taxCost,taxRate)
    return multiplyNullIsZero(taxRateTmp,taxCost)
}
