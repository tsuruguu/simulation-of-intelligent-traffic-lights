import * as React from 'react';
import { useState, useEffect, useCallback, useMemo } from 'react';
import { Intersection } from './Intersection';
import { SimulationResult, Direction, LightState, Command } from '../types/simulation';
import { Play, Pause, SkipForward, RefreshCcw, Activity, Car, Download, ScrollText, BarChart3 } from 'lucide-react';

export const Dashboard: React.FC = () => {
    const [commands, setCommands] = useState<Command[]>([]);
    const [data, setData] = useState<SimulationResult | null>(null);
    const [currentStep, setCurrentStep] = useState(0);
    const [isPlaying, setIsPlaying] = useState(false);
    const [speed, setSpeed] = useState(800);
    const [eventLog, setEventLog] = useState<string[]>(["System gotowy. Wygeneruj scenariusz lub wgraj dane."]);

    const [vehicleRoutes, setVehicleRoutes] = useState<Record<string, { from: Direction, to: Direction }>>({});

    const [waitingVehicles, setWaitingVehicles] = useState<Record<Direction, string[]>>({
        NORTH: [], EAST: [], SOUTH: [], WEST: []
    });

    const [crossingVehicles, setCrossingVehicles] = useState<{ id: string, from: Direction, to: Direction }[]>([]);

    const arrivalSchedule = useMemo(() => {
        const schedule: Record<number, { id: string, from: Direction }[]> = {};
        let stepCounter = 0;
        commands.forEach(cmd => {
            if (cmd.type === 'addVehicle') {
                if (!schedule[stepCounter]) schedule[stepCounter] = [];
                schedule[stepCounter].push({ id: cmd.vehicleId, from: cmd.startRoad });
            } else if (cmd.type === 'step') {
                stepCounter++;
            }
        });
        return schedule;
    }, [commands]);

    const handleNextStep = useCallback(() => {
        if (!data || currentStep >= data.stepStatuses.length) return;

        const stepStatus = data.stepStatuses[currentStep];

        const newLeftIds = stepStatus.leftVehicles;
        const newLeaving = newLeftIds
            .map(id => ({ id, ...vehicleRoutes[id] }))
            .filter(v => v.from && v.to);

        if (newLeaving.length > 0) {
            setCrossingVehicles(prev => [...prev, ...newLeaving]);

            setTimeout(() => {
                setCrossingVehicles(prev => prev.filter(v => !newLeftIds.includes(v.id)));
            }, 2100);
        }

        setWaitingVehicles(prev => {
            const next = { ...prev };
            if (arrivalSchedule[currentStep]) {
                arrivalSchedule[currentStep].forEach(v => {
                    if (!next[v.from].includes(v.id)) next[v.from] = [...next[v.from], v.id];
                });
            }
            newLeftIds.forEach(id => {
                (Object.keys(next) as Direction[]).forEach(d => {
                    next[d] = next[d].filter(vId => vId !== id);
                });
            });
            return next;
        });

        setCurrentStep(prev => prev + 1);
    }, [data, currentStep, arrivalSchedule, vehicleRoutes]);

    useEffect(() => {
        let timer: number;
        if (isPlaying) {
            timer = window.setInterval(handleNextStep, speed);
        }
        return () => clearInterval(timer);
    }, [isPlaying, handleNextStep, speed]);

    const getLightStates = (): Record<Direction, LightState> => {
        const defaultLights: Record<Direction, LightState> = {
            NORTH: 'RED', EAST: 'RED', SOUTH: 'RED', WEST: 'RED'
        };

        if (!data || !data.stepStatuses[currentStep]) return defaultLights;

        const currentLeft = data.stepStatuses[currentStep]?.leftVehicles || [];
        const nextLeft = data.stepStatuses[currentStep + 1]?.leftVehicles || [];

        const isMoving = (dirs: Direction[], ids: string[]) =>
            ids.some(id => dirs.includes(vehicleRoutes[id]?.from));

        const verticalMovingNow = isMoving(['NORTH', 'SOUTH'], currentLeft);
        const verticalMovingNext = isMoving(['NORTH', 'SOUTH'], nextLeft);

        const horizontalMovingNow = isMoving(['EAST', 'WEST'], currentLeft);
        const horizontalMovingNext = isMoving(['EAST', 'WEST'], nextLeft);

        const lights = { ...defaultLights };

        if (verticalMovingNow) {
            lights.NORTH = lights.SOUTH = verticalMovingNext ? 'GREEN' : 'YELLOW';
        }

        if (horizontalMovingNow) {
            lights.EAST = lights.WEST = horizontalMovingNext ? 'GREEN' : 'YELLOW';
        }

        return lights;
    };

    const getRandomRoute = () => {
        const dirs: Direction[] = ['NORTH', 'SOUTH', 'EAST', 'WEST'];
        const start = dirs[Math.floor(Math.random() * 4)];
        let end = dirs[Math.floor(Math.random() * 4)];
        while (end === start) end = dirs[Math.floor(Math.random() * 4)];
        return { start, end };
    };

    const generateTraffic = (count: number) => {
        const newRoutes: Record<string, { from: Direction, to: Direction }> = {};
        const newCmds: Command[] = [];
        let localCounter = commands.filter(c => c.type === 'addVehicle').length + 1;

        for (let i = 0; i < count; i++) {
            const { start, end } = getRandomRoute();
            const id = `AUTO_${localCounter++}`;
            if (i > 0 && i % 5 === 0) newCmds.push({ type: 'step' });

            newRoutes[id] = { from: start, to: end };
            newCmds.push({ type: 'addVehicle', vehicleId: id, startRoad: start, endRoad: end });
        }
        setVehicleRoutes(prev => ({ ...prev, ...newRoutes }));
        setCommands(prev => [...prev, ...newCmds]);
        setEventLog(prev => [`[PLANNER] Dodano ${count} pojazdów do scenariusza.`, ...prev]);
    };

    const addEmergencyVehicle = () => {
        const { start, end } = getRandomRoute();
        const id = `EMERGENCY_${Math.floor(Math.random() * 1000)}`;
        setVehicleRoutes(prev => ({ ...prev, [id]: { from: start, to: end } }));
        setCommands(prev => [...prev, { type: 'addVehicle', vehicleId: id, startRoad: start, endRoad: end }]);
        setEventLog(prev => [`[PLANNER] Dodano pojazd uprzywilejowany: ${id}`, ...prev]);
    };

    const exportInputJson = () => {
        const blob = new Blob([JSON.stringify({ commands }, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `input_${Date.now()}.json`;
        a.click();
    };

    const exportOutputJson = () => {
        if (!data) return;
        const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `output_${Date.now()}.json`;
        a.click();
    };

    const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const json = JSON.parse(e.target?.result as string);

                setCurrentStep(0);
                setWaitingVehicles({ NORTH: [], EAST: [], SOUTH: [], WEST: [] });
                setCrossingVehicles([]);
                setIsPlaying(false);

                if (json.commands) {
                    setCommands(json.commands);
                    const newRoutes: Record<string, { from: Direction, to: Direction }> = {};
                    json.commands.forEach((cmd: any) => {
                        if (cmd.type === 'addVehicle') {
                            newRoutes[cmd.vehicleId] = { from: cmd.startRoad, to: cmd.endRoad };
                        }
                    });
                    setVehicleRoutes(newRoutes);
                    setData(null);
                    setEventLog(prev => ["📂 Wczytano INPUT. Kliknij 'URUCHOM', aby obliczyć.", ...prev]);
                }
                else if (json.stepStatuses) {
                    setData(json);
                    setEventLog(prev => ["📂 Wczytano OUTPUT. Można odtwarzać animację.", ...prev]);
                }
            } catch (err) {
                alert("Błąd: Nieprawidłowy format pliku JSON!");
            }
        };
        reader.readAsText(file);
    };


    const runSimulationOnBackend = async () => {
        if (commands.length === 0) return;
        setEventLog(prev => ["⏳ Uruchamiam silnik w Javie...", ...prev]);
        try {
            const response = await fetch('http://localhost:3001/run-simulation', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ commands })
            });
            const result = await response.json();
            setData(result);
            setCurrentStep(0);
            setWaitingVehicles({ NORTH: [], EAST: [], SOUTH: [], WEST: [] });
            setCrossingVehicles([]);
            setIsPlaying(true);
            setEventLog(prev => ["✅ Backend przeliczył dane. Start animacji!", ...prev]);
        } catch (err) {
            setEventLog(prev => ["❌ Błąd! Czy 'node bridge.js' działa w terminalu?", ...prev]);
        }
    };

    return (
        <div className="min-h-screen p-8 flex flex-col items-center gap-6 bg-[#0f0f12] text-white">
            <div className="w-full max-w-6xl grid grid-cols-4 gap-4">
                <div className="col-span-2 bg-traffic-gray p-6 rounded-2xl border border-neutral-700">
                    <h1 className="text-xl font-bold flex items-center gap-2"><Activity className="text-blue-500" /> Smart Traffic Analyzer</h1>
                    <p className="text-xs text-gray-400">AVSystem Recruitment Project</p>
                </div>
                <div className="bg-traffic-gray p-4 rounded-2xl border border-neutral-700 text-center flex flex-col justify-center">
                    <div className="text-2xl font-mono text-blue-400">{currentStep} / {data?.stepStatuses?.length || 0}</div>
                    <div className="text-[10px] uppercase text-gray-500 font-bold tracking-widest">Krok Symulacji</div>
                </div>
                <div className="bg-traffic-gray p-4 rounded-2xl border border-neutral-700 text-center flex flex-col justify-center">
                    <div className="text-2xl font-mono text-emerald-400">
                        {data?.stepStatuses ? data.stepStatuses.slice(0, currentStep).reduce((acc, s) => acc + (s.leftVehicles?.length || 0), 0) : 0}
                    </div>
                    <div className="text-[10px] uppercase text-gray-500 font-bold tracking-widest">Pojazdy obsłużone</div>
                </div>
            </div>

            <div className="w-full max-w-6xl grid grid-cols-12 gap-6">
                <div className="col-span-3 flex flex-col gap-4">
                    <div className="bg-traffic-gray p-4 rounded-xl border border-neutral-700">
                        <h3 className="text-xs font-bold text-blue-400 uppercase mb-3 flex justify-between">
                            <span>Generator Ruchu</span>
                            <span className="text-neutral-500">{commands.length} cmd</span>
                        </h3>
                        <div className="flex flex-col gap-2">
                            <button onClick={() => generateTraffic(10)} className="bg-neutral-800 hover:bg-neutral-700 py-2 rounded-lg text-xs border border-neutral-600 transition-colors">Dodaj 10 aut</button>
                            <button onClick={() => generateTraffic(50)} className="bg-blue-600/20 hover:bg-blue-600/40 text-blue-400 py-2 rounded-lg text-xs font-bold border border-blue-600/50 transition-colors">Szczyt (50)</button>
                            <button onClick={addEmergencyVehicle} className="bg-rose-600/20 hover:bg-rose-600/40 text-rose-400 py-2 rounded-lg text-xs font-bold border border-rose-600/50 transition-colors">Dodaj Karetkę</button>
                            <button onClick={runSimulationOnBackend} className="w-full bg-blue-600 hover:bg-blue-500 py-3 rounded-lg text-sm font-bold flex items-center justify-center gap-2 mt-2 shadow-lg active:scale-95 transition-transform">
                                <Play size={16} fill="white" /> URUCHOM W UI
                            </button>
                        </div>
                    </div>

                    <div className="bg-traffic-gray p-4 rounded-xl border border-neutral-700">
                        <h3 className="text-xs font-bold text-emerald-400 uppercase mb-3">Zarządzanie danymi</h3>

                        <label className="w-full mb-4 bg-blue-600/20 hover:bg-blue-600/40 text-blue-400 py-2 rounded-lg text-xs font-bold border border-blue-600/50 flex items-center justify-center gap-2 cursor-pointer transition-colors">
                            <RefreshCcw size={14}/> Wgraj JSON (Input/Output)
                            <input type="file" accept=".json" onChange={handleFileUpload} className="hidden" />
                        </label>

                        <div className="grid grid-cols-2 gap-2">
                            <button onClick={exportInputJson} className="bg-neutral-800 hover:bg-neutral-700 py-2 rounded-lg text-[10px] border border-neutral-600 flex flex-col items-center gap-1 transition-colors">
                                <Download size={14}/> <span>Zapisz Input</span>
                            </button>

                            <button
                                onClick={exportOutputJson}
                                disabled={!data}
                                className={`py-2 rounded-lg text-[10px] flex flex-col items-center gap-1 transition-colors ${
                                    data ? 'bg-neutral-800 hover:bg-neutral-700 border border-neutral-600' : 'opacity-30 cursor-not-allowed border border-transparent'
                                }`}
                            >
                                <Download size={14}/> <span>Zapisz Output</span>
                            </button>
                        </div>
                    </div>
                </div>

                <div className="col-span-6 flex flex-col items-center gap-6">
                    {data ? (
                        <Intersection
                            lights={getLightStates()}
                            waitingVehicles={waitingVehicles}
                            crossingVehicles={crossingVehicles}
                        />
                    ) : (
                        <div className="w-[600px] h-[600px] bg-neutral-900 rounded-3xl border-8 border-dashed border-neutral-800 flex flex-col items-center justify-center text-center p-12">
                            <Car size={48} className="text-neutral-700 mb-4 animate-bounce" />
                            <h2 className="font-bold text-white mb-2">Gotowy do startu</h2>
                            <p className="text-sm text-gray-500 leading-relaxed">Wygeneruj ruch po lewej lub wgraj plik JSON, a następnie kliknij niebieski przycisk.</p>
                        </div>
                    )}

                    <div className="flex items-center gap-6 bg-traffic-gray px-8 py-3 rounded-full border border-neutral-700 shadow-2xl">
                        <button onClick={() => { setCurrentStep(0); setWaitingVehicles({ NORTH: [], EAST: [], SOUTH: [], WEST: [] }); setCrossingVehicles([]); }} className="text-gray-400 hover:text-white transition-colors"><RefreshCcw size={20} /></button>
                        <button onClick={() => setIsPlaying(!isPlaying)} className="w-12 h-12 flex items-center justify-center bg-blue-600 hover:bg-blue-500 rounded-full shadow-[0_0_20px_rgba(37,99,235,0.4)] transition-all active:scale-90">
                            {isPlaying ? <Pause fill="white" /> : <Play fill="white" className="ml-1" />}
                        </button>
                        <button onClick={handleNextStep} className="text-gray-400 hover:text-white transition-colors"><SkipForward size={20} /></button>
                        <select value={speed} onChange={(e) => setSpeed(Number(e.target.value))} className="bg-neutral-800 border-none rounded-lg px-2 py-1 text-xs text-blue-400 font-bold outline-none cursor-pointer">
                            <option value={2500}>0.25x</option>
                            <option value={1200}>0.5x</option>
                            <option value={800}>1.0x</option>
                            <option value={300}>2.5x</option>
                        </select>
                    </div>
                </div>

                <div className="col-span-3 bg-traffic-gray rounded-xl border border-neutral-700 overflow-hidden flex flex-col">
                    <div className="p-3 bg-neutral-800 border-b border-neutral-700 flex items-center gap-2">
                        <ScrollText size={14} className="text-blue-400" />
                        <span className="text-[10px] font-bold uppercase tracking-widest text-white">Live Event Log</span>
                    </div>
                    <div className="p-4 font-mono text-[10px] flex flex-col gap-2 overflow-y-auto h-[550px] custom-scrollbar">
                        {eventLog.map((log, i) => (
                            <div key={i} className={`pb-2 border-l-2 pl-3 ${i === 0 ? 'border-blue-500 text-blue-100 font-bold' : 'border-neutral-700 text-neutral-500'}`}>{log}</div>
                        ))}
                    </div>
                </div>
            </div>
        </div>
    );

};