import express from 'express';
import cors from 'cors';
import { exec } from 'child_process';
import fs from 'fs';
import path from 'path';

const app = express();
app.use(cors());
app.use(express.json());

app.post('/run-simulation', (req, res) => {
    const inputPath = path.join(process.cwd(), 'public/input_temp.json');
    const outputPath = path.join(process.cwd(), 'public/output_temp.json');
    const jarPath = path.join(process.cwd(), '../backend/target/traffic-simulation-backend-1.0-SNAPSHOT.jar');

    fs.writeFileSync(inputPath, JSON.stringify(req.body, null, 2));

    exec(`java -jar ${jarPath} ${inputPath} ${outputPath}`, (error) => {
        if (error) {
            return res.status(500).json({ error: "Błąd backendu Javy: " + error.message });
        }

        const resultData = JSON.parse(fs.readFileSync(outputPath, 'utf8'));
        res.json(resultData);
    });
});

app.listen(3001, () => console.log('🚀 Mostek UI-Backend działa na porcie 3001'));