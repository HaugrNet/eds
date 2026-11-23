# The click test of Rune

Everything Rune offers, used through its pages in real browsers, as people
would, by several at once: the System Administrator who claims an empty stone,
two users, a guest who comes in by an invitation link, and a second device of
one of them. They chat, share and preview files, keep a calendar and plans,
tell where they are, call each other, work in a chamber, and change their
accounts; the administrator sets up the stone (features, status keys, allowed
origins, users, rooms) and watches it under "Stones".

It needs a JAR of EDS with Rune, and Chromium (the snap by default, or the one
named by `CHROMIUM`). From the project root:

```bash
./eds.sh build rune                               # or the whole build
eds-quarkus/src/main/webui/scripts/click/run.sh
```

`run.sh` starts a stone of its own for each run: `eds-quarkus/target/eds-rune.jar`,
empty, with the in-memory database and a fresh MasterKey, on port 8091 (or
`CLICK_STONE_PORT`), and stops it afterwards. `click.ts` can also be run alone
against an empty test stone, e.g. `node scripts/click/click.ts http://localhost:8090`;
it claims the stone as its first account, so never point it at a stone people use.

## What counts as a failure

- A step which doesn't work: the test stops there, and each browser's screenshot
  is written to `eds-quarkus/target/click/`, beside the stone's log.
- Anything a page showed the developer, even when all steps worked: an uncaught
  exception, an error in the console, a request the stone answered with an error
  which the step didn't expect, or a dialog of the browser (`confirm`, `prompt`)
  which nobody expected, or one which was expected and never came.
- An `ERROR` in the stone's log.

## How it drives the browsers

`driver.ts` speaks the DevTools protocol over Node's own WebSocket; nothing is
installed for it. A click is a mouse press at the middle of the element, once the
page held still for a moment, and only if the element is what a person would hit
there; typing is the browser's own input, checked afterwards. What counts is what
a person sees: shown, not hidden, and inside the open dialog if one is modal.

Two things a headless browser can't do are done the way other browsers do them:
the save dialog of the File System Access API is left out, so downloads go the way
of browsers without it (Firefox, Safari), and are compared byte for byte with what
was uploaded; and camera and microphone are Chromium's fake devices.

Rune looks for new rooms and changed access every 20 seconds, so the steps which
wait for that wait up to 30 seconds. A run takes about four minutes.
