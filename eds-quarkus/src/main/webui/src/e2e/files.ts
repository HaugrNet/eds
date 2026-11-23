import { t } from '../i18n';
import * as staging from '../staging';
import { chunkCount, contentReader, contentWriter, E2EError, isProtected, plainLength } from './convention';
import { refusal, roomState } from './rooms';

/**
 * Files of chambers (E2E.md, "Content"): Rune encrypts a file before the
 * stone gets it, and decrypts what comes back, chunk by chunk. With the
 * browser's private file storage the result goes to a staged file there,
 * so a large file needn't be in memory; without it, to memory. In a room
 * nothing happens here: the content is passed on as it is.
 */
export interface Passed {
  blob: Blob;
  /** True for content of a chamber which no app that knows chambers wrote: it is not protected. */
  unprotected?: boolean;
  /** Removes what was staged for this. */
  release: () => Promise<void>;
}

const NOTHING = async () => undefined;
let counter = 0;

/** Smaller content, such as a thumbnail, stays in memory. */
const STAGED_FROM = 4 * 1024 * 1024;

/** Where the chunks go: a staged file, or memory. */
async function sink(name: string, type: string, size: number): Promise<{ write: (chunk: Uint8Array) => Promise<void>; done: () => Promise<Passed>; abandon: () => Promise<void> }> {
  if (size >= STAGED_FROM && staging.available()) {
    try {
      await staging.open(name);
      await staging.truncate(name);
      return {
        write: async (chunk) => { await staging.append(name, chunk); },
        done: async () => {
          await staging.close(name);
          const file = await staging.file(name);
          if (!file) {
            throw new Error(t('The file is gone from the storage of this device.'));
          }
          return { blob: type ? file.slice(0, file.size, type) : file, release: () => staging.remove(name) };
        },
        abandon: () => staging.remove(name),
      };
    } catch {
      // Without the storage, in memory.
    }
  }
  const parts: BlobPart[] = [];
  return {
    write: async (chunk) => { parts.push(chunk as Uint8Array<ArrayBuffer>); },
    done: async () => ({ blob: new Blob(parts, { type: type || 'application/octet-stream' }), release: NOTHING }),
    abandon: NOTHING,
  };
}

async function bytes(blob: Blob, from: number, to: number): Promise<Uint8Array> {
  return new Uint8Array(await blob.slice(from, to).arrayBuffer());
}

/** True if the room is a chamber, in whatever state. */
export async function inChamber(circleId: string): Promise<boolean> {
  return (await roomState(circleId)).kind !== 'room';
}

/**
 * What goes to the stone for a file of a room: the file itself in a room,
 * encrypted in a chamber. The context is empty for a file, "thumbnail" for
 * a thumbnail.
 */
export async function protectBlob(circleId: string, context: string, plain: Blob, progress?: (done: number, total: number) => void): Promise<Passed> {
  const state = await roomState(circleId);
  if (state.kind === 'room') {
    return { blob: plain, release: NOTHING };
  }
  if (!state.current) {
    throw new E2EError(refusal(state) ?? '');
  }
  const writer = await contentWriter(state.current, circleId, context);
  const out = await sink(`protect.${Date.now()}.${++counter}`, '', plain.size);
  try {
    await out.write(writer.header);
    const count = chunkCount(plain.size, writer.chunkSize);
    for (let i = 0; i < count; i++) {
      await out.write(await writer.seal(i, i === count - 1, await bytes(plain, i * writer.chunkSize, (i + 1) * writer.chunkSize)));
      progress?.(Math.min(plain.size, (i + 1) * writer.chunkSize), plain.size);
    }
    return await out.done();
  } catch (e) {
    await out.abandon();
    throw e;
  }
}

/**
 * A file of a room as it came from the stone, readable: as it is in a room,
 * decrypted in a chamber. `name` names the staged file, so what a reload
 * left behind is found and removed; `type` is the media type of the result.
 */
export async function revealBlob(circleId: string, context: string, stored: Blob, name?: string, type = ''): Promise<Passed> {
  const state = await roomState(circleId);
  if (state.kind === 'room') {
    return { blob: stored, release: NOTHING };
  }
  const start = await bytes(stored, 0, 70);
  if (!isProtected(start)) {
    return { blob: stored, unprotected: true, release: NOTHING };
  }
  if (state.keys.size === 0) {
    throw new E2EError(refusal(state) ?? '');
  }
  const reader = await contentReader(state.keys, circleId, context, start);
  const out = await sink(name ?? `reveal.${Date.now()}.${++counter}`, type, stored.size);
  try {
    const count = reader.count(stored.size);
    for (let i = 0; i < count; i++) {
      await out.write(await reader.open(i, i === count - 1, await bytes(stored, reader.offset + i * reader.step, reader.offset + (i + 1) * reader.step)));
    }
    return await out.done();
  } catch (e) {
    await out.abandon();
    throw e;
  }
}

/** The size of a file as its people see it: what the stone stores of a chamber's file is a little more. */
export function shownSize(storedSize: number | undefined, chamber: boolean): number {
  return chamber ? plainLength(storedSize ?? 0) : storedSize ?? 0;
}
