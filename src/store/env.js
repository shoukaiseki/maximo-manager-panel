import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

import envsData from '../../server/backend.config.json'

const state = {
  envs: envsData,
  selectedKey: envsData[0]?.key || ''
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