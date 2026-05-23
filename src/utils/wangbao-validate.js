/**
 { required: false,  message: "只能输入数字" , validator: isNumberInRuleValidate , trigger: "blur"}
  */
/**
 * rule验证,是否为数字
 * @param rule
 * @param value
 * @param callback
 */
export function isNumberInRuleValidate(rule, value, callback) {
  if(value == undefined||value == ''){
    if(rule&&rule.required){
      callback(new Error(rule.message||'必填属性'))
      return;
    }
    callback()
    return
  }
  let reg = /^-?\d+(\.?[0-9]+)?$/;
  if(reg.test(value)){
    callback()
  }else{
    callback(new Error(rule.message||'只能输入数字'))
  }
}

/**
 * rule验证,是否为整数
 * @param rule
 * @param value
 * @param callback
 */
export function isIntegerInRuleValidate(rule, value, callback) {
  if(value == undefined||value == ''){
    if(rule&&rule.required){
      callback(new Error(rule.message||'必填属性'))
      return;
    }
    callback()
    return
  }
  let reg = /^-?\d+?$/;
  if(reg.test(value)){
    callback()
  }else{
    callback(new Error(rule.message||'只能输入整数'))
  }
}

/**
 * rule验证,是否为正整数
 * @param rule
 * @param value
 * @param callback
 */
export function isPositiveIntegerInRuleValidate(rule, value, callback) {
  if(value == undefined||value === ''){
    if(rule&&rule.required){
      callback(new Error(rule.message||'必填属性'))
      return;
    }
    callback()
    return
  }
  let reg = /^\d+?$/;
  if(reg.test(value)){
    callback()
  }else{
    callback(new Error(rule.message||'只能输入正整数,不能为负数'))
  }
}

/**
 * rule验证,是否为正整数,而且不为0
 * @param rule
 * @param value
 * @param callback
 */
export function isPositiveIntegerAndNotZeroInRuleValidate(rule, value, callback) {
  if(value == undefined||value === ''){
    if(rule&&rule.required){
      callback(new Error(rule.message||'必填属性'))
      return;
    }
    callback()
    return
  }
  let reg = /^\d+?$/;
  if(reg.test(value)){
    if(value!=0){
      callback()
        return
    }
  }
  callback(new Error(rule.message||'只能输入正整数,而且必须大于0'))
}
