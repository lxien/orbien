/*
 *    Copyright 2026 lxien
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
 * 字节格式化工具类
 */
export class ByteUtils {
  /**
   * 将字节数格式化为带单位的字符串（KB/MB/GB/TB）
   * @param bytes 字节数
   * @returns 格式化后的字符串
   */
  public static formatBytes(bytes: number): string {
    if (bytes <= 0) return '0 B'
    let value = bytes
    let unit = 'B'
    
    if (value >= 1024 * 1024 * 1024 * 1024) {
      value /= (1024.0 * 1024 * 1024 * 1024)
      unit = 'TB'
    } else if (value >= 1024 * 1024 * 1024) {
      value /= (1024.0 * 1024 * 1024)
      unit = 'GB'
    } else if (value >= 1024 * 1024) {
      value /= (1024.0 * 1024)
      unit = 'MB'
    } else if (value >= 1024) {
      value /= 1024.0
      unit = 'KB'
    }
    
    if (value === Math.floor(value)) {
      return `${Math.floor(value)} ${unit}`
    } else {
      const formatted = value.toFixed(2).replace(/\.?0*$/, '')
      return `${formatted} ${unit}`
    }
  }

  /**
   * 格式化大数字，和formatBytes(bytes: number): string相同
   * @param num 数字
   * @returns 格式化后的数字
   */
  public static formatNumber(num: number): number {
    if (num <= 0) return 0
    let value = num
    
    if (value >= 1024 * 1024 * 1024 * 1024) {
      value /= (1024.0 * 1024 * 1024 * 1024) // 转换为TB单位
    } else if (value >= 1024 * 1024 * 1024) {
      value /= (1024.0 * 1024 * 1024) // 转换为GB单位
    } else if (value >= 1024 * 1024) {
      value /= (1024.0 * 1024) // 转换为MB单位
    } else if (value >= 1024) {
      value /= 1024.0 // 转换为KB单位
    }
    
    if (value === Math.floor(value)) {
      return Math.floor(value)
    } else {
      return parseFloat(value.toFixed(2).replace(/\.?0*$/, ''))
    }
  }

  /**
   * 根据数据最大值确定统一的显示单位（B / KB / MB / GB / TB）
   * 用于图表 Y 轴等需要"所有刻度使用同一单位"的场景
   * @param maxBytes 数据中的最大值（字节数）
   * @returns { divisor: number; unit: string } 除数与单位标签
   */
  public static getUnitInfo(maxBytes: number): { divisor: number; unit: string } {
    if (maxBytes >= 1024 * 1024 * 1024 * 1024) {
      return { divisor: 1024 * 1024 * 1024 * 1024, unit: 'TB' }
    } else if (maxBytes >= 1024 * 1024 * 1024) {
      return { divisor: 1024 * 1024 * 1024, unit: 'GB' }
    } else if (maxBytes >= 1024 * 1024) {
      return { divisor: 1024 * 1024, unit: 'MB' }
    } else if (maxBytes >= 1024) {
      return { divisor: 1024, unit: 'KB' }
    } else {
      return { divisor: 1, unit: 'B' }
    }
  }
}
