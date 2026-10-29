const starStyle = {
  background: 'none',
  border: 'none',
  padding: '0 1px',
  cursor: 'pointer',
  fontSize: '1.1em',
  lineHeight: 1,
}

/**
 * Nam ngoi sao bam duoc. value la diem cua chinh nguoi dang dang nhap (0 = chua cham).
 * Khong truyen onPick thi chi hien thi, khong bam duoc.
 */
export default function Stars({ value = 0, onPick, disabled }) {
  return (
    <span role="group" aria-label="Chấm điểm">
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          style={starStyle}
          disabled={disabled || !onPick}
          title={`${star} sao`}
          aria-label={`${star} sao`}
          onClick={() => onPick && onPick(star)}
        >
          {star <= value ? '★' : '☆'}
        </button>
      ))}
    </span>
  )
}
