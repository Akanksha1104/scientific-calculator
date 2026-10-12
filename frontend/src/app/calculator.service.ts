import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type AngleMode = 'DEG' | 'RAD';

export interface CalculationResult {
  expression: string;
  result: number;
  angleMode: AngleMode;
  timestamp: string;
}

@Injectable({ providedIn: 'root' })
export class CalculatorService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/calculator';

  evaluate(expression: string, angleMode: AngleMode): Observable<CalculationResult> {
    return this.http.post<CalculationResult>(`${this.baseUrl}/evaluate`, { expression, angleMode });
  }

  getHistory(): Observable<CalculationResult[]> {
    return this.http.get<CalculationResult[]>(`${this.baseUrl}/history`);
  }

  clearHistory(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/history`);
  }
}
