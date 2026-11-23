import type { Metadata } from './api';
import { friendly } from './messages';
import { api, BASE_PATH, session } from './session';
import { t } from './i18n';
import { inChamber, protectBlob, revealBlob, type Passed } from './e2e/files';
import { protectName } from './e2e/names';

/** Media types the browser may show inline; EDS serves only these inline. */
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const VIDEO_TYPES = ['video/mp4', 'video/webm'];

/** Larger files are offered for download only (RUNE.md, "Serving"). */
export const PREVIEW_MAX_BYTES = Number(import.meta.env.VITE_PREVIEW_MAX_MB ?? 100) * 1024 * 1024;

const THUMBNAIL_EDGE = 320;
const THUMBNAIL_MAX_BYTES = 262144;

export function isImage(metadata: Metadata): boolean {
  return IMAGE_TYPES.includes((metadata.mediaType ?? '').toLowerCase());
}

export function isVideo(metadata: Metadata): boolean {
  return VIDEO_TYPES.includes((metadata.mediaType ?? '').toLowerCase());
}

export function isPreviewable(metadata: Metadata): boolean {
  return (isImage(metadata) || isVideo(metadata)) && (metadata.size ?? 0) <= PREVIEW_MAX_BYTES;
}

// Download tokens, per Data Object, renewed a minute before they expire.
const tokens = new Map<string, { url: string; expires: number }>();

/**
 * The URL of the content of a Data Object with a download token, for
 * <img>, <video> and download links, which can't send the session header.
 */
export async function contentUrl(dataId: string): Promise<string> {
  const cached = tokens.get(dataId);
  if (cached && cached.expires - 60000 > Date.now()) {
    return cached.url;
  }
  const response = await api.data.createDownloadToken({ dataId });
  const url = `${BASE_PATH}/data/${encodeURIComponent(dataId)}/content?token=${encodeURIComponent(response.token ?? '')}`;
  const expires = response.expires ? new Date(`${response.expires}Z`).getTime() : Date.now() + 300000;
  tokens.set(dataId, { url, expires });
  return url;
}

export function forgetTokens(): void {
  tokens.clear();
  for (const view of views.values()) {
    void view.then((v) => { URL.revokeObjectURL(v.url); return v.release(); }).catch(() => undefined);
  }
  views.clear();
}

/** What an <img> or <video> shows: an address, whether the content was protected, and how to let go of it. */
export interface View {
  url: string;
  /** True for content of a chamber which no app that knows chambers wrote. */
  unprotected: boolean;
  release: () => Promise<void>;
}

// The decrypted thumbnails of chambers, by their IDs, until logout.
const views = new Map<string, Promise<View>>();

/**
 * The address of a content to show. In a room that is the address with a
 * download token, which the browser reads itself, with ranges. In a
 * chamber the browser would get ciphertext: Rune reads the content,
 * decrypts it (onto the device's private file storage if there is one) and
 * hands out an address of the result; it is released when no longer shown.
 */
export async function viewOf(dataId: string, circleId: string | undefined, context: string, mediaType?: string): Promise<View> {
  const url = await contentUrl(dataId);
  if (!circleId || !(await inChamber(circleId))) {
    return { url, unprotected: false, release: async () => undefined };
  }
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(t('The download failed ({status}).', { status: response.status }));
  }
  const passed: Passed = await revealBlob(circleId, context, await response.blob(), undefined, mediaType ?? '');
  const address = URL.createObjectURL(passed.blob);
  return {
    url: address,
    unprotected: passed.unprotected === true,
    release: async () => {
      URL.revokeObjectURL(address);
      await passed.release();
    },
  };
}

/** The thumbnail of a Data Object to show; decrypted ones are kept until logout. */
export function thumbnailView(thumbnailId: string, circleId: string | undefined): Promise<View> {
  let view = views.get(thumbnailId);
  if (!view) {
    view = viewOf(thumbnailId, circleId, 'thumbnail', 'image/jpeg');
    views.set(thumbnailId, view);
    view.catch(() => views.delete(thumbnailId));
  }
  return view;
}

/**
 * Moves a file to another room. Between rooms the stone does it, and
 * encrypts the file with the other room's key. When either is a chamber the
 * stone can't: Rune reads the file, decrypts it, encrypts it for the other
 * room and stores it there, then deletes it here.
 */
export async function moveToRoom(metadata: Metadata, fromCircleId: string, toCircleId: string): Promise<void> {
  const dataId = metadata.dataId as string;
  const [from, to] = await Promise.all([inChamber(fromCircleId), inChamber(toCircleId)]);
  if (!from && !to) {
    await api.data.postDataMove({ dataId, processDataRequest: { targetCircleId: toCircleId } });
    return;
  }
  if (metadata.typeName === 'folder') {
    throw new Error(t('A folder can\'t be moved into or out of a chamber. Move its files.'));
  }
  const response = await fetch(await contentUrl(dataId));
  if (!response.ok) {
    throw new Error(t('The download failed ({status}).', { status: response.status }));
  }
  const plain = await revealBlob(fromCircleId, '', await response.blob(), undefined, metadata.mediaType ?? '');
  try {
    if (plain.unprotected) {
      throw new Error(t('This file is not protected: it was written by an app which does not know chambers. Download it, and upload it where it should go.'));
    }
    await upload(new File([plain.blob], metadata.dataName ?? 'file', { type: metadata.mediaType ?? '' }), toCircleId, undefined, () => undefined);
  } finally {
    await plain.release();
  }
  await api.data.deleteDataById({ dataId });
}

