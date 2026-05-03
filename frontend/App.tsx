import { Dashboard } from './components/Dashboard';

/**
 * Główny komponent aplikacji.
 * Odpowiada za wyrenderowanie dashboardu symulacji.
 */
function App() {
    return (
        <div className="w-full min-h-screen bg-[#0a0a0a]">
            <Dashboard />
        </div>
    );
}

// Kluczowa linia, która naprawia błąd "is not a module"
export default App;