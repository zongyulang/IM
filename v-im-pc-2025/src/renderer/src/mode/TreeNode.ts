export interface TreeNode {
  id: string
  label: string
  parentId: string
  count: number
  children: Array<TreeNode>
}
