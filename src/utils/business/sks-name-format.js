import { BUSINESS_NAME_SUFFIX } from '../sks-constant'

export function formatPurchaseOrderName(domain){
    if (!domain) {
       return
    }
    if (domain.virtulCompany) {
        domain.poName=domain.virtulCompany.companyName+" "+BUSINESS_NAME_SUFFIX.poName
    }
}
