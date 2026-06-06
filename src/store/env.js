import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

// 默认环境配置
const defaultEnvs = [
  {
    key: 'default',
    name: 'Maximo 环境',
    backendBaseUrl: 'http://localhost:9080',
    useApiKey: false,
    apiKey: ''
  }
]

const state = {
  envs: defaultEnvs,
  selectedKey: defaultEnvs[0]?.key || ''
}

const mutations = {
  SET_SELECTED_ENV(state, key) {
    state.selectedKey = key
  }
}

const actions = {
  setSelectedEnv({ commit }, key) {
    commit('SET_SELECTED_ENV', key)
  }
}

const getters = {
  selectedEnv: state => {
    return state.envs.find(item => item.key === state.selectedKey) || state.envs[0]
  },
  envOptions: state => {
    return state.envs.map(env => ({
      key: env.key,
      name: env.name
    }))
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
  getters
}