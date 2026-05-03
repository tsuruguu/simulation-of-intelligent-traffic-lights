import React from 'react'
import ReactDOM from 'react-dom/client'
import App from '../App'
import './index.css' // Ważne: import stylów musi tu być![cite: 17, 18]

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <App />
    </React.StrictMode>,
)