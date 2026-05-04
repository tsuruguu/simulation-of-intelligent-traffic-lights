import * as React from 'react';
import { TrafficLight } from './TrafficLight';
import { Direction, LightState } from '../types/simulation';

interface IntersectionProps {
    lights: Record<Direction, LightState>;
    waitingVehicles: Record<Direction, string[]>;
    crossingVehicles: { id: string, from: Direction, to: Direction }[];
}

export const Intersection: React.FC<IntersectionProps> = ({ lights, waitingVehicles, crossingVehicles }) => {
    return (
        <div className="relative w-[600px] h-[600px] bg-[#1a1a1a] rounded-3xl border-8 border-[#333] shadow-2xl overflow-hidden">
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="absolute w-[160px] h-full bg-[#222]" />
                <div className="absolute h-[160px] w-full bg-[#222]" />
                <div className="absolute w-[160px] h-[160px] bg-[#252525]" />
            </div>

            <div className="absolute top-[140px] left-[380px] z-20"><TrafficLight state={lights.NORTH} direction="N" /></div>
            <div className="absolute bottom-[140px] left-[150px] z-20"><TrafficLight state={lights.SOUTH} direction="S" /></div>
            <div className="absolute top-[150px] left-[140px] z-20"><TrafficLight state={lights.WEST} direction="W" horizontal /></div>
            <div className="absolute top-[380px] right-[140px] z-20"><TrafficLight state={lights.EAST} direction="E" horizontal /></div>

            {Object.entries(waitingVehicles).map(([dir, ids]) =>
                ids.map((id, i) => (
                    <Car key={id} id={id} dir={dir as Direction} index={i} type="waiting" />
                ))
            )}

            {crossingVehicles.map(v => (
                <LeavingCar key={v.id} vehicle={v} />
            ))}
        </div>
    );
};


const LeavingCar = ({ vehicle }: { vehicle: { id: string, from: Direction, to: Direction } }) => {
    const [isExiting, setIsExiting] = React.useState(false);

    React.useEffect(() => {
        const frame = requestAnimationFrame(() => setIsExiting(true));
        return () => cancelAnimationFrame(frame);
    }, []);

    const start = getPos(vehicle.from, 0);
    const end = getExitPos(vehicle.to);

    const style: React.CSSProperties = {
        position: 'absolute',
        transition: 'all 2000ms cubic-bezier(0.4, 0, 0.2, 1)',
        zIndex: 50,
        left: start.left,
        top: start.top,
        transform: start.rotate,
        opacity: 1,
        ...(isExiting && {
            left: end.left,
            top: end.top,
            opacity: 0,
            transform: `${start.rotate} scale(0.9)`
        })
    };

    return <div style={style}><CarBox id={vehicle.id} color="bg-white border-2 border-blue-400" /></div>;
};

const Car = ({ id, dir, index, type }: { id: string, dir: Direction, index: number, type: 'waiting' | 'leaving' }) => {
    const pos = getPos(dir, index);
    const colorMap = { NORTH: 'bg-blue-500', SOUTH: 'bg-rose-500', WEST: 'bg-amber-500', EAST: 'bg-emerald-500' };

    return (
        <div className="absolute transition-all duration-500" style={pos}>
            <CarBox id={id} color={colorMap[dir]} />
        </div>
    );
};

const CarBox = ({ id, color }: { id: string, color: string }) => (
    <div className={`${color} w-10 h-14 rounded-md shadow-lg flex items-center justify-center`}>
        <span className="text-[9px] font-black text-black">{id.split('_').pop()}</span>
    </div>
);

function getPos(dir: Direction, i: number) {
    const gap = 65;
    const stop = 155;
    switch(dir) {
        case 'NORTH': return { top: stop - i*gap, left: 330, rotate: 'rotate(180deg)' };
        case 'SOUTH': return { top: 600 - stop - 56 + i*gap, left: 230, rotate: 'rotate(0deg)' };
        case 'WEST':  return { left: stop - i*gap, top: 230, rotate: 'rotate(90deg)' };
        case 'EAST':  return { left: 600 - stop - 40 + i*gap, top: 330, rotate: 'rotate(-90deg)' };
    }
}

function getExitPos(to: Direction) {
    switch(to) {
        case 'NORTH': return { top: -100, left: 230 };
        case 'SOUTH': return { top: 700, left: 330 };
        case 'WEST':  return { left: -100, top: 330 };
        case 'EAST':  return { left: 700, top: 230 };
    }
}