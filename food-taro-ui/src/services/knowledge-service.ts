import { http } from './http'

export interface KnowledgeRichTextNode {
  type?: string
  name?: string
  text?: string
  attrs?: Record<string, string>
  children?: KnowledgeRichTextNode[]
}

export interface KnowledgeArticleSummary {
  id: string
  title: string
  category: string
  imageUrl?: string
  bodyPreview: string
  publishedAt: string
}

export interface KnowledgeArticleDetail extends KnowledgeArticleSummary {
  bodyNodes: KnowledgeRichTextNode[]
}

export interface KnowledgeArticleListResponse {
  items: KnowledgeArticleSummary[]
  page: number
  size: number
  hasMore: boolean
}

export class KnowledgeService {
  queryHistory(params?: { page?: number; size?: number }) {
    return http.get<KnowledgeArticleListResponse>('/api/knowledge', { params })
  }

  getArticle(id: string) {
    return http.get<KnowledgeArticleDetail>(`/api/knowledge/${id}`)
  }
}
