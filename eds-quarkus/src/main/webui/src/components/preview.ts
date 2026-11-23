import type { Metadata } from '../api';
import { formatSize, h } from '../dom';
import { download as saveFile, isPaused } from '../downloads';
import { isImage, isPreviewable, isVideo, PREVIEW_MAX_BYTES, viewOf, type View } from '../media';
import { inChamber, shownSize } from '../e2e/files';
import { errorMessage } from '../session';
import { formatNumber, t } from '../i18n';

/**
 * Shows a Data Object: images and videos inline (video with seeking, via
 * range requests with a download token), everything else, and media above
 * the preview limit, as a download.
 */
export async function openPreview(metadata: Metadata): Promise<void> {
  const body = h('div', { class: 'preview-body' }, h('p', { class: 'muted' }, t('Loading…')));
  const close = h('button', { class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  // With progress, also on the button, as the files pane may be hidden.
  const download = h('button', { type: 'button', onclick: () => startDownload() }, isPaused(metadata.dataId as string) ? t('Resume download') : t('Download'));
  const startDownload = (): void => {
    download.disabled = true;
    void saveFile(metadata, (received, total) => {
      download.textContent = total > 0 ? `${formatNumber(Math.round((received / total) * 100))} %` : formatSize(received);
    }).finally(() => {
      download.disabled = false;
      download.textContent = isPaused(metadata.dataId as string) ? t('Resume download') : t('Download');
    });
  };
  const chamber = metadata.circleId ? await inChamber(metadata.circleId).catch(() => false) : false;
  let view: View | null = null;
  const dialog = h('dialog', { class: 'preview' },
    h('header', {}, h('h2', {}, metadata.dataName ?? ''), h('span', { class: 'muted' }, formatSize(shownSize(metadata.size, chamber))), download, close),
    body);
  dialog.addEventListener('close', () => {
    // Stops a playing video and frees the element.
    dialog.querySelectorAll('video').forEach((video) => { video.pause(); video.removeAttribute('src'); video.load(); });
    dialog.remove();
    // What was decrypted for this goes from the device.
    void view?.release();
  });
  document.body.append(dialog);
  dialog.showModal();

  try {
    const shown = isPreviewable(metadata) && (isImage(metadata) || isVideo(metadata));
    if (shown) {
      // In a chamber the whole content is read and decrypted first.
      view = await viewOf(metadata.dataId as string, metadata.circleId, '', metadata.mediaType);
      if (!dialog.open) {
        void view.release();
        return;
      }
    }
    if (view?.unprotected) {
      body.replaceChildren(h('p', { class: 'error' }, t('This file is not protected: it was written by an app which does not know chambers. Rune does not show it as part of the chamber.')),
        h('p', {}, h('button', { type: 'button', onclick: () => startDownload() }, t('Download'))));
    } else if (view && isImage(metadata)) {
      body.replaceChildren(h('img', { src: view.url, alt: metadata.dataName ?? '' }));
    } else if (view) {
      body.replaceChildren(h('video', { src: view.url, controls: true, autoplay: true, playsinline: true, preload: 'metadata' }));
    } else {
      const reason = (isImage(metadata) || isVideo(metadata))
        ? t('Larger than the preview limit of {size}.', { size: formatSize(PREVIEW_MAX_BYTES) })
        : t('No preview for this type.');
      body.replaceChildren(h('p', { class: 'muted' }, reason),
        h('p', {}, h('button', { type: 'button', onclick: () => startDownload() }, t('Download'))));
    }
  } catch (e) {
    body.replaceChildren(h('p', { class: 'error' }, await errorMessage(e)));
  }
}
