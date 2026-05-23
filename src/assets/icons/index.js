import Vue from 'vue'
import SvgIcon from '/src/components/SvgIcon'// svg component

// register globally
Vue.component('svg-icon', SvgIcon)

const req = require.context('./svg', false, /\.svg$/)
// console.log("icons.svg.req",req)
const requireAll = requireContext => requireContext.keys().map(requireContext)
requireAll(req)

// console.log("req.keys",req.keys())

req.keys().map(item=>{
    // console.log(item)
})
