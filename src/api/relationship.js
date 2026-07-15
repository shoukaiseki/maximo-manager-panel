import solonRequest from './solonapi'

export function getMaxRelationshipList(params) {
  return solonRequest({
    url: '/relationship/list',
    method: 'get',
    params
  })
}

export function getMaxRelationshipDetail(id) {
  return solonRequest({
    url: `/relationship/${id}`,
    method: 'get'
  })
}

export function getAllMaxRelationships() {
  return solonRequest({
    url: '/relationship/all',
    method: 'get'
  })
}

export function getMaxRelationshipsByParent(parent) {
  return solonRequest({
    url: `/relationship/parent/${parent}`,
    method: 'get'
  })
}