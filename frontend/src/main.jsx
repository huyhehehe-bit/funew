import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '../Long/index.css'
import App from '../QA/App.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
