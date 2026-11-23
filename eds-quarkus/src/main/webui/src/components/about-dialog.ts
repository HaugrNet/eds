import { h } from '../dom';
import { serverInfo } from '../server';
import { t } from '../i18n';

/**
 * About Rune, from the account menu: that what is put in a room is stored
 * encrypted, how (the design is the chapter Crypto of PLAN.md), and what isn't: the
 * encryption is done by EDS, not in the browser, and names stay readable.
 */
export function openAboutDialog(): void {
  const close = h('button', { type: 'button', class: 'icon', 'aria-label': t('Close'), onclick: () => dialog.close() }, '✕');
  const part = (title: string, ...texts: string[]) =>
    h('section', {}, h('h3', {}, title), ...texts.map((text) => h('p', {}, text)));
  const secure = location.protocol === 'https:';
  const system = h('section', { hidden: true }, h('h3', {}, t('This system')));
  const dialog = h('dialog', { class: 'modal wide about' },
    h('header', {}, h('h2', {}, t('About Rune')), close),
    h('div', { class: 'dialog-body' },
      h('p', {}, t('Rune is a place for rooms with a chat and files, kept by a stone: a Haugr EDS instance, an encrypted data store. Every message, file and thumbnail in a room is encrypted before it is stored. This is how.')),
      part(t('Your passphrase'),
        t('Your account has two pairs of keys: ML-KEM-1024, which is made to also withstand quantum computers, and the elliptic curve P-384. Both are used together, so the protection holds as long as one of them does.'),
        t('The private keys are locked with a key made from your passphrase (PBKDF2-HMAC-SHA512). Without the passphrase they can\'t be opened, which is also why nobody can recover a forgotten one.')),
      part(t('Rooms'),
        t('Each room has a random secret of 256 bits. Every member of the room has a copy of it, locked with that member\'s keys. Who isn\'t a member has no copy and can\'t read the room; that goes for the System Administrator too.')),
      part(t('Messages and files'),
        t('Each message and each file gets a key of its own, made from the room\'s secret (HKDF-SHA512), and is encrypted in pieces with AES-256-GCM. Each piece is sealed, so a changed, missing or swapped piece is noticed when it is read.')),
      part(t('The MasterKey'),
        t('All keys the stone keeps are locked a second time with the MasterKey. The stone gets it from elsewhere when it starts and only has it in memory. A stolen copy of the database can\'t be read without it, and gives nothing to try passphrases against.')),
      part(t('Chambers'),
        t('A chamber is a room whose messages, files, thumbnails and entries of features Rune encrypts on your device, before the stone gets them (P-384, HKDF-SHA512, AES-256-GCM). The key of a chamber is given from member to member, locked to each one\'s own key for chambers; the stone stores these locked copies and can\'t open them. Your own key is kept on the stone locked with your recovery key, which only you have.'),
        t('What a chamber does not hide: who is in it, who wrote when, and the names, types and sizes of its files. And Rune itself comes from the stone: a stone which turns against its people could hand out a changed Rune. A chamber protects against a copy of the database and a curious operator; for more than that, use Tolk, which doesn\'t come from the stone. Compare fingerprints with the others to know that nobody put a key in between.')),
      part(t('What you should know'),
        t('In a room, the stone does the encryption, not your browser. Your passphrase and what you read and write travel to the stone, and it works with them while you are logged in. So the connection must be protected, and the stone trusted while it runs.'),
        t('Names and avatars of accounts and rooms, names of files and folders, and their sizes and times are stored readable.')),
      system,
      h('p', { class: secure ? 'about-connection' : 'about-connection error' }, secure
        ? t('Your connection to this stone is encrypted (HTTPS).')
        : t('Your connection to this stone is not encrypted (no HTTPS). Others on the network can read along.'))));
  dialog.addEventListener('close', () => dialog.remove());
  serverInfo().then((info) => {
    if (info.persistent === undefined) {
      return;
    }
    system.append(h('p', {}, info.persistent
      ? t('This is a production system. Its database ({database}) keeps the data over a restart of the stone.', { database: info.database ?? '' })
      : t('This is a test system. Its database ({database}) is in memory only: all data is lost when the stone stops.', { database: info.database ?? '' })));
    system.hidden = false;
  }).catch(() => undefined);
  document.body.append(dialog);
  dialog.showModal();
}
