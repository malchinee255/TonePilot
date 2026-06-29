import axios from 'axios'

export const api = axios.create({
  baseURL: '',
  timeout: 20000
})

export type ApiResponse<T> = {
  success: boolean
  data: T
  message: string
}

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await request
  if (!response.data.success) {
    throw new Error(response.data.message)
  }
  return response.data.data
}
