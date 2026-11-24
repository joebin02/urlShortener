import http from 'k6/http';
import { check, sleep } from 'k6';

// Set SHORT_CODE to an existing link and BASE_URL to the API base.
const base = __ENV.BASE_URL || 'http://localhost:8080';
const code = __ENV.SHORT_CODE || 'missing';
export const options = {
  scenarios: { steady_redirects: { executor: 'constant-arrival-rate', rate: 1000, timeUnit: '1s', duration: '60s', preAllocatedVUs: 100, maxVUs: 500 } },
  thresholds: { http_req_failed: ['rate<0.01'], http_req_duration: ['p(95)<100'] },
};
export default function () {
  const response = http.get(`${base}/${code}`, { redirects: 0 });
  check(response, { 'redirect or missing link response': r => r.status === 302 });
  sleep(0.001);
}
