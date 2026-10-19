export default function LoadStatus({ store }) {
  if (store.loading) return <p role="status">Đang tải dữ liệu...</p>
  if (!store.error) return null
  return (
    <p className="error" role="alert">
      {store.error}{' '}
      <button type="button" onClick={store.reload}>Thử lại</button>
    </p>
  )
}
