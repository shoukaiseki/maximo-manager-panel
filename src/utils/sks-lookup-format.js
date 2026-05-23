/**
 * 通用js方法封装处理
 * Copyright (c) 2019 ruoyi
 */

export function getCompanyByCompanyId(companyList,companyId){
  for (let i = 0; i < companyList.length; i++) {
    let company=companyList[i]
    if (company.companyId == companyId) {
      return company;
    }
  }
  return undefined;
}


export function getCompanyNameByCompanyId(companyList,companyId){
  let company=getCompanyByCompanyId(companyList,companyId);
  if(company==undefined){
    return companyId;
  }
  return company.companyName;
}
