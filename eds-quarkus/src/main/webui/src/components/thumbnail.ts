import type { Metadata } from '../api';
import { h } from '../dom';
import { isImage, isVideo, thumbnailView } from '../media';

/** A thumbnail of a Data Object: the stored one, or an icon for its type. */
export function thumbnail(metadata: Metadata): HTMLElement {
  const box = h('span', { class: 'thumb', 'aria-hidden': 'true' }, icon(metadata));
  if (metadata.thumbnailId) {
    thumbnailView(metadata.thumbnailId, metadata.circleId)
      .then((view) => {
        if (view.unprotected) {
          return; // Not passed off as part of the chamber: the icon stays.
        }
        // The image is put in the page once it has loaded, so it can't wait to
        // be seen before it loads: Chromium never loads a lazy image outside
        // the page. Thumbnails are small, and a folder lists at most a page.
        const image = h('img', { src: view.url, alt: '', loading: 'eager', decoding: 'async' });
        image.addEventListener('load', () => box.replaceChildren(image, ...(isVideo(metadata) ? [h('span', { class: 'play' }, '▶')] : [])));
      })
      .catch(() => undefined);
  }
  return box;
}

function icon(metadata: Metadata): string {
  if (metadata.typeName === 'folder') {
    return '📁';
  }
  if (isImage(metadata)) {
    return '🖼';
  }
  if (isVideo(metadata)) {
    return '🎞';
  }
  return '📄';
}
