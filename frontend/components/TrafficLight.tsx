import * as React from 'react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

// Helper do łączenia klas Tailwinda
function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs));
}

interface TrafficLightProps {
    state: 'RED' | 'YELLOW' | 'GREEN';
    direction: string;
    horizontal?: boolean; // Czy sygnalizator jest obrócony (dla osi E-W)
}

export const TrafficLight: React.FC<TrafficLightProps> = ({ state, direction, horizontal }) => {
    return (
        <div className={cn(
            "flex bg-traffic-gray p-2 rounded-xl border-4 border-black shadow-xl",
            horizontal ? "flex-row space-x-2" : "flex-col space-y-2"
        )}>
            {/* Czerwone */}
            <div className={cn(
                "w-6 h-6 rounded-full border-2 border-black/20 transition-all duration-300",
                state === 'RED' ? "bg-red-600 shadow-[0_0_15px_rgba(220,38,38,0.8)]" : "bg-red-950"
            )} />

            {/* Żółte */}
            <div className={cn(
                "w-6 h-6 rounded-full border-2 border-black/20 transition-all duration-300",
                state === 'YELLOW' ? "bg-yellow-400 shadow-[0_0_15px_rgba(250,204,21,0.8)]" : "bg-yellow-950"
            )} />

            {/* Zielone */}
            <div className={cn(
                "w-6 h-6 rounded-full border-2 border-black/20 transition-all duration-300",
                state === 'GREEN' ? "bg-green-500 shadow-[0_0_15px_rgba(34,197,94,0.8)]" : "bg-green-950"
            )} />

            <span className="absolute -top-6 left-1/2 -translate-x-1/2 text-[10px] font-bold text-gray-400 uppercase">
        {direction}
      </span>
        </div>
    );
};