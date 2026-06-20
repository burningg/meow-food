import { http } from './http'

export class FeedbackService {
  submitFeedback(content: string) {
    return http.post<void>('/api/feedback', { content })
  }
}