export interface UploadProgress {
  (sent: number, total: number): void;
}

/**
 * Uploads a file: registers the Data Object, streams the content with
 * progress (fetch has no upload progress, hence XMLHttpRequest), then
 * stores a thumbnail for images and videos. In a chamber both are
 * encrypted first (E2E.md); the stone gets the name, the media type and
 * what it can't read. Returns the new ID. If the
 * content doesn't arrive, the registration is removed again: an empty entry
 * would stay in the room's files, and refuse the next try for its name.
 */
export async function upload(file: File, circleId: string, folderId: string | undefined, progress: UploadProgress): Promise<string> {
  const mediaType = /^[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}\/[a-zA-Z0-9][a-zA-Z0-9!#$&^_.+-]{0,126}$/.test(file.type) ? file.type : undefined;
  const registered = await api.data.postData({
    processDataRequest: { circleId, folderId, dataName: await protectName(circleId, file.name), mediaType },
  });
  const dataId = registered.dataId as string;

  let body: Passed;
  try {
    body = await protectBlob(circleId, '', file);
  } catch (e) {
    await api.data.deleteDataById({ dataId }).catch(() => undefined);
    throw e;
  }
  const content = new Promise<void>((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open('PUT', `${BASE_PATH}/data/${encodeURIComponent(dataId)}/content`);
    request.setRequestHeader('Authorization', `Bearer ${session()?.token ?? ''}`);
    request.setRequestHeader('Content-Type', 'application/octet-stream');
    request.upload.onprogress = (event) => progress(event.loaded, event.lengthComputable ? event.total : body.blob.size);
    request.onload = () => {
      if (request.status === 200) {
        resolve();
      } else {
        let message = t('The upload failed ({status}).', { status: request.status });
        try {
          message = friendly(JSON.parse(request.responseText).returnMessage, message);
        } catch {
          // Not JSON.
        }
        reject(new Error(message));
      }
    };
    request.onerror = () => reject(new Error(t('The upload failed, the connection was lost.')));
    request.onabort = request.onerror;
    request.ontimeout = request.onerror;
    request.send(body.blob);
  });
  try {
    await content;
  } catch (e) {
    await body.release();
    // Best effort: without a connection it stays, and can be deleted by hand.
    await api.data.deleteDataById({ dataId }).catch(() => undefined);
    throw e;
  }
  await body.release();

  try {
    const thumbnail = await makeThumbnail(file);
    if (thumbnail) {
      const sealed = await protectBlob(circleId, 'thumbnail', thumbnail);
      try {
        await api.data.putThumbnail({ dataId, body: sealed.blob });
      } finally {
        await sealed.release();
      }
    }
  } catch {
    // A file without a thumbnail is shown with an icon.
  }
  return dataId;
}

/**
 * Draws a JPEG thumbnail of an image, or of a frame of a video, in the
 * browser. EDS stores it encrypted and never looks at it.
 */
export async function makeThumbnail(file: File): Promise<Blob | null> {
  let source: CanvasImageSource;
  let width: number;
  let height: number;
  let cleanup = () => undefined as void;

  if (IMAGE_TYPES.includes(file.type) || file.type === 'image/gif') {
    const bitmap = await createImageBitmap(file);
    source = bitmap;
    width = bitmap.width;
    height = bitmap.height;
    cleanup = () => bitmap.close();
  } else if (VIDEO_TYPES.includes(file.type)) {
    const video = await videoFrame(file);
    source = video;
    width = video.videoWidth;
    height = video.videoHeight;
    cleanup = () => URL.revokeObjectURL(video.src);
  } else {
    return null;
  }

  try {
    const scale = Math.min(1, THUMBNAIL_EDGE / Math.max(width, height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(width * scale));
    canvas.height = Math.max(1, Math.round(height * scale));
    const context = canvas.getContext('2d');
    if (!context) {
      return null;
    }
    context.drawImage(source, 0, 0, canvas.width, canvas.height);
    for (const quality of [0.8, 0.6, 0.4]) {
      const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality));
      if (blob && blob.size <= THUMBNAIL_MAX_BYTES) {
        return blob;
      }
    }
    return null;
  } finally {
    cleanup();
  }
}

function videoFrame(file: File): Promise<HTMLVideoElement> {
  return new Promise((resolve, reject) => {
    const video = document.createElement('video');
    video.muted = true;
    video.preload = 'metadata';
    video.playsInline = true;
    video.onloadedmetadata = () => {
      video.currentTime = Math.min(1, (video.duration || 0) / 2);
    };
    video.onseeked = () => resolve(video);
    video.onerror = () => reject(new Error(t('The video can\'t be read.')));
    video.src = URL.createObjectURL(file);
  });
}
