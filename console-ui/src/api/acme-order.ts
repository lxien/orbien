import request from '@/utils/http'

export function fetchCreateAcmeOrder(params: Api.AcmeOrder.CreateParams) {
  return request.post<Api.AcmeOrder.OrderDTO>({
    url: '/api/acme-order/create',
    data: params
  })
}

export function fetchAcmeOrderDetail(orderId: number) {
  return request.get<Api.AcmeOrder.OrderDTO>({
    url: `/api/acme-order/${orderId}`
  })
}

export function fetchAcmeOrderPage(params: Api.Common.CommonSearchParams) {
  return request.get<Api.Common.PaginatedResponse<Api.AcmeOrder.OrderDTO>>({
    url: '/api/acme-order',
    params
  })
}

export function fetchVerifyAcmeOrder(orderId: number) {
  return request.post({
    url: `/api/acme-order/${orderId}/verify`
  })
}

export function fetchCancelAcmeOrder(orderId: number) {
  return request.post({
    url: `/api/acme-order/${orderId}/cancel`
  })
}

export function fetchRetryAcmeOrder(orderId: number) {
  return request.post({
    url: `/api/acme-order/${orderId}/retry`
  })
}

export function fetchDeleteAcmeOrders(ids: Api.AcmeOrder.DeleteIds) {
  return request.del({
    url: '/api/acme-order',
    data: ids
  })
}
