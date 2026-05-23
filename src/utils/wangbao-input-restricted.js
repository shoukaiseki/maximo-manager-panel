/**
 * 字符串转数字
 * @param value
 * @returns {number|*}
 */
export function stringToDecimal(value){
  if (value === undefined) {
    return value
  }
  if(value.constructor === Number){
    return value
  }

  let newVal=value.replace(/[^0-9.]/ig,"");
  let lastInd = newVal.lastIndexOf(".")
  let ind=newVal.indexOf(".")
  while(lastInd>-1&&(ind!=lastInd)){
    newVal=newVal.replace('.','')
    lastInd = newVal.lastIndexOf(".")
    ind=newVal.indexOf(".")
  }
  return Number(newVal)
}

// 通过正则过滤小数点后两位
export function inputRestrictedDecimal({value,rowData,attrName,precision,max,min,step,stepStrictly}) {
  // console.log("inputRestrictedDecimal",value,rowData)
  let newVal = value === undefined ? value : stringToDecimal(value);
  let minusSign=""
  let point=""
  if(value!==undefined){
    minusSign=value.indexOf("-")==0?"-":""
    point=value.lastIndexOf(".")==value.length-1?".":""
  }
  if (newVal !== undefined) {
    // console.log("newVal1=",newVal)
    if (isNaN(newVal)) {
      const tmp=value.match(/^\d*(\.?\d{0,2})/g)[0];
      // console.log("tmp=",tmp)
      rowData[attrName]=minusSign+tmp+point
      return;
    }
    // console.log("minusSign=",minusSign)
    if(minusSign==="-"){
      newVal=0.0-newVal
    }
    // console.log("newVal2=",newVal)

    // console.log("newVal2=",newVal)
    if (stepStrictly) {
      const stepPrecision = getPrecision(step);
      const precisionFactor = Math.pow(10, stepPrecision);
      newVal = Math.round(newVal / step) * precisionFactor * step / precisionFactor;
    }

    // console.log("newVal3=",newVal)
    if (precision !== undefined) {
      newVal = toPrecision(newVal, precision);
    }
  }
  if (newVal >= max){
    newVal = max;
    // console.log("newVal >= max")
  }
  if (newVal <= min){
    newVal = min;
    // console.log("newVal <= min")
  }
  if(newVal==0&&minusSign==="-"){
    newVal=minusSign+newVal
  }
  // console.log("newVal.9=",newVal)
  if(point==="."){
    const tmp2=newVal+""
    if (tmp2.lastIndexOf('.') == tmp2.length - 1) {

    }else{
      newVal=newVal+point
    }
  }
  rowData[attrName]=newVal
  // console.log("newVal.end=",newVal)

  return  newVal
}

export function toPrecision(num, precision) {
  if (precision === undefined) precision = numPrecision;
  return parseFloat(Math.round(num * Math.pow(10, precision)) / Math.pow(10, precision));
}

export function getPrecision(value) {
  if (value === undefined) return 0;
  const valueString = value.toString();
  const dotPosition = valueString.indexOf('.');
  let precision = 0;
  if (dotPosition !== -1) {
    precision = valueString.length - dotPosition - 1;
  }
  return precision;
}
export function numPrecision() {
  const { value, step, getPrecision, precision } = this;
  const stepPrecision = getPrecision(step);
  if (precision !== undefined) {
    if (stepPrecision > precision) {
      console.warn('[Element Warn][InputNumber]precision should not be less than the decimal places of step');
    }
    return precision;
  } else {
    return Math.max(getPrecision(value), stepPrecision);
  }
}

