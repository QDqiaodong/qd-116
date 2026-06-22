import request from '../utils/request'

export function listAssets(params) {
  return request.get('/tooling/list', { params })
}

export function getAsset(id) {
  return request.get(`/tooling/${id}`)
}

export function createAsset(data, forceCreate = false) {
  return request.post('/tooling', data, { params: { forceCreate } })
}

export function checkDuplicateAsset(data) {
  return request.post('/tooling/check-duplicate', data)
}

export function updateAsset(id, data, forceUpdate = false, statusChangeRemark) {
  const params = { forceUpdate }
  if (statusChangeRemark !== undefined && statusChangeRemark !== null) {
    params.statusChangeRemark = statusChangeRemark
  }
  return request.put(`/tooling/${id}`, data, { params })
}

export function deleteAsset(id) {
  return request.delete(`/tooling/${id}`)
}

export function getStats() {
  return request.get('/tooling/stats')
}

export function getSystemConfig() {
  return request.get('/system/config')
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

export function getWorkstationStays(toolingCode) {
  return request.get(`/transfer/${toolingCode}/stays`)
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

export function markMissing(params) {
  return request.post('/inventory/diff/mark-missing', null, { params })
}

export function markMisplaced(params) {
  return request.post('/inventory/diff/mark-misplaced', null, { params })
}

export function markMatch(params) {
  return request.post('/inventory/diff/mark-match', null, { params })
}

export function markExtra(params) {
  return request.post('/inventory/diff/mark-extra', null, { params })
}

export function handleRecover(diffId, params) {
  return request.post(`/inventory/diff/handle-recover/${diffId}`, null, { params })
}

export function handleCorrectWorkstation(diffId, params) {
  return request.post(`/inventory/diff/handle-correct-workstation/${diffId}`, null, { params })
}

export function handleScrap(diffId, params) {
  return request.post(`/inventory/diff/handle-scrap/${diffId}`, null, { params })
}

export function listPendingDiffs(diffType) {
  return request.get('/inventory/diff/pending/list', { params: { diffType } })
}

export function countPendingDiffs() {
  return request.get('/inventory/diff/pending/count')
}

export function listToolingDiffs(toolingCode) {
  return request.get(`/inventory/diff/${toolingCode}`)
}

export function listToolingDiffsByMonth(checkMonth) {
  return request.get(`/inventory/diff/month/${checkMonth}`)
}

export function getInventorySummaryStats(checkMonth) {
  return request.get('/inventory/stats/summary', { params: { checkMonth } })
}

export function getNextLocatorBlockCode() {
  return request.get('/code-rule/locator-block/next')
}

export function validateLocatorBlockCode(toolingCode, excludeId) {
  const params = { toolingCode }
  if (excludeId !== undefined && excludeId !== null) {
    params.excludeId = excludeId
  }
  return request.post('/code-rule/locator-block/validate', null, { params })
}

export function getLocatorBlockCodeInfo() {
  return request.get('/code-rule/locator-block/info')
}

export function createWorkstationCapacity(data) {
  return request.post('/workstation-capacity', data)
}

export function updateWorkstationCapacity(id, data) {
  return request.put(`/workstation-capacity/${id}`, data)
}

export function deleteWorkstationCapacity(id) {
  return request.delete(`/workstation-capacity/${id}`)
}

export function getWorkstationCapacity(id) {
  return request.get(`/workstation-capacity/${id}`)
}

export function listWorkstationCapacities(params) {
  return request.get('/workstation-capacity/list', { params })
}

export function checkWorkstationCapacity(workstation) {
  return request.get('/workstation-capacity/check', { params: { workstation } })
}

export function createInventoryBatch(params) {
  return request.post('/inventory-batch', null, { params })
}

export function freezeInventoryBatch(batchId, params) {
  return request.post(`/inventory-batch/${batchId}/freeze`, null, { params })
}

export function unfreezeInventoryBatch(batchId, params) {
  return request.post(`/inventory-batch/${batchId}/unfreeze`, null, { params })
}

export function closeInventoryBatch(batchId, params) {
  return request.post(`/inventory-batch/${batchId}/close`, null, { params })
}

export function updateInventoryBatch(batchId, data) {
  return request.put(`/inventory-batch/${batchId}`, data)
}

export function deleteInventoryBatch(batchId) {
  return request.delete(`/inventory-batch/${batchId}`)
}

export function getInventoryBatch(batchId) {
  return request.get(`/inventory-batch/${batchId}`)
}

export function getInventoryBatchByMonth(batchMonth) {
  return request.get(`/inventory-batch/month/${batchMonth}`)
}

export function listInventoryBatches() {
  return request.get('/inventory-batch/list')
}

export function getLatestInventoryBatch() {
  return request.get('/inventory-batch/latest')
}

export function listInventoryBatchSnapshots(batchId) {
  return request.get(`/inventory-batch/${batchId}/snapshots`)
}

export function listInventoryBatchSnapshotsByMonth(batchMonth) {
  return request.get(`/inventory-batch/month/${batchMonth}/snapshots`)
}

export function getInventoryBatchSnapshot(batchId, toolingCode) {
  return request.get(`/inventory-batch/${batchId}/snapshots/${toolingCode}`)
}

export function getInventoryBatchSnapshotCount(batchId) {
  return request.get(`/inventory-batch/${batchId}/snapshot-count`)
}

export function getInventoryBatchStatus(batchMonth) {
  return request.get(`/inventory-batch/status/${batchMonth}`)
}

export function listWorkstationNames() {
  return request.get('/workstation-capacity/names')
}

export function getLatestInventoryBatchSummary() {
  return request.get('/inventory-batch/latest/summary')
}

export function checkHighRiskTransfer(fromWorkstation, toWorkstation) {
  return request.post('/high-risk-transfer-approval/check', null, {
    params: { fromWorkstation, toWorkstation },
  })
}

export function applyHighRiskTransfer(params) {
  return request.post('/high-risk-transfer-approval/apply', null, { params })
}

export function approveHighRiskTransfer(id, params) {
  return request.put(`/high-risk-transfer-approval/${id}/approve`, null, { params })
}

export function executeHighRiskTransfer(id, executor) {
  return request.post(`/high-risk-transfer-approval/${id}/execute`, null, {
    params: { executor },
  })
}

export function cancelHighRiskTransfer(id, canceller) {
  return request.put(`/high-risk-transfer-approval/${id}/cancel`, null, {
    params: { canceller },
  })
}

export function getHighRiskTransferApproval(id) {
  return request.get(`/high-risk-transfer-approval/${id}`)
}

export function listHighRiskTransferApprovals(params) {
  return request.get('/high-risk-transfer-approval/search', { params })
}

export function listAllHighRiskTransferApprovals() {
  return request.get('/high-risk-transfer-approval/list')
}

export function listPendingHighRiskTransferApprovals() {
  return request.get('/high-risk-transfer-approval/pending')
}

export function listScrapReasons(category, enabledOnly = true) {
  return request.get('/scrap-reason/list', { params: { category, enabledOnly } })
}

export function getScrapSummary(toolingCode) {
  return request.get(`/scrap/summary/${toolingCode}`)
}
