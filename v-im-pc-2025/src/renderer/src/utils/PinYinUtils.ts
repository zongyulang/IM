import { match } from 'pinyin-pro'

function keywordFilter(items: any[], keyword: string): any[] {
  return items.filter((item) => {
    return !!(keyword.trim() === '' || match(item.name, keyword))
  })
}

export default keywordFilter
