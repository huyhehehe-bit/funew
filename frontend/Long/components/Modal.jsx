// Popup dialog dung cho Create / Update
export default function Modal({ title, onClose, children, busy }) {
  const close = () => { if (!busy) onClose() }
  return (
    <div className="backdrop" onClick={close}>
      <div className="block modal" role="dialog" aria-modal="true" aria-label={title} onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <b>{title}</b>
          <button type="button" onClick={close} disabled={busy}>X</button>
        </div>
        {children}
      </div>
    </div>
  )
}
