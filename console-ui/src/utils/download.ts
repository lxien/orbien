/**
 * 文件下载工具模块
 * 提供统一的文件下载功能，支持从Blob或URL下载文件
 */

import { ElMessage } from 'element-plus'

/**
 * 从Blob下载文件
 * @param blob 文件内容
 * @param fileName 文件名
 * @param showSuccess 是否显示成功提示
 */
export function downloadBlob(blob: Blob, fileName: string, showSuccess: boolean = true): void {
  if (!blob) {
    ElMessage.error('下载内容为空')
    return
  }

  try {
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)

    if (showSuccess) {
      ElMessage.success('下载成功')
    }
  } catch (error: any) {
    console.error('下载失败:', error)
    ElMessage.error(error?.message || '下载失败')
  }
}

/**
 * 从URL下载文件（直接跳转方式）
 * @param url 文件URL
 * @param fileName 文件名（可选）
 */
export function downloadUrl(url: string, fileName?: string): void {
  try {
    const a = document.createElement('a')
    a.href = url
    if (fileName) {
      a.download = fileName
    }
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
  } catch (error: any) {
    console.error('下载失败:', error)
    ElMessage.error(error?.message || '下载失败')
  }
}

/**
 * 下载文本内容
 * @param text 文本内容
 * @param fileName 文件名
 * @param encoding 编码格式，默认UTF-8
 */
export function downloadText(text: string, fileName: string, encoding: string = 'UTF-8'): void {
  try {
    const blob = new Blob([text], { type: `text/plain;charset=${encoding}` })
    downloadBlob(blob, fileName)
  } catch (error: any) {
    console.error('下载失败:', error)
    ElMessage.error(error?.message || '下载失败')
  }
}
