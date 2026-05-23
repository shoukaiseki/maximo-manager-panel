import Vue from 'vue'
// import globalVue from '@/global/global_variable'


// const state = {
//     sidebar: {
//         opened: Cookies.get('sidebarStatus') ? !!+Cookies.get('sidebarStatus') : true,
//         withoutAnimation: false
//     },
//     device: 'desktop',
//     size: Cookies.get('size') || 'medium'
// }

export const wbCache={
    getSysUser:(userId,{success=(user)=>{}}={})=>{
        // Vue.prototype.$store.state.userId
        Vue.prototype.$store.dispatch("GetSysUser", userId)
            .then((response) => {
                success(response.data)
                // this.$router.push({ path: this.redirect || "/" });
            })
            .catch(() => {
                // this.loading = false;
                // this.getCode();
            });
        // nextTimeSysUser
    }
}



export default wbCache
