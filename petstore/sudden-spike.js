import http from 'k6/http';
import { sleep } from 'k6';

export let options = {
    stages: [
        { duration: '2m', target: 100 },
        { duration: '2m', target: 150 },
        { duration: '5m', target: 150 },
    ],
};

export default function () {
    http.get('https://yevhenii-petstorepetservice-eastus-hecchzgvasfzcma5.eastus-01.azurewebsites.net/petstorepetservice/v2/health');
    sleep(1);
}
