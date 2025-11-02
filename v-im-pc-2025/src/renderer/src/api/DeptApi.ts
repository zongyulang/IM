import FetchRequest from '@renderer/api/FetchRequest'
import type { Dept } from '@renderer/mode/Dept'
import type { User } from '@renderer/mode/User'
import type { TreeNode } from '@renderer/mode/TreeNode'

class DeptApi {
  static url = '/vim/server/depts'

  /**
   * 获取所有上级部门
   * @param deptId 部门ID
   */
  static parent(deptId: string): Promise<Dept[]> {
    return FetchRequest.get(this.url + '/parent?deptId=' + deptId, true)
  }

  /**
   * 获取所有部门
   */
  static list(): Promise<TreeNode[]> {
    return FetchRequest.get(this.url, true)
  }

  /**
   * 获取部门
   * @param id 部门ID
   */
  static get(id: string): Promise<Dept> {
    return FetchRequest.get(`${this.url}/${id}`, true)
  }

  /**
   * 获取部门用户
   * @param deptId 部门ID
   * @param pageNo pageNo
   * @param pageSize pageSize
   */
  static users(deptId: string, pageNo = 1, pageSize = 100): Promise<User[]> {
    return FetchRequest.get(`${this.url}/${deptId}/users?current=${pageNo}&size=${pageSize}`, true)
  }

  /**
   * 获取部门人数
   */
  static count(): Promise<number> {
    return FetchRequest.get(`${this.url}/count`, true)
  }
}

export default DeptApi
