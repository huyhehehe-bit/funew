export default function Menu({ items, current, onChange }) {
  return (
    <nav className="block menu">
      {items.map((item) => (
        <button
          key={item}
          className={item === current ? 'active' : ''}
          onClick={() => onChange(item)}
        >
          {item}
        </button>
      ))}
    </nav>
  )
}
