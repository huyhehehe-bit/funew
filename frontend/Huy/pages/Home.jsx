import { formatMoment } from '../newsStatus'

// Anh thay the khi bai khong co thumbnail hoac URL hong. Seed theo slug/id de moi
// bai mot anh khac nhau thay vi dung chung mot anh.
const placeholder = (a) => `https://picsum.photos/seed/${encodeURIComponent(a.slug || a.id)}/800/450`

// Trang chu doc bao: luoi the bai da xuat ban. Backend da loc chi tra PUBLISHED
// cho khach va doc gia, nen o day khong loc gi them.
export default function Home({ articles, categoryName, loading, error, onOpen }) {
  return (
    <div className="home">
      <h1 className="home-title">Tin tức FU</h1>
      {loading && <p role="status">Đang tải bài viết...</p>}
      {error && <p className="error" role="alert">{error}</p>}
      {!loading && !error && articles.length === 0 && (
        <p>Chưa có bài viết nào được xuất bản.</p>
      )}
      <div className="card-grid">
        {articles.map((a) => (
          <article key={a.id} className="news-card" onClick={() => onOpen(a)}
            tabIndex={0} role="button"
            onKeyDown={(e) => { if (e.key === 'Enter') onOpen(a) }}>
            <div className="news-card-thumb">
              <img src={a.thumbnailUrl || placeholder(a)} alt="" loading="lazy"
                onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholder(a) }} />
            </div>
            <div className="news-card-body">
              <span className="news-card-cat">{categoryName(a.categoryId)}</span>
              <h2 className="news-card-title">{a.title}{a.isFeatured ? ' ★' : ''}</h2>
              <p className="news-card-summary">{a.summary}</p>
              <div className="news-card-meta">
                {formatMoment(a.publishedAt || a.updatedAt)}
                {a.ratingCount > 0 && <span> · ★ {a.ratingAvg} ({a.ratingCount})</span>}
              </div>
            </div>
          </article>
        ))}
      </div>
    </div>
  )
}
