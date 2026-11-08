import { formatMoment } from '../newsStatus'
import Stars from '../components/Stars'

const placeholder = (a) => `https://picsum.photos/seed/${encodeURIComponent(a.slug || a.id)}/1200/600`

// Trang chi tiet mot bai. Bai lay thang tu danh sach (NewsResponse da co content),
// nen khong goi them API. Doc gia dang nhap thi cham diem duoc; khach thi khong.
export default function Article({ article, categoryName, canRate, myScore, rating, onRate, onBack }) {
  if (!article) return null
  return (
    <article className="article">
      <button type="button" className="link-back" onClick={onBack}>← Về trang chủ</button>
      <span className="news-card-cat">{categoryName(article.categoryId)}</span>
      <h1 className="article-title">{article.title}</h1>
      <div className="article-meta">
        {formatMoment(article.publishedAt || article.updatedAt)}
        {article.source && (
          <> · Nguồn: <a href={article.source} target="_blank" rel="noreferrer">{article.source}</a></>
        )}
      </div>
      <img className="article-thumb" src={article.thumbnailUrl || placeholder(article)} alt=""
        onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholder(article) }} />
      {article.summary && <p className="article-summary">{article.summary}</p>}
      <div className="article-content">
        {(article.content || '').split('\n').filter(Boolean).map((para, i) => (
          <p key={i}>{para}</p>
        ))}
      </div>
      {(article.tags || []).length > 0 && (
        <div className="article-tags">
          {article.tags.map((t) => <span key={t} className="tag">#{t}</span>)}
        </div>
      )}
      <div className="article-rating">
        <span>Đánh giá: {article.ratingAvg ? `${article.ratingAvg} ` : '– '}({article.ratingCount || 0})</span>{' '}
        {canRate
          ? <Stars value={myScore || 0} onPick={(s) => onRate(article, s)} disabled={rating} />
          : <small>Đăng nhập bằng tài khoản độc giả để chấm điểm.</small>}
      </div>
    </article>
  )
}
