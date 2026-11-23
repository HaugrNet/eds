// What the tests of scripts/flow share: users which run Rune's own modules
// in workers, and the checks.
import { Worker } from 'node:worker_threads';

/** The stone the tests run against. */
export const base = process.argv[2] ?? 'http://localhost:8080';
let count = 0;

/** How many checks were made. */
export function checks(): number {
  return count;
}

/** A user of Rune: Rune's own modules, the ones without a page, in a worker of their own. */
export class User {
  private worker: Worker;
  private pending = new Map<number, { resolve: (value: any) => void; reject: (error: Error) => void }>();
  private counter = 0;
  readonly name: string;

  constructor(name: string) {
    this.name = name;
    this.worker = new Worker(new URL('./dist/user.js', import.meta.url), { workerData: { base } });
    this.worker.on('message', (answer: { id: number; result?: unknown; error?: string }) => {
      const waiting = this.pending.get(answer.id);
      this.pending.delete(answer.id);
      if (answer.error !== undefined) {
        waiting?.reject(new Error(`${this.name}: ${answer.error}`));
      } else {
        waiting?.resolve(answer.result);
      }
    });
    this.worker.on('error', (error) => {
      for (const waiting of this.pending.values()) {
        waiting.reject(error);
      }
    });
  }

  do<T = any>(op: string, ...args: unknown[]): Promise<T> {
    const id = ++this.counter;
    return new Promise<T>((resolve, reject) => {
      this.pending.set(id, { resolve, reject });
      this.worker.postMessage({ id, op, args });
    });
  }

  async refused(what: string, op: string, ...args: unknown[]): Promise<string> {
    count++;
    try {
      await this.do(op, ...args);
    } catch (error) {
      return (error as Error).message;
    }
    throw new Error(`${what}: ${this.name} was not refused`);
  }

  end(): Promise<number> {
    return this.worker.terminate();
  }
}

/** Fails unless the two are the same, compared as JSON. */
export function same(what: string, actual: unknown, expected: unknown): void {
  count++;
  const a = JSON.stringify(actual);
  const e = JSON.stringify(expected);
  if (a !== e) {
    throw new Error(`${what}\n  expected: ${e}\n  actual:   ${a}`);
  }
}

export function yes(what: string, condition: unknown): void {
  count++;
  if (!condition) {
    throw new Error(what);
  }
}

export function step(text: string): void {
  console.log(`- ${text}`);
}

