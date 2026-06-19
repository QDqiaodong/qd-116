import request from '../utils/request'

export function listAssets(params) {
  return request.get('/tooling/list', { params })
}

export function getAsset(id) {
  return request.get(`/tooling/${id}`)
}

export function createAsset(data) {
  return request.post('/tooling', data)
}

export function updateAsset(id, data) {
  return request.put(`/tooling/${id}`, data)
}

export function deleteAsset(id) {
  return request.delete(`/tooling/${id}`)
}

export function getStats() {
  return request.get('/tooling/stats')
}

export function transferTooling(params) {
  return request.post('/transfer', null, { params })
}

export function listTransfers() {
  return request.get('/transfer/list')
}

export function getTransferHistory(toolingCode) {
  return request.get(`/transfer/${toolingCode}`)
}

export function createCheck(params) {
  return request.post('/inventory', null, { params })
}

export function listChecks() {
  return request.get('/inventory/list')
}

export function getLatestCheck() {
  return request.get('/inventory/latest')
}

export function scrapTooling(params) {
  return request.post('/scrap', null, { params })
}

export function listScraps() {
  return request.get('/scrap/list')
}

export function getScrapHistory(toolingCode) {
  return request.get(`/scrap/${toolingCode}`)
}

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function saveSpecTemplate(category, data) {
  return request.post(`/spec-template/${category}`, data)
}

export function getSpecTemplate(category) {
  return request.get(`/spec-template/${category}`)
}

export function deleteSpecTemplate(category) {
  return request.delete(`/spec-template/${category}`)
}

export function listSpecCategories() {
  return request.get('/spec-template/categories')
}

export function getToolingTrace(toolingCode) {
  return request.get(`/trace/${toolingCode}`)
}

export function recordToolingDiff(params) {
  return request.post('/inventory/diff', null, { params })
}

export function listToolingDiffs(toolingCode) {
  return request.get(`/inventory/diff/${toolingCode}`)
}
